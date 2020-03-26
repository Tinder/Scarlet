/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.support

import com.tinder.scarlet.stomp.okhttp.models.StompCommand
import com.tinder.scarlet.stomp.okhttp.models.StompMessage
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.ByteArrayOutputStream

/**
 * An decoder for STOMP frames.
 */
class StompMessageDecoder {

    /**
     * Decodes the given byte array into a StompMessage.
     * @param array the array to decode
     */
    fun decode(array: ByteArray): StompMessage? {
        val buffer = array.inputStream().source().buffer()
        return decode(buffer)
    }

    fun decode(byteBuffer: BufferedSource): StompMessage? {
        skipLeadingEol(byteBuffer)
        val stompCommand = readCommand(byteBuffer) ?: return null

        return if (stompCommand != StompCommand.HEARTBEAT) {
            val headerAccessor = StompHeaderAccessor.of()

            val payload = if (!byteBuffer.exhausted()) {
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

    private fun skipLeadingEol(byteBuffer: BufferedSource) {
        while (true) {
            if (!tryConsumeEndOfLine(byteBuffer)) break
        }
    }

    private fun readPayloadOrNull(
        bufferedSource: BufferedSource,
        headerAccessor: StompHeaderAccessor
    ): ByteArray? {
        val contentLength = headerAccessor.contentLength
        return if (contentLength != null && contentLength >= 0) {
            readPayloadWithContentLength(bufferedSource, contentLength)
        } else {
            readPayloadWithoutContentLength(bufferedSource)
        }
    }

    private fun readPayloadWithContentLength(
        bufferedSource: BufferedSource,
        contentLength: Int
    ): ByteArray? {
        if (bufferedSource.exhausted()) return null

        val payload = ByteArray(contentLength)
        bufferedSource.read(payload)

        if (bufferedSource.exhausted()) return null
        val lastSymbolIsNullOctet = bufferedSource.readUtf8CodePoint() == 0
        check(lastSymbolIsNullOctet) { "Frame must be terminated with a null octet" }

        return payload
    }

    private fun readPayloadWithoutContentLength(buffer: BufferedSource): ByteArray? {
        val payload = ByteArrayOutputStream(256)
        while (!buffer.exhausted()) {
            val codePoint = buffer.readUtf8CodePoint()
            if (codePoint != 0) {
                payload.write(codePoint)
            } else {
                return payload.toByteArray()
            }
        }
        return null
    }

    private fun readHeaders(byteBuffer: BufferedSource, headerAccessor: StompHeaderAccessor) {
        while (true) {
            val headerStream = ByteArrayOutputStream(256)
            var headerComplete = false

            while (!byteBuffer.exhausted()) {
                if (tryConsumeEndOfLine(byteBuffer)) {
                    headerComplete = true
                    break
                }
                headerStream.write(byteBuffer.readUtf8CodePoint())
            }

            if (headerStream.size() > 0 && headerComplete) {
                val header = headerStream.toByteArray().toString(Charsets.UTF_8)
                val colonIndex = header.indexOf(':')

                if (colonIndex > 0) {
                    val headerName = unescape(header.substring(0, colonIndex))
                    val headerValue = unescape(header.substring(colonIndex + 1))

                    headerAccessor[headerName] = headerValue
                } else {
                    check(byteBuffer.exhausted()) { "Illegal header: '$header'. A header must be of the form <name>:[<value>]." }
                }
            } else {
                break
            }
        }
    }

    private fun readCommand(byteBuffer: BufferedSource): StompCommand? {
        val command = ByteArrayOutputStream(256)
        while (!byteBuffer.exhausted() && !tryConsumeEndOfLine(byteBuffer)) {
            command.write(byteBuffer.readUtf8CodePoint())
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
    private fun tryConsumeEndOfLine(bufferedSource: BufferedSource): Boolean {
        if (bufferedSource.exhausted()) return false
        val peekSource = bufferedSource.peek()

        return when (peekSource.readUtf8CodePoint().toChar()) {
            '\n' -> {
                bufferedSource.skip(1)
                true
            }
            '\r' -> {
                val nextChartIsNewLine = peekSource.readUtf8CodePoint().toChar() == '\n'
                check(!peekSource.exhausted() && nextChartIsNewLine) { "'\\r' must be followed by '\\n'" }
                bufferedSource.skip(2)
                true
            }
            else -> false
        }
    }

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
}
