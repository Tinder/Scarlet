package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.core.StompCommand
import com.tinder.scarlet.stomp.core.StompMessage
import org.junit.Test

class StompMessageEncoderDecoderTest {

    @Test
    fun `correct_decode_message_after_encode`() {
        val headers = StompHeaderAccessor.of(
            mapOf(
                "foo" to "bar",
                "bar" to "foo"
            )
        ).apply { destination("destination") }
            .createHeader()

        val message = StompMessage.Builder()
            .withPayload("foo")
            .withHeaders(headers)
            .create(StompCommand.SEND)

        val data = StompMessageEncoder.encode(message)
        val decodeMessage = StompMessageDecoder.decode(data)

        assert(decodeMessage.payload == "foo")
        assert(decodeMessage.headers.destination == "destination")

        assert(decodeMessage.headers.size == 4)//because encoder add content-lenth
    }

}