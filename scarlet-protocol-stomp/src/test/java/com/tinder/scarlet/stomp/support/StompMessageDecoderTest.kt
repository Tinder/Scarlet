/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.support

import com.tinder.scarlet.stomp.okhttp.models.StompCommand
import com.tinder.scarlet.stomp.okhttp.models.StompHeader
import com.tinder.scarlet.stomp.okhttp.models.StompMessage
import com.tinder.scarlet.stomp.okhttp.support.StompMessageDecoder
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNull
import org.junit.Test

class StompMessageDecoderTest {

    private val decoder = StompMessageDecoder()

    @Test
    fun `decode message with CRLF EOls`() {
        val message = decode("DISCONNECT\r\n\r\n\u0000")
        assertEquals(
            StompCommand.DISCONNECT,
            message.command
        )
        assertEquals(0, message.headers.size)
        assertEquals(0, message.payload.size)
    }

    @Test
    fun `decode message with no headers and no body`() {
        val message = decode("DISCONNECT\n\n\u0000")
        assertEquals(
            StompCommand.DISCONNECT,
            message.command
        )
        assertEquals(0, message.headers.size)
        assertEquals(0, message.payload.size)
    }

    @Test
    fun `decode message with no body`() {
        val accept = "accept-version:1.1\n"
        val host = "host:github.org\n"
        val message = decode("CONNECT\n$accept$host\n\u0000")

        assertEquals(
            StompCommand.CONNECT,
            message.command
        )
        assertEquals(2, message.headers.size)
        assertEquals("1.1", message.headers[StompHeader.ACCEPT_VERSION])
        assertEquals("github.org", message.headers[StompHeader.HOST])
        assertEquals(0, message.payload.size)
    }

    @Test
    fun `decode message`() {
        val message = decode("MESSAGE\ndestination:test\n\nThe body of the message\u0000")

        assertEquals(StompCommand.MESSAGE, message.command)
        assertEquals(message.headers.toString(), 1, message.headers.size)
        assertEquals("test", message.headers.destination)

        val bodyText = message.payload.toString(Charsets.UTF_8)
        assertEquals("The body of the message", bodyText)
    }

    @Test
    fun `decode message with content length`() {
        val message =
            decode("MESSAGE\ndestination:test\ncontent-length:23\n\nThe body of the message\u0000")

        assertEquals(StompCommand.MESSAGE, message.command)
        assertEquals(2, message.headers.size)
        assertEquals(23, message.headers[StompHeader.CONTENT_LENGTH]?.toInt())

        val bodyText = message.payload.toString(Charsets.UTF_8)
        assertEquals("The body of the message", bodyText)
    }

    @Test
    fun `decode frame with invalid content length`() {
        val message =
            decode("MESSAGE\ndestination:test\ncontent-length:-1\n\nThe body of the message\u0000")

        assertEquals(StompCommand.MESSAGE, message.command)
        assertEquals(2, message.headers.size)
        assertEquals(-1, message.headers[StompHeader.CONTENT_LENGTH]?.toInt())

        val bodyText = message.payload.toString(Charsets.UTF_8)
        assertEquals("The body of the message", bodyText)
    }

    @Test
    fun `decode frame with content length zero`() {
        val message = decode("MESSAGE\ndestination:test\ncontent-length:0\n\n\u0000")

        assertEquals(StompCommand.MESSAGE, message.command)
        assertEquals(2, message.headers.size)
        assertEquals(0, message.headers[StompHeader.CONTENT_LENGTH]?.toInt())

        val bodyText = message.payload.toString(Charsets.UTF_8)
        assertEquals("", bodyText)
    }

    @Test
    fun `decode frame with null octects in the body`() {
        val message =
            decode("MESSAGE\ndestination:test\ncontent-length:23\n\nThe b\u0000dy \u0000f the message\u0000")

        assertEquals(StompCommand.MESSAGE, message.command)
        assertEquals(2, message.headers.size)
        assertEquals(23, message.headers[StompHeader.CONTENT_LENGTH]?.toInt())

        val bodyText = message.payload.toString(Charsets.UTF_8)
        assertEquals("The b\u0000dy \u0000f the message", bodyText)
    }

    @Test
    fun `decode frame with escaped headers`() {
        val message =
            decode("CONNECTED\na\\c\\r\\n\\\\b:alpha\\cbravo\\r\\n\\\\\n\n\u0000")
        assertEquals(
            StompCommand.CONNECTED,
            message.command
        )
        assertEquals(1, message.headers.size)
        assertEquals("alpha:bravo\r\n\\", message.headers["a:\r\n\\b"])
    }

    @Test(expected = IllegalStateException::class)
    fun `decode frame body not allowed`() {
        decode("CONNECTED\naccept-version:1.2\n\nThe body of the message\u0000")
    }

    @Test
    fun `decode frame with header with empty value`() {
        val accept = "accept-version:1.1\n"
        val valuelessKey = "key:\n"
        val message = decode("CONNECTED\n$accept$valuelessKey\n\u0000")

        assertEquals(
            StompCommand.CONNECTED,
            message.command
        )
        assertEquals(2, message.headers.size)
        assertEquals("1.1", message.headers["accept-version"])
        assertEquals("", message.headers["key"])
        assertEquals(0, message.payload.size)
    }

    @Test
    fun decodeFrameWithIncompleteCommand() {
        assertIncompleteDecode("MESSAG")
    }

    @Test
    fun `decode frame with incomplete header`() {
        assertIncompleteDecode("SEND\ndestination")
        assertIncompleteDecode("SEND\ndestination:")
        assertIncompleteDecode("SEND\ndestination:test")
    }

    @Test
    fun `decode frame without null octet terminator`() {
        assertIncompleteDecode("SEND\ndestination:test\n")
        assertIncompleteDecode("SEND\ndestination:test\n\n")
        assertIncompleteDecode("SEND\ndestination:test\n\nThe body")
    }

    @Test
    fun `decode frame with insufficient content`() {
        assertIncompleteDecode("SEND\ncontent-length:23\n\nThe body of the mess")
    }

    @Test
    fun decodeHeartbeat() {
        val frame = "\n"
        val array = frame.toByteArray()
        val message = decoder.decode(array)

        assertEquals(
            StompCommand.HEARTBEAT,
            message?.command
        )
    }

    private fun assertIncompleteDecode(frame: String) {
        val message = decoder.decode(frame.toByteArray(Charsets.UTF_8))
        assertNull(message)
    }

    private fun decode(stompFrame: String): StompMessage {
        val array = stompFrame.toByteArray()
        return array.let(decoder::decode)!!
    }
}