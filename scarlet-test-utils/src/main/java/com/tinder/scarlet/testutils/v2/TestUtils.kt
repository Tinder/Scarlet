/*
 * © 2018 Match Group, LLC.
 */

@file:JvmName("TestUtils")

package com.tinder.scarlet.testutils.v2

import com.tinder.scarlet.Stream
import com.tinder.scarlet.testutils.TestStreamObserver
import com.tinder.scarlet.testutils.ValueAssert
import com.tinder.scarlet.v2.Event
import com.tinder.scarlet.v2.LifecycleState
import com.tinder.scarlet.v2.ProtocolEvent
import org.assertj.core.api.Assertions.assertThat

inline fun <reified T : LifecycleState> ValueAssert<Event.OnLifecycleStateChange>.withLifecycleState() =
    assert {
        assertThat(lifecycleState).isInstanceOf(T::class.java)
    }

inline fun <reified T : ProtocolEvent> ValueAssert<Event.OnProtocolEvent>.withProtocolEvent() = assert {
    assertThat(protocolEvent).isInstanceOf(T::class.java)
}

