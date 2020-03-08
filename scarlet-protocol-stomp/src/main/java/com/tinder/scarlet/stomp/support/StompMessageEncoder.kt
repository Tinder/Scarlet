package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.models.StompCommand
import com.tinder.scarlet.stomp.core.models.StompHeader
import com.tinder.scarlet.stomp.core.models.StompMessage
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

/**
 * An encoder for STOMP frames.
 */
class StompMessageEncoder {

    companion object {

        private const val LF = '\n'.toInt()
        private const val COLON = ':'.toInt()

        private const val HEARTBEAT_PAYLOAD = "\n"
        private const val START_HEADERS_SIZE = 64
    }

    /**
     * Encodes the given STOMP {@code stompMessage} into a byte array.
     * @param stompMessage the message to encode
     * @return the encoded message
     */
    fun encode(stompMessage: StompMessage): ByteArray {
        val arraySize = START_HEADERS_SIZE + stompMessage.payload.size
        val arrayOutputStream = ByteArrayOutputStream(arraySize)

        DataOutputStream(arrayOutputStream).use { dataOutputStream ->

            val command = stompMessage.command
            if (command != StompCommand.HEARTBEAT) {
                writeMessage(dataOutputStream, command, stompMessage)
            } else {
                dataOutputStream.writeChars(HEARTBEAT_PAYLOAD)
            }
        }

        return arrayOutputStream.toByteArray()
    }

    private fun writeMessage(
        dataOutputStream: DataOutputStream,
        command: StompCommand,
        stompMessage: StompMessage
    ) = with(dataOutputStream) {
        write(command.toString().toByteArray(Charsets.UTF_8))
        write(LF)
        writeHeaders(stompMessage, dataOutputStream)
        write(LF)
        write(stompMessage.payload)
        writeByte(0)
    }

    private fun writeHeaders(stompMessage: StompMessage, outputStream: DataOutputStream) {
        if (stompMessage.headers.isEmpty()) return

        val command = stompMessage.command
        val headers = stompMessage.headers
        val shouldEscape = command != StompCommand.CONNECT && command != StompCommand.CONNECTED

        headers.forEach { (key, value) ->
            if (command.isBodyAllowed && key == StompHeader.CONTENT_LENGTH) return@forEach

            val headerKey = encode(key, shouldEscape)
            val headerValue = encode(value, shouldEscape)

            with(outputStream) {
                write(headerKey)
                write(COLON)
                write(headerValue)
                write(LF)
            }
        }

        if (command.isBodyAllowed) {
            val contentLength = stompMessage.payload.size
            with(outputStream) {
                write(StompHeader.CONTENT_LENGTH.toByteArray(Charsets.UTF_8))
                write(COLON)
                write(contentLength.toString().toByteArray(Charsets.UTF_8))
                write(LF)
            }
        }
    }

    private fun encode(input: String, escape: Boolean): ByteArray {
        val outputString = if (escape) escape(input) else input
        return outputString.toByteArray(Charsets.UTF_8)
    }

    /**
     * See STOMP Spec 1.2:
     * <a href="https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"Value Encoding"</a>.
     */
    private fun escape(inString: String): String {
        val stringBuilder = StringBuilder(inString.length)
        inString.forEach { symbol ->
            when (symbol) {
                '\\' -> stringBuilder.append("\\\\")
                ':' -> stringBuilder.append("\\c")
                '\n' -> stringBuilder.append("\\n")
                '\r' -> stringBuilder.append("\\r")
                else -> stringBuilder.append(symbol)
            }
        }
        return stringBuilder.toString()
    }
}