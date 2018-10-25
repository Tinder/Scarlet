/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.internal.servicemethod.MessageAdapterResolver
import com.tinder.scarlet.internal.servicemethod.StreamAdapterResolver
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.messageadapter.builtin.BuiltInMessageAdapterFactory
import com.tinder.scarlet.retry.BackoffStrategy
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.streamadapter.builtin.BuiltInStreamAdapterFactory
import com.tinder.scarlet.v2.lifecycle.DefaultLifecycle
import com.tinder.scarlet.v2.service.Coordinator
import com.tinder.scarlet.v2.service.LifecycleEventSource
import com.tinder.scarlet.v2.service.Session
import com.tinder.scarlet.v2.service.StateMachineFactory
import com.tinder.scarlet.v2.service.TimerEventSource
import com.tinder.scarlet.v2.stub.StubInterface
import com.tinder.scarlet.v2.stub.StubMethod
import com.tinder.scarlet.v2.transitionadapter.DeserializationStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.DeserializedValueStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.EventStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.LifecycleStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.NoOpStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.ProtocolEventStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.ProtocolSpecificEventStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.StateStateTransitionAdapter
import com.tinder.scarlet.v2.transitionadapter.StateTransitionAdapterResolver
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

class Scarlet internal constructor(
    private val stubInterfaceFactory: StubInterface.Factory
) {

    fun <T> create(service: Class<T>): T = stubInterfaceFactory.create(service)

    /**
     * Same as [create].
     */
    inline fun <reified T : Any> create(): T = create(T::class.java)

    data class Configuration(
        val protocol: Protocol,
        val topic: Topic = Topic.Main,
        val lifecycle: Lifecycle = DEFAULT_LIFECYCLE,
        val backoffStrategy: BackoffStrategy = DEFAULT_BACKOFF_STRATEGY,
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList(),
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val debug: Boolean = false
    )

    class Factory {

        // TODO protocol coordinator cache

        fun create(configuration: Configuration): Scarlet {

            val coordinator = Coordinator(
                StateMachineFactory(),
                Session(
                    configuration.protocol,
                    configuration.topic
                ),
                LifecycleEventSource(
                    configuration.lifecycle
                ),
                TimerEventSource(
                    getScheduler(configuration.debug),
                    configuration.backoffStrategy
                ),
                getScheduler(configuration.debug)
            )
            coordinator.start()

            val messageAdapterResolver = configuration.createMessageAdapterResolver()

            val stubInterfaceFactory = StubInterface.Factory(
                RuntimePlatform.get(),
                coordinator,
                StubMethod.Factory(
                    configuration.createStreamAdapterResolver(),
                    messageAdapterResolver,
                    createStateTransitionAdapterResolver(
                        messageAdapterResolver,
                        configuration.protocol.createEventAdapterFactory()
                    )
                )
            )

            return Scarlet(stubInterfaceFactory)
        }

        private fun Configuration.createStreamAdapterResolver(): StreamAdapterResolver {
            return StreamAdapterResolver(streamAdapterFactories + BuiltInStreamAdapterFactory())
        }

        private fun Configuration.createMessageAdapterResolver(): MessageAdapterResolver {
            return MessageAdapterResolver(messageAdapterFactories + BuiltInMessageAdapterFactory())
        }

        private fun createStateTransitionAdapterResolver(
            messageAdapterResolver: MessageAdapterResolver,
            protocolEventAdapterFactory: ProtocolEventAdapter.Factory
        ): StateTransitionAdapterResolver {
            return StateTransitionAdapterResolver(
                listOf(
                    NoOpStateTransitionAdapter.Factory(),
                    EventStateTransitionAdapter.Factory(),
                    StateStateTransitionAdapter.Factory(),
                    ProtocolEventStateTransitionAdapter.Factory(),
                    ProtocolSpecificEventStateTransitionAdapter.Factory(protocolEventAdapterFactory),
                    LifecycleStateTransitionAdapter.Factory(),
                    DeserializationStateTransitionAdapter.Factory(messageAdapterResolver),
                    DeserializedValueStateTransitionAdapter.Factory(messageAdapterResolver)
                )
            )
        }

        private fun getScheduler(isDebug: Boolean): Scheduler {
            return if (isDebug) DEBUG_SCHEDULER else DEFAULT_SCHEDULER
        }
    }

    private companion object {
        private val DEFAULT_LIFECYCLE = DefaultLifecycle()
        private val RETRY_BASE_DURATION = 1000L
        private val RETRY_MAX_DURATION = 10000L
        private val DEFAULT_BACKOFF_STRATEGY =
            ExponentialBackoffStrategy(RETRY_BASE_DURATION, RETRY_MAX_DURATION)
        private val DEFAULT_SCHEDULER = Schedulers.computation()
        private val DEBUG_SCHEDULER = Schedulers.trampoline()
    }
}
