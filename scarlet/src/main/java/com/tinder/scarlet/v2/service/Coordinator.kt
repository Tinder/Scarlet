/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.service

import com.tinder.StateMachine
import com.tinder.scarlet.v2.stub.StubInterface
import com.tinder.scarlet.v2.stub.StubMethod
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject

internal class Coordinator(
    private val stateMachineFactory: StateMachineFactory,
    private val session: Session,
    private val lifecycleEventSource: LifecycleEventSource,
    private val timerEventSource: TimerEventSource,
    private val scheduler: Scheduler
) : StubInterface.Callback, EventCallback {

    private val stateMachine = stateMachineFactory.create()
    private val publishSubject = PublishSubject.create<StateMachine.Transition.Valid<State, Event, SideEffect>>()

    fun start() {
        lifecycleEventSource.start(this)
        session.start(this)
    }

    override fun send(stubMethod: StubMethod.Send, data: Any): Any {
        val message = stubMethod.messageAdapter.toMessage(data)
        return session.send(message)
    }

    override fun receive(stubMethod: StubMethod.Receive): Any {
        return publishSubject.hide()
    }

    override fun onEvent(event: Event) {
        val transition = stateMachine.transition(event) as? StateMachine.Transition.Valid ?: return
        val sideEffect = transition.sideEffect ?: return
        with(sideEffect) {
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
        publishSubject.onNext(transition)

        when (transition.toState) {
            is State.Destroyed -> {
                session.stop()
                publishSubject.onComplete()
            }
        }
    }
}
