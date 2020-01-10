package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.StompCommand
import com.tinder.scarlet.stomp.core.StompHeader
import com.tinder.scarlet.stomp.core.StompMessage


object StompMessageEncoder {

    private const val LF = '\n'
    private const val COLON = ':'

    private const val HEARTBEAT_PAYLOAD = "\n"
    const val TERMINATE_MESSAGE_SYMBOL = '\u0000'

    fun encode(stompMessage: StompMessage): String {
        val stringBuilder = StringBuilder()

        val command = stompMessage.command
        if (command == StompCommand.UNKNOWN) {
            stringBuilder.append(HEARTBEAT_PAYLOAD)
        } else {
            val headers = writeHeaders(command, stompMessage.headers, stompMessage.payload.orEmpty())
            stringBuilder
                .append(command.toString())
                .append(LF)
                .append(headers)
                .append(LF)
                .apply { stompMessage.payload?.let(::append) }
                .append(TERMINATE_MESSAGE_SYMBOL)
        }

        return stringBuilder.toString()
    }

    private fun writeHeaders(
        command: StompCommand,
        headers: Map<String, String>,
        payload: String
    ): String {
        if (headers.isEmpty()) {
            return ""
        }
        val shouldEscape = command != StompCommand.CONNECT && command != StompCommand.CONNECTED

        val stringBuilder = StringBuilder()
        headers.forEach { (key, value) ->
            if (command.isBodyAllowed && key == StompHeader.CONTENT_LENGTH) {
                return@forEach
            }

            val headerKey = encode(key, shouldEscape)
            val headerValue = encode(value, shouldEscape)

            stringBuilder
                .append(headerKey)
                .append(COLON)
                .append(headerValue)
                .append(LF)
        }
        if (command.isBodyAllowed) {
            val contentLength = payload.length
            stringBuilder.append(StompHeader.CONTENT_LENGTH)
                .append(COLON)
                .append(contentLength)
                .append(LF)
        }
        return stringBuilder.toString()
    }

    private fun encode(input: String, escape: Boolean): String {
        return if (escape) escape(input) else input
    }

    /**
     * See STOMP Spec 1.2:
     * <a href="https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"Value Encoding"</a>.
     */
    private fun escape(inString: String): String {
        var sb: StringBuilder? = null
        inString.forEachIndexed { index, symbol ->
            when (symbol) {
                '\\' -> {
                    sb = getStringBuilder(sb, inString, index)
                    sb?.append("\\\\")
                }
                ':' -> {
                    sb = getStringBuilder(sb, inString, index)
                    sb?.append("\\c")
                }
                '\n' -> {
                    sb = getStringBuilder(sb, inString, index)
                    sb?.append("\\n")
                }
                '\r' -> {
                    sb = getStringBuilder(sb, inString, index)
                    sb?.append("\\r")
                }
                else -> sb?.append(symbol)
            }
        }
        return sb?.toString() ?: inString
    }

    private fun getStringBuilder(
        stringBuilder: StringBuilder?,
        inString: String,
        index: Int
    ): StringBuilder {
        var sb: StringBuilder? = stringBuilder
        if (sb == null) {
            sb = java.lang.StringBuilder(inString.length)
            sb.append(inString.substring(0, index))
        }
        return sb
    }

}