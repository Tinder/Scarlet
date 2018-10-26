/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.tinder.StateMachine
import com.tinder.scarlet.Event
import com.tinder.scarlet.SideEffect
import com.tinder.scarlet.State
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.utils.toStream
import com.tinder.scarlet.internal.stub.StubInterface
import com.tinder.scarlet.internal.stub.StubMethod
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
        lifecycleEventSource.start(this)
        session.start(this)
    }

    @Synchronized
    override fun send(stubMethod: StubMethod.Send, data: Any): Any {
        val message = stubMethod.messageAdapter.toMessage(data)
        return session.send(message)
    }

    @Synchronized
    override fun receive(stubMethod: StubMethod.Receive): Any {
        val stream = Flowable.defer<StateTransition> { publishProcessor }
            .observeOn(scheduler)
            .flatMap { stubMethod.stateTransitionAdatper.adapt(it)?.let { Flowable.just(it) } ?: Flowable.empty() }
            .toStream()
        return stubMethod.streamAdapter.adapt(stream)
    }

    @Synchronized
    override fun onEvent(event: Event) {
        val transition = stateMachine.transition(event) as? StateMachine.Transition.Valid ?: return
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
            is State.WillConnect -> {
                lifecycleEventSource.requestNext()
            }
            is State.Connected -> {
                lifecycleEventSource.requestNext()
            }
            is State.Disconnected -> {
                lifecycleEventSource.requestNext()
            }
            is State.Destroyed -> {
                session.stop()
                publishProcessor.onComplete()
            }
        }
    }
}
