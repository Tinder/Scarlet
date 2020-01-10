package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.StompCommand
import com.tinder.scarlet.stomp.core.StompMessage

object StompMessageDecoder {

    //todo decoder to message
    fun decode(text: String): StompMessage {
        return StompMessage.Builder()
            .withPayload("foo")
            .create(StompCommand.MESSAGE)
    }

}