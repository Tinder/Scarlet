package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.models.StompCommand
import com.tinder.scarlet.stomp.core.models.StompMessage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * An decoder for STOMP frames.
 */
class StompMessageDecoder {

    fun decode(data: ByteArray): StompMessage {
        val byteBuffer = ByteBuffer.wrap(data)
        return decode(byteBuffer)
    }

    private fun decode(byteBuffer: ByteBuffer): StompMessage {
        skipLeadingEol(byteBuffer)
        val command = readCommand(byteBuffer)

        return if (command.isNotEmpty()) {

            val stompCommand = StompCommand.valueOf(command)
            val headerAccessor = StompHeaderAccessor.of()

            val payload: ByteArray? = null
            if (byteBuffer.isNotEmpty()) {
                readHeaders(byteBuffer, headerAccessor)
                val payload = readPayload(byteBuffer, headerAccessor)

                if (payload != null) {
                    if (payload.isNotEmpty()) {
                        if (!stompCommand.isBodyAllowed) {
                            throw IllegalStateException(stompCommand.toString() + " shouldn't have a payload: length=" + payload.size + ", headers=" + headerAccessor)
                        }
                    }
                } else {
                    byteBuffer.reset()
                }
            }
            StompMessage.Builder()
                .withPayload(payload ?: ByteArray(0))
                .create(stompCommand)
        } else {
            StompMessage.Builder().create(StompCommand.UNKNOWN)
        }
    }

    private fun skipLeadingEol(byteBuffer: ByteBuffer) {
        while (true) {
            if (!tryConsumeEndOfLine(byteBuffer)) break
        }
    }

    private fun readPayload(
        byteBuffer: ByteBuffer,
        headerAccessor: StompHeaderAccessor
    ): ByteArray? {
        val contentLength = headerAccessor.contentLength
        if (contentLength != null && contentLength >= 0) {
            return if (byteBuffer.remaining() > contentLength) {
                val payload = ByteArray(contentLength)
                byteBuffer[payload]
                check(
                    byteBuffer.get().toInt() == 0
                ) { "Frame must be terminated with a null octet" }
                payload
            } else {
                null
            }
        } else {
            val payload = ByteArrayOutputStream(256)
            while (byteBuffer.remaining() > 0) {
                val b = byteBuffer.get()
                if (b.toInt() == 0) {
                    return payload.toByteArray()
                } else {
                    payload.write(b.toInt())
                }
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
                val header = String(headerStream.toByteArray(), Charsets.UTF_8)
                val colonIndex = header.indexOf(':')
                if (colonIndex <= 0) {
                    if (byteBuffer.isNotEmpty()) throw IllegalStateException("Illegal header: '$header'. A header must be of the form <name>:[<value>].")
                } else {
                    val headerName = unescape(header.substring(0, colonIndex))
                    val headerValue = unescape(header.substring(colonIndex + 1))
                    headerAccessor[headerName] = headerValue
                }
            } else {
                break
            }
        }
    }

    private fun readCommand(byteBuffer: ByteBuffer): String {
        val command = ByteArrayOutputStream(256)
        while (byteBuffer.isNotEmpty() && !tryConsumeEndOfLine(byteBuffer)) {
            command.write(byteBuffer.get().toInt())
        }
        return String(command.toByteArray(), Charsets.UTF_8)
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
}
