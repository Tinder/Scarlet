/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("ValueAssertUtils")

package com.tinder.scarlet.testutils

import com.tinder.scarlet.Message
import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.websocket.ShutdownReason
import com.tinder.scarlet.websocket.WebSocketEvent
import org.assertj.core.api.Assertions.assertThat

inline fun <reified T : LifecycleState> ValueAssert<Event.OnLifecycleStateChange>.withLifecycleState() =
    assert {
        assertThat(lifecycleState).isInstanceOf(T::class.java)
    }

inline fun <reified T : ProtocolEvent> ValueAssert<Event.OnProtocolEvent>.withProtocolEvent() = assert {
    assertThat(protocolEvent).isInstanceOf(T::class.java)
}

fun ValueAssert<ProtocolEvent.OnMessageReceived>.containingText2(expectedText: String): ValueAssert<ProtocolEvent.OnMessageReceived> {
    return assert {
        assertThat(message).isInstanceOf(Message.Text::class.java)
        val (text) = message as Message.Text
        assertThat(text).isEqualTo(expectedText)
    }
}

fun ValueAssert<ProtocolEvent.OnMessageReceived>.containingBytes2(expectedBytes: ByteArray) : ValueAssert<ProtocolEvent.OnMessageReceived>{
    return assert {
        assertThat(message).isInstanceOf(Message.Bytes::class.java)
        val (bytes) = message as Message.Bytes
        assertThat(bytes).isEqualTo(expectedBytes)
    }
}

fun ValueAssert<WebSocketEvent.OnMessageReceived>.containingText(expectedText: String): ValueAssert<WebSocketEvent.OnMessageReceived> {
    return assert {
        assertThat(message).isInstanceOf(Message.Text::class.java)
        val (text) = message as Message.Text
        assertThat(text).isEqualTo(expectedText)
    }
}

fun ValueAssert<WebSocketEvent.OnMessageReceived>.containingBytes(expectedBytes: ByteArray) : ValueAssert<WebSocketEvent.OnMessageReceived>{
    return assert {
        assertThat(message).isInstanceOf(Message.Bytes::class.java)
        val (bytes) = message as Message.Bytes
        assertThat(bytes).isEqualTo(expectedBytes)
    }
}

fun ValueAssert<WebSocketEvent.OnConnectionClosing>.withClosingReason(
    expectedShutdownReason: ShutdownReason
) : ValueAssert<WebSocketEvent.OnConnectionClosing> {
    return assert {
        assertThat(shutdownReason).isEqualTo(expectedShutdownReason)
    }
}

fun ValueAssert<WebSocketEvent.OnConnectionClosed>.withClosedReason(expectedShutdownReason: ShutdownReason): ValueAssert<WebSocketEvent.OnConnectionClosed> {
    return assert {
        assertThat(shutdownReason).isEqualTo(expectedShutdownReason)
    }
}