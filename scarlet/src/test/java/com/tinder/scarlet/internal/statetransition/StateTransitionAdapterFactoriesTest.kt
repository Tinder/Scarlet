/*
 * © 2019 Match Group, LLC.
 */

package com.tinder.scarlet.internal.statetransition

import com.tinder.scarlet.internal.common.Types
import com.tinder.scarlet.internal.common.toMessageTypeAndAnnotations
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapterFactoriesTest.Expectation.IGNORED
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapterFactoriesTest.Expectation.ILLEGAL
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapterFactoriesTest.Expectation.SUPPORTED
import com.tinder.scarlet.internal.utils.MessageAdapterResolver
import com.tinder.scarlet.messageadapter.builtin.BuiltInMessageAdapterFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.quality.Strictness
import java.lang.reflect.Type
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberFunctions

@RunWith(Parameterized::class)
internal class StateTransitionAdapterFactoriesTest(
    private val factory: StateTransitionAdapter.Factory,
    private val adapterClazz: Class<*>,
    private val typeAndAnnotations: Pair<Type, Array<Annotation>>,
    private val expectation: Expectation
) {

    @Suppress("UNUSED")
    @get:Rule
    val mockitoRule: MockitoRule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS)

    @Test
    fun create() {
        // Given
        val (type, annotations) = typeAndAnnotations

        // Then
        when (expectation) {
            SUPPORTED -> {
                val adapter = factory.create(type, annotations)
                assertThat(adapter).isNotNull()
                assertThat(adapter).isInstanceOf(adapterClazz)
            }
            IGNORED -> {
                val adapter = factory.create(type, annotations)
                assertThat(adapter).isNull()
            }
            ILLEGAL -> {
                assertThatIllegalStateException()
                    .isThrownBy {
                        factory.create(type, annotations)
                    }
            }
        }
    }

    enum class Expectation {
        SUPPORTED,
        IGNORED,
        ILLEGAL
    }

    companion object {

        private val messageAdapterResolver =
            MessageAdapterResolver(listOf(BuiltInMessageAdapterFactory()))

        @Parameterized.Parameters(name = "{index}: {0} should support {1} ? {2}")
        @JvmStatic
        fun data() = listOf(
            param(
                StateTransitionToStateTransitionAdapter.Factory(),
                StateTransitionToStateTransitionAdapter::class.java,
                listOf(Types::streamOfStateTransition)
            ),
            param(
                StateTransitionToStateAdapter.Factory(),
                StateTransitionToStateAdapter::class.java,
                listOf(Types::streamOfState),
                listOf(
                    Types::streamOfStateSubclassDisconnecting,
                    Types::streamOfStateSubclassDisconnected
                )
            ),
            param(
                StateTransitionToEventAdapter.Factory(),
                StateTransitionToEventAdapter::class.java,
                listOf(Types::streamOfEvent),
                listOf(
                    Types::streamOfEventSubclassOnProtocolEvent,
                    Types::streamOfEventSubclassOnRetry,
                    Types::streamOfEventSubclassOnLifecycle
                )
            ),
            param(
                StateTransitionToLifecycleStateAdapter.Factory(),
                StateTransitionToLifecycleStateAdapter::class.java,
                listOf(Types::streamOfLifecycleState),
                listOf(
                    Types::streamOfLifecycleStateSubclassStarted,
                    Types::streamOfLifecycleStateSubclassStopped
                )
            ),
            param(
                StateTransitionToProtocolEventAdapter.Factory(),
                StateTransitionToProtocolEventAdapter::class.java,
                listOf(Types::streamOfProtocolEvent),
                listOf(
                    Types::streamOfProtocolEventSubclassOnConnectionOpened,
                    Types::streamOfProtocolEventSubclassOnMessageReceived
                )
            ),
            param(
                StateTransitionToDeserializationAdapter.Factory(messageAdapterResolver),
                StateTransitionToDeserializationAdapter::class.java,
                listOf(Types::streamOfDeserializationOfString)
            ),
            param(
                StateTransitionToDeserializedValueAdapter.Factory(messageAdapterResolver),
                StateTransitionToDeserializedValueAdapter::class.java,
                listOf(Types::streamOfString, Types::streamOfByteArray)
            )
        ).flatten()

        private fun param(
            factory: StateTransitionAdapter.Factory,
            adapterClazz: Class<*>,
            shouldSupport: List<KFunction<*>>,
            shouldThrow: List<KFunction<*>> = emptyList()
        ): List<Array<*>> {
            val supportingTypes = shouldSupport.map { it.toMessageTypeAndAnnotations() }
            val throwingTypes = shouldThrow.map { it.toMessageTypeAndAnnotations() }
            val ignoringTypes =
                (Types::class.declaredMemberFunctions - (shouldSupport + shouldThrow))
                    .map { it.toMessageTypeAndAnnotations() }
            return supportingTypes.map { arrayOf(factory, adapterClazz, it, SUPPORTED) } +
                    throwingTypes.map { arrayOf(factory, adapterClazz, it, ILLEGAL) } +
                    ignoringTypes.map { arrayOf(factory, adapterClazz, it, IGNORED) }
        }
    }
}