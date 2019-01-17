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
import com.tinder.scarlet.lifecycle.LifecycleRegistry
import com.tinder.scarlet.messageadapter.builtin.BuiltInMessageAdapterFactory
import com.tinder.scarlet.retry.BackoffStrategy
import com.tinder.scarlet.retry.ExponentialBackoffStrategy
import com.tinder.scarlet.streamadapter.builtin.BuiltInStreamAdapterFactory
import com.tinder.scarlet.ws.Receive
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

class Scarlet private constructor(
    private val protocol: Protocol,
    private val configuration: Configuration = Configuration(),
    private val parent: Scarlet? = null
) {

    private val session: Session = Session(
        protocol,
        parent?.session
    )
    private val stubInterfaceFactory: StubInterface.Factory
    private val proxyFactory: ProxyFactory

    constructor(protocol: Protocol, configuration: Configuration) : this(protocol, configuration, null)

    init {
        val coordinator = Coordinator(
            StateMachineFactory(),
            session,
            LifecycleEventSource(
                getScheduler(configuration.debug),
                parentScope() ?: configuration.lifecycle
            ),
            TimerEventSource(
                getScheduler(configuration.debug),
                configuration.backoffStrategy
            ),
            getScheduler(configuration.debug)
        )
        coordinator.start()

        val messageAdapterResolver = configuration.createMessageAdapterResolver()

        stubInterfaceFactory = StubInterface.Factory(
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

        proxyFactory = ProxyFactory(RuntimePlatform.get())
    }

    fun <T> create(service: Class<T>): T {
        val stubInterface = stubInterfaceFactory.create(service)
        return proxyFactory.create(service, stubInterface)
    }

    /**
     * Same as [create].
     */
    inline fun <reified T : Any> create(): T = create(T::class.java)

    fun child(protocol: Protocol, configuration: Configuration): Scarlet {
        return Scarlet(protocol, configuration, this)
    }

    private fun parentScope(): Lifecycle? {
        val parent = parent ?: return null
        val protocolEventStream = parent.create<OnConnectionOpenService>().observeProtocolEvent()
        val parentConnectionOpenFlowable = Flowable.fromPublisher(protocolEventStream)
            .flatMap {
                when (it) {
                    is ProtocolEvent.OnOpened -> Flowable.just(LifecycleState.Started)
                    is ProtocolEvent.OnClosed,
                    is ProtocolEvent.OnFailed -> Flowable.just(LifecycleState.Stopped)
                    else -> Flowable.empty()
                }
            }
        val lifecycleRegistry = LifecycleRegistry()
        parentConnectionOpenFlowable.subscribe(lifecycleRegistry)
        return configuration.lifecycle
            .combineWith(lifecycleRegistry)
    }

    data class Configuration(
        val lifecycle: Lifecycle = DEFAULT_LIFECYCLE,
        val backoffStrategy: BackoffStrategy = DEFAULT_BACKOFF_STRATEGY,
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList(),
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList(),
        val debug: Boolean = false
    )

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

        private fun Configuration.createStreamAdapterResolver(): StreamAdapterResolver {
            return StreamAdapterResolver(streamAdapterFactories + BuiltInStreamAdapterFactory())
        }

        private fun Configuration.createMessageAdapterResolver(): MessageAdapterResolver {
            return MessageAdapterResolver(messageAdapterFactories + BuiltInMessageAdapterFactory())
        }

        private fun createStateTransitionAdapterResolver(
            messageAdapterResolver: MessageAdapterResolver,
            protocolSpecificEventAdapterFactory: ProtocolSpecificEventAdapter.Factory
        ): StateTransitionAdapterResolver {
            return StateTransitionAdapterResolver(
                listOf(
                    NoOpStateTransitionAdapter.Factory(),
                    EventStateTransitionAdapter.Factory(),
                    StateStateTransitionAdapter.Factory(),
                    ProtocolEventStateTransitionAdapter.Factory(),
                    ProtocolSpecificEventStateTransitionAdapter.Factory(
                        protocolSpecificEventAdapterFactory
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

        interface OnConnectionOpenService {
            @Receive
            fun observeProtocolEvent(): Stream<ProtocolEvent>
        }
    }
}
