/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import com.tinder.scarlet.internal.coordinator.Coordinator
import com.tinder.scarlet.internal.coordinator.LifecycleEventSource
import com.tinder.scarlet.internal.coordinator.Session
import com.tinder.scarlet.internal.coordinator.StateMachineFactory
import com.tinder.scarlet.internal.coordinator.TimerEventSource
import com.tinder.scarlet.internal.statetransition.DeserializationStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.DeserializedValueStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.EventStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.LifecycleStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.NoOpStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.ProtocolEventStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.ProtocolSpecificEventStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.StateStateTransitionAdapter
import com.tinder.scarlet.internal.statetransition.StateTransitionAdapterResolver
import com.tinder.scarlet.internal.stub.ProxyFactory
import com.tinder.scarlet.internal.stub.StubInterface
import com.tinder.scarlet.internal.stub.StubMethod
import com.tinder.scarlet.internal.utils.MessageAdapterResolver
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.internal.utils.StreamAdapterResolver
import com.tinder.scarlet.lifecycle.DefaultLifecycle
import com.tinder.scarlet.messageadapter.builtin.BuiltInMessageAdapterFactory
import com.tinder.scarlet.retry.BackoffStrategy
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.streamadapter.builtin.BuiltInStreamAdapterFactory
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

class Scarlet internal constructor(
    private val stubInterfaceFactory: StubInterface.Factory,
    private val proxyFactory: ProxyFactory
) {

    fun <T> create(service: Class<T>): T {
        val stubInterface = stubInterfaceFactory.create(service)
        return proxyFactory.create(service, stubInterface)
    }

    /**
     * Same as [create].
     */
    inline fun <reified T : Any> create(): T = create(T::class.java)

    data class Configuration(
        val topic: Topic = Topic.Main,
        val lifecycle: Lifecycle = DEFAULT_LIFECYCLE,
        val backoffStrategy: BackoffStrategy = DEFAULT_BACKOFF_STRATEGY,
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList(),
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val debug: Boolean = false
    )

    class Factory {

        fun create(protocol: Protocol, configuration: Configuration = Configuration()): Scarlet {

            val coordinator = Coordinator(
                StateMachineFactory(),
                Session(
                    protocol,
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
                        protocol.createEventAdapterFactory()
                    )
                )
            )

            val proxyFactory = ProxyFactory(RuntimePlatform.get())

            return Scarlet(stubInterfaceFactory, proxyFactory)
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
                    ProtocolSpecificEventStateTransitionAdapter.Factory(
                        protocolEventAdapterFactory
                    ),
                    LifecycleStateTransitionAdapter.Factory(),
                    DeserializationStateTransitionAdapter.Factory(
                        messageAdapterResolver
                    ),
                    DeserializedValueStateTransitionAdapter.Factory(
                        messageAdapterResolver
                    )
                )
            )
        }

        private fun getScheduler(isDebug: Boolean): Scheduler {
            return if (isDebug) DEBUG_SCHEDULER else DEFAULT_SCHEDULER
        }
    }

    private companion object {
        private val DEFAULT_LIFECYCLE = DefaultLifecycle()
        private const val RETRY_BASE_DURATION = 1000L
        private const val RETRY_MAX_DURATION = 10000L
        private val DEFAULT_BACKOFF_STRATEGY =
            ExponentialBackoffStrategy(
                RETRY_BASE_DURATION,
                RETRY_MAX_DURATION
            )
        private val DEFAULT_SCHEDULER = Schedulers.computation()
        private val DEBUG_SCHEDULER = Schedulers.trampoline()
    }
}
