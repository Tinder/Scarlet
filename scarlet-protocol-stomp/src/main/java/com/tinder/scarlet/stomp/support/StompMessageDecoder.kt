package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.models.StompCommand
import com.tinder.scarlet.stomp.core.models.StompMessage
import java.util.regex.Pattern

/**
 * An decoder for STOMP frames.
 */
class StompMessageDecoder {

    private val PATTERN_HEADER = Pattern.compile("([^:\\s]+)\\s*:\\s*([^:\\s]+)")

    fun decode(data: ByteArray): StompMessage = StompMessage.Builder()
        .create(StompCommand.UNKNOWN)
}