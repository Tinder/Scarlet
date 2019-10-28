/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.tinder.StateMachine
import com.tinder.scarlet.Event
import com.tinder.scarlet.SideEffect
import com.tinder.scarlet.State
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.internal.stub.StubInterface
import com.tinder.scarlet.internal.stub.StubMethod
import com.tinder.scarlet.utils.toStream
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.processors.PublishProcessor

internal class Coordinator(
    private val stateMachineFactory: StateMachineFactory,
    private val session: Session,
    private val lifecycleEventSource: LifecycleEventSource,
    private val timerEventSource: TimerEventSource,
    private val scheduler: Scheduler
) : StubInterface.Callback, EventCallback {

    private val stateMachine = stateMachineFactory.create()
    private val publishProcessor = PublishProcessor.create<StateTransition>()

    fun start() {
        session.start(this)
        lifecycleEventSource.start(this)
    }

    @Synchronized
    override fun send(stubMethod: StubMethod.Send, data: Any): Any {
        val message = stubMethod.messageAdapter.toMessage(data)
        return session.send(message)
    }

    @Synchronized
    override fun receive(stubMethod: StubMethod.Receive): Any {
        val stream = Flowable.defer<StateTransition> { publishProcessor }
            .onBackpressureBuffer()
            .observeOn(scheduler)
                // TODO handle deserialization here
                // TODO pass deserialzed value to protocol event so that sse event can use deserailized message
            .flatMap { stubMethod.stateTransitionAdatper.adapt(it)?.let { Flowable.just(it) } ?: Flowable.empty() }
            .toStream()
        return stubMethod.streamAdapter.adapt(stream)
    }

    @Synchronized
    override fun onEvent(event: Event) {
        val transition = stateMachine.transition(event) as? StateMachine.Transition.Valid ?: return

        when (transition.toState) {
            is State.WillConnect -> {
                lifecycleEventSource.resume()
            }
            is State.Connecting -> {
                lifecycleEventSource.pause()
            }
            is State.Connected -> {
                lifecycleEventSource.resume()
            }
            is State.Disconnecting -> {
                lifecycleEventSource.pause()
            }
            is State.Disconnected -> {
                lifecycleEventSource.resume()
            }
            is State.Destroyed -> {
                session.stop()
                lifecycleEventSource.stop()
            }
        }

        with(transition.sideEffect) {
            when (this) {
                is SideEffect.OpenProtocol -> {
                    session.openSession()
                }
                is SideEffect.CloseProtocol -> {
                    session.closeSession()
                }
                is SideEffect.ForceCloseProtocol -> {
                    session.forceCloseSession()
                }
                is SideEffect.ScheduleRetry -> {
                    timerEventSource.start(retryCount, this@Coordinator)
                }
                is SideEffect.CancelRetry -> {
                    timerEventSource.stop()
                }
            }
        }

        publishProcessor.onNext(
            StateTransition(
                transition.fromState,
                transition.event,
                transition.toState,
                transition.sideEffect
            )
        )

        when (transition.toState) {
            is State.Destroyed -> {
                publishProcessor.onComplete()
            }
        }
    }
}
