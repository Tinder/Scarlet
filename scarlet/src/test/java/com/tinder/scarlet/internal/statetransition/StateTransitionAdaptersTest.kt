/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.nhaarman.mockitokotlin2.given
import com.nhaarman.mockitokotlin2.mock
import com.tinder.scarlet.Event
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.internal.common.CLIENT_ABORT
import com.tinder.scarlet.internal.common.CLIENT_CLOSURE
import com.tinder.scarlet.internal.common.FAILED_DESERIALIZATION
import com.tinder.scarlet.internal.common.MALFORMED_MESSAGE
import com.tinder.scarlet.internal.common.MESSAGE
import com.tinder.scarlet.internal.common.SERVER_CLOSURE
import com.tinder.scarlet.internal.common.SERVER_MESSAGES
import com.tinder.scarlet.internal.common.SUCCESSFUL_DESERIALIZATION
import com.tinder.scarlet.internal.common.TEXT
import com.tinder.scarlet.internal.common.THROWABLE
import io.reactivex.Flowable
import io.reactivex.Maybe
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness

@RunWith(Parameterized::class)
internal class StateTransitionAdaptersTest(
    private val input: List<StateTransition>,
    private val adapter: StateTransitionAdapter<*>,
    private val output: List<*>
) {
    @Suppress("UNUSED")
    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    @Test
    fun adapt() {
        // When
        val results = Flowable.fromIterable(input)
            .flatMapMaybe { it -> adapter.adapt(it)?.let { Maybe.just(it) } ?: Maybe.empty() }
            .blockingIterable()
            .toList()

        // Then
        assertThat(results).isEqualTo(output)
    }

    companion object {

        private val messageAdapter = mock<MessageAdapter<Any>>()
            .apply {
                given(fromMessage(MESSAGE)).willReturn(TEXT)
                given(fromMessage(MALFORMED_MESSAGE)).willThrow(THROWABLE)
            }

        @Parameterized.Parameters(name = "{index}: {1}: {0} => {2}")
        @JvmStatic
        fun data() = listOf(
            param(
                input = SERVER_MESSAGES,
                adapter = StateTransitionToStateTransitionAdapter(),
                output = SERVER_MESSAGES
            ),
            param(
                input = SERVER_CLOSURE,
                adapter = StateTransitionToStateTransitionAdapter(),
                output = SERVER_CLOSURE
            ),
            param(
                input = CLIENT_CLOSURE,
                adapter = StateTransitionToStateTransitionAdapter(),
                output = CLIENT_CLOSURE
            ),
            param(
                input = CLIENT_ABORT,
                adapter = StateTransitionToStateTransitionAdapter(),
                output = CLIENT_ABORT
            ),
            param(
                input = SERVER_MESSAGES,
                adapter = StateTransitionToStateAdapter(),
                output = SERVER_MESSAGES.map { it.toState }),
            param(
                input = SERVER_CLOSURE,
                adapter = StateTransitionToStateAdapter(),
                output = SERVER_CLOSURE.map { it.toState }),
            param(
                input = CLIENT_CLOSURE,
                adapter = StateTransitionToStateAdapter(),
                output = CLIENT_CLOSURE.map { it.toState }),
            param(
                input = CLIENT_ABORT,
                adapter = StateTransitionToStateAdapter(),
                output = CLIENT_ABORT.map { it.toState }),
            param(
                input = SERVER_MESSAGES,
                adapter = StateTransitionToEventAdapter(),
                output = SERVER_MESSAGES.map { it.event }),
            param(
                input = SERVER_CLOSURE,
                adapter = StateTransitionToEventAdapter(),
                output = SERVER_CLOSURE.map { it.event }),
            param(
                input = CLIENT_CLOSURE,
                adapter = StateTransitionToEventAdapter(),
                output = CLIENT_CLOSURE.map { it.event }),
            param(
                input = CLIENT_ABORT,
                adapter = StateTransitionToEventAdapter(),
                output = CLIENT_ABORT.map { it.event }),
            param(
                input = SERVER_MESSAGES,
                adapter = StateTransitionToLifecycleStateAdapter(),
                output = SERVER_MESSAGES.mapNotNull { (it.event as? Event.OnLifecycleStateChange)?.lifecycleState }),
            param(
                input = SERVER_CLOSURE,
                adapter = StateTransitionToLifecycleStateAdapter(),
                output = SERVER_CLOSURE.mapNotNull { (it.event as? Event.OnLifecycleStateChange)?.lifecycleState }),
            param(
                input = CLIENT_CLOSURE,
                adapter = StateTransitionToLifecycleStateAdapter(),
                output = CLIENT_CLOSURE.mapNotNull { (it.event as? Event.OnLifecycleStateChange)?.lifecycleState }),
            param(
                input = CLIENT_ABORT,
                adapter = StateTransitionToLifecycleStateAdapter(),
                output = CLIENT_ABORT.mapNotNull { (it.event as? Event.OnLifecycleStateChange)?.lifecycleState }),
            param(
                input = SERVER_MESSAGES,
                adapter = StateTransitionToProtocolEventAdapter(),
                output = SERVER_MESSAGES.mapNotNull { (it.event as? Event.OnProtocolEvent)?.protocolEvent }),
            param(
                input = SERVER_CLOSURE,
                adapter = StateTransitionToProtocolEventAdapter(),
                output = SERVER_CLOSURE.mapNotNull { (it.event as? Event.OnProtocolEvent)?.protocolEvent }),
            param(
                input = CLIENT_CLOSURE,
                adapter = StateTransitionToProtocolEventAdapter(),
                output = CLIENT_CLOSURE.mapNotNull { (it.event as? Event.OnProtocolEvent)?.protocolEvent }),
            param(
                input = CLIENT_ABORT,
                adapter = StateTransitionToProtocolEventAdapter(),
                output = CLIENT_ABORT.mapNotNull { (it.event as? Event.OnProtocolEvent)?.protocolEvent }),
            param(
                input = SERVER_MESSAGES,
                adapter = StateTransitionToDeserializationAdapter(messageAdapter),
                output = listOf(
                    SUCCESSFUL_DESERIALIZATION,
                    FAILED_DESERIALIZATION,
                    FAILED_DESERIALIZATION,
                    SUCCESSFUL_DESERIALIZATION,
                    FAILED_DESERIALIZATION
                )
            ),
            param(
                input = SERVER_CLOSURE,
                adapter = StateTransitionToDeserializationAdapter(messageAdapter),
                output = listOf(
                    SUCCESSFUL_DESERIALIZATION,
                    FAILED_DESERIALIZATION
                )
            ),
            param(
                input = CLIENT_CLOSURE,
                adapter = StateTransitionToDeserializationAdapter(messageAdapter),
                output = emptyList<Any>()
            ),
            param(
                input = CLIENT_ABORT,
                adapter = StateTransitionToDeserializationAdapter(messageAdapter),
                output = emptyList<Any>()
            ),
            param(
                input = SERVER_MESSAGES,
                adapter = StateTransitionToDeserializedValueAdapter(messageAdapter),
                output = listOf(TEXT, TEXT)
            ),
            param(
                input = SERVER_CLOSURE,
                adapter = StateTransitionToDeserializedValueAdapter(messageAdapter),
                output = listOf(TEXT)
            ),
            param(
                input = CLIENT_CLOSURE,
                adapter = StateTransitionToDeserializedValueAdapter(messageAdapter),
                output = emptyList<Any>()
            ),
            param(
                input = CLIENT_ABORT,
                adapter = StateTransitionToDeserializedValueAdapter(messageAdapter),
                output = emptyList<Any>()
            )
        )

        private fun param(
            input: List<StateTransition>,
            adapter: StateTransitionAdapter<*>,
            output: List<*>
        ) =
            arrayOf(input, adapter, output)
    }
}