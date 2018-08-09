/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("TestUtils")

package com.tinder.scarlet.testutils

import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Message
import com.tinder.scarlet.ShutdownReason
import com.tinder.scarlet.State
import com.tinder.scarlet.Stream
import com.tinder.scarlet.WebSocket
import org.assertj.core.api.Assertions.assertThat

fun <T : Any> Stream<T>.test() = TestStreamObserver(this)

inline fun <reified T : Any> any(noinline assertion: T.() -> Unit = {}): ValueAssert<T> = ValueAssert<T>()
    .assert { assertThat(this).isInstanceOf(T::class.java) }
    .assert(assertion)

inline fun <reified T : Lifecycle.State> ValueAssert<Event.OnLifecycle.StateChange<*>>.withLifecycleState() =
    assert {
        assertThat(state).isInstanceOf(T::class.java)
    }

inline fun <reified T : WebSocket.Event> ValueAssert<Event.OnWebSocket.Event<*>>.withWebSocketEvent() = assert {
    assertThat(event).isInstanceOf(T::class.java)
}

inline fun <reified T : State> ValueAssert<Event.OnStateChange<*>>.withState() = assert {
    assertThat(state).isInstanceOf(T::class.java)
}

fun ValueAssert<WebSocket.Event.OnMessageReceived>.containingText(expectedText: String) = assert {
    assertThat(message).isInstanceOf(Message.Text::class.java)
    val (text) = message as Message.Text
    assertThat(text).isEqualTo(expectedText)
}

fun ValueAssert<WebSocket.Event.OnMessageReceived>.containingBytes(expectedBytes: ByteArray) = assert {
    assertThat(message).isInstanceOf(Message.Bytes::class.java)
    val (bytes) = message as Message.Bytes
    assertThat(bytes).isEqualTo(expectedBytes)
}

fun ValueAssert<WebSocket.Event.OnConnectionClosing>.withClosingReason(
    expectedShutdownReason: ShutdownReason
) = assert {
    assertThat(shutdownReason).isEqualTo(expectedShutdownReason)
}

fun ValueAssert<WebSocket.Event.OnConnectionClosed>.withClosedReason(expectedShutdownReason: ShutdownReason) = assert {
    assertThat(shutdownReason).isEqualTo(expectedShutdownReason)
}
