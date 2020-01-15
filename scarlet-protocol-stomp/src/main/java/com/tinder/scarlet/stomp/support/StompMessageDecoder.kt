package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.models.StompCommand
import com.tinder.scarlet.stomp.core.models.StompMessage
import com.tinder.scarlet.stomp.support.StompMessageEncoder.TERMINATE_MESSAGE_SYMBOL
import java.io.StringReader
import java.util.Scanner
import java.util.regex.Pattern

object StompMessageDecoder {

    private val PATTERN_HEADER = Pattern.compile("([^:\\s]+)\\s*:\\s*([^:\\s]+)")

    fun decode(data: String): StompMessage = if (data.isNotEmpty()) {
        val reader = Scanner(StringReader(data))
        reader.useDelimiter("\\n")

        val command = StompCommand.valueOf(reader.next())
        val headerAccessor = StompHeaderAccessor.of()

        while (reader.hasNext(PATTERN_HEADER)) {
            val matcher = PATTERN_HEADER.matcher(reader.next())
            if (matcher.find()) {
                headerAccessor[matcher.group(1)] = matcher.group(2)
            }
        }

        reader.skip("\n\n")
        reader.useDelimiter(TERMINATE_MESSAGE_SYMBOL.toString())
        val payload = if (reader.hasNext()) reader.next() else null

        StompMessage.Builder()
            .withPayload(payload)
            .withHeaders(headerAccessor.createHeader())
            .create(command)
    } else {
        StompMessage.Builder()
            .create(StompCommand.UNKNOWN)
    }

}