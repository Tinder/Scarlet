/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.support

import com.tinder.scarlet.stomp.okhttp.models.StompCommand
import com.tinder.scarlet.stomp.okhttp.models.StompMessage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * An decoder for STOMP frames.
 */
class StompMessageDecoder {

    /**
     * Decodes the given byte array into a StompMessage.
     * @param array the array to decode
     */
    fun decode(array: ByteArray): StompMessage? {
        val byteBuffer = ByteBuffer.wrap(array)
        return decode(byteBuffer)
    }

    private fun decode(byteBuffer: ByteBuffer): StompMessage? {
        skipLeadingEol(byteBuffer)
        val stompCommand = readCommand(byteBuffer) ?: return null

        return if (stompCommand != StompCommand.HEARTBEAT) {
            val headerAccessor = StompHeaderAccessor.of()

            val payload = if (byteBuffer.isNotEmpty()) {
                readHeaders(byteBuffer, headerAccessor)
                readPayloadOrNull(byteBuffer, headerAccessor) ?: return null
            } else {
                ByteArray(0)
            }

            StompMessage.Builder()
                .withHeaders(headerAccessor.createHeader())
                .withPayload(payload)
                .create(stompCommand)
        } else {
            StompMessage.Builder()
                .create(StompCommand.HEARTBEAT)
        }
    }

    private fun skipLeadingEol(byteBuffer: ByteBuffer) {
        while (true) {
            if (!tryConsumeEndOfLine(byteBuffer)) break
        }
    }

    private fun readPayloadOrNull(
        byteBuffer: ByteBuffer,
        headerAccessor: StompHeaderAccessor
    ): ByteArray? {
        val contentLength = headerAccessor.contentLength
        return if (contentLength != null && contentLength >= 0) {
            readPayloadWithContentLength(byteBuffer, contentLength)
        } else {
            readPayloadWithoutContentLength(byteBuffer)
        }
    }

    private fun readPayloadWithContentLength(
        byteBuffer: ByteBuffer,
        contentLength: Int
    ) = byteBuffer
        .takeIf { buffer -> buffer.remaining() > contentLength }
        ?.let { buffer ->
            val payload = ByteArray(contentLength)
            buffer.get(payload)

            val lastSymbolIsNullOctet = byteBuffer.get().toInt() == 0
            check(lastSymbolIsNullOctet) { "Frame must be terminated with a null octet" }
            payload
        }

    private fun readPayloadWithoutContentLength(byteBuffer: ByteBuffer): ByteArray? {
        val payload = ByteArrayOutputStream(256)
        while (byteBuffer.isNotEmpty()) {
            val byte = byteBuffer.get()
            if (byte.toInt() != 0) {
                payload.write(byte.toInt())
            } else {
                return payload.toByteArray()
            }
        }
        return null
    }

    private fun readHeaders(byteBuffer: ByteBuffer, headerAccessor: StompHeaderAccessor) {
        while (true) {
            val headerStream = ByteArrayOutputStream(256)
            var headerComplete = false

            while (byteBuffer.hasRemaining()) {
                if (tryConsumeEndOfLine(byteBuffer)) {
                    headerComplete = true
                    break
                }
                headerStream.write(byteBuffer.get().toInt())
            }

            if (headerStream.size() > 0 && headerComplete) {
                val header = headerStream.toByteArray().toString(Charsets.UTF_8)
                val colonIndex = header.indexOf(':')

                if (colonIndex > 0) {
                    val headerName = unescape(header.substring(0, colonIndex))
                    val headerValue = unescape(header.substring(colonIndex + 1))

                    headerAccessor[headerName] = headerValue
                } else {
                    check(byteBuffer.isEmpty()) { "Illegal header: '$header'. A header must be of the form <name>:[<value>]." }
                }
            } else {
                break
            }
        }
    }

    private fun readCommand(byteBuffer: ByteBuffer): StompCommand? {
        val command = ByteArrayOutputStream(256)
        while (byteBuffer.isNotEmpty() && !tryConsumeEndOfLine(byteBuffer)) {
            command.write(byteBuffer.get().toInt())
        }
        val commandString = command.toByteArray().toString(Charsets.UTF_8)
        return try {
            if (commandString.isNotEmpty()) {
                StompCommand.valueOf(commandString)
            } else {
                StompCommand.HEARTBEAT
            }
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Try to read an EOL incrementing the buffer position if successful.
     * @return whether an EOL was consumed
     */
    private fun tryConsumeEndOfLine(byteBuffer: ByteBuffer): Boolean = byteBuffer
        .takeIf { buffer -> buffer.isNotEmpty() }
        ?.let { buffer ->
            when (byteBuffer.get()) {
                '\n'.toByte() -> true
                '\r'.toByte() -> checkSequence(byteBuffer)
                else -> {
                    buffer.position(buffer.position() - 1)
                    false
                }
            }
        } ?: false

    /**
     * See STOMP Spec 1.2:
     * ["Value Encoding"](https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding).
     */
    private fun unescape(inString: String): String {
        val stringBuilder = StringBuilder(inString.length)
        var pos = 0 // position in the old string
        var index = inString.indexOf('\\')
        while (index >= 0) {
            stringBuilder.append(inString.substring(pos, index))
            check(index + 1 < inString.length) { "Illegal escape sequence at index $index: $inString" }

            when (inString[index + 1]) {
                'r' -> stringBuilder.append('\r')
                'n' -> stringBuilder.append('\n')
                'c' -> stringBuilder.append(':')
                '\\' -> stringBuilder.append('\\')
                else -> throw IllegalStateException("Illegal escape sequence at index $index: $inString") // should never happen
            }
            pos = index + 2
            index = inString.indexOf('\\', pos)
        }
        stringBuilder.append(inString.substring(pos))
        return stringBuilder.toString()
    }

    private fun checkSequence(byteBuffer: ByteBuffer): Boolean {
        val nextChartIsNewLine = byteBuffer.get() == '\n'.toByte()
        check(byteBuffer.remaining() > 0 && nextChartIsNewLine) { "'\\r' must be followed by '\\n'" }
        return true
    }

    private fun ByteBuffer.isNotEmpty(): Boolean = remaining() > 0

    private fun ByteBuffer.isEmpty(): Boolean = remaining() == 0
}
