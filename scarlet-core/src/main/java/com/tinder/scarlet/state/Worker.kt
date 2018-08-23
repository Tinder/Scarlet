/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.StateMachine
import com.tinder.scarlet.RequestFactory
import com.tinder.scarlet.state.Worker.Event.OnLifecycleDestroyed
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStarted
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStopped
import com.tinder.scarlet.state.Worker.Event.OnShouldStart
import com.tinder.scarlet.state.Worker.Event.OnWorkFailed
import com.tinder.scarlet.state.Worker.Event.OnWorkStarted
import com.tinder.scarlet.state.Worker.Event.OnWorkStopped
import com.tinder.scarlet.state.Worker.State.Destroyed
import com.tinder.scarlet.state.Worker.State.Started
import com.tinder.scarlet.state.Worker.State.Starting
import com.tinder.scarlet.state.Worker.State.Stopped
import com.tinder.scarlet.state.Worker.State.Stopping
import com.tinder.scarlet.state.Worker.State.WillStart

internal class Worker<START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any>(
    private val startRequestFactory: RequestFactory<START_REQUEST>,
    private val stopRequestFactory: RequestFactory<STOP_REQUEST>,
    listener: (Transition<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>) -> Unit
) {

    private val sideEffect = SideEffect()
    private val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State().Stopped())
        state<Stopped> {
            on<OnLifecycleStarted> {
                transitionTo(
                    WillStart(retryCount = 0),
                    sideEffect.ScheduleRetry(0)
                )
            }
            on<OnLifecycleDestroyed> {
                transitionTo(Destroyed())
            }
        }
        state<WillStart> {
            on<OnShouldStart> {
                val request = startRequestFactory.createRequest()
                transitionTo(Starting(retryCount, request), sideEffect.StartWork(request))
            }
            on<OnLifecycleStopped> {
                transitionTo(Stopped(), sideEffect.UnscheduleRetry())
            }
            on<OnLifecycleDestroyed> {
                transitionTo(Destroyed(), sideEffect.UnscheduleRetry())
            }
        }
        state<Starting> {
            on<OnWorkStarted<START_RESPONSE>> {
                transitionTo(Started(request, it.response))
            }
            on<OnWorkFailed> {
                transitionTo(
                    WillStart(retryCount + 1),
                    sideEffect.ScheduleRetry(retryCount)
                )
            }
        }
        state<Started> {
            on<OnLifecycleStopped> {
                val request = stopRequestFactory.createRequest()
                transitionTo(Stopping(request), sideEffect.StopWork(request))
            }
            on<OnLifecycleDestroyed> {
                transitionTo(Destroyed(), sideEffect.ForceStopWork())
            }
            on<OnWorkFailed> {
                transitionTo(
                    WillStart(retryCount = 0),
                    sideEffect.ScheduleRetry(0)
                )
            }
        }
        state<Stopping> {
            on<OnWorkStopped<STOP_RESPONSE>> {
                transitionTo(Stopped(request, it.response))
            }
        }
        state<Destroyed> {
        }
        onTransition {
            if (it is StateMachine.Transition.Valid) {
                listener(
                    Transition(
                        it.fromState,
                        it.event,
                        it.toState,
                        it.sideEffect
                    )
                )
            }
        }
    }

    fun onLifecycleStarted() {
        stateMachine.transition(OnLifecycleStarted)
    }

    fun onLifecycleStopped() {
        stateMachine.transition(OnLifecycleStopped)
    }

    fun onLifecycleDestroyed() {
        stateMachine.transition(OnLifecycleDestroyed)
    }

    fun onShouldStart(response: START_RESPONSE? = null) {
        stateMachine.transition(OnShouldStart)
    }

    fun onWorkStarted(response: STOP_RESPONSE? = null) {
        stateMachine.transition(OnWorkStarted(response))
    }

    fun onWorkStopped(response: STOP_RESPONSE? = null) {
        stateMachine.transition(OnWorkStopped(response))
    }

    fun onWorkFailed(throwable: Throwable) {
        stateMachine.transition(OnWorkFailed(throwable))
    }

    data class Transition<START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any>(
        val fromState: Worker<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>.State,
        val event: Event,
        val toState: Worker<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>.State,
        val sideEffect: Worker<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>.SideEffect?
    )

    open inner class State {
        inner class Starting internal constructor(
            val retryCount: Int,
            val request: START_REQUEST? = null
        ) : State()

        inner class Started internal constructor(
            val request: START_REQUEST? = null,
            val response: START_RESPONSE? = null
        ) : State()

        inner class Stopping internal constructor(
            val request: STOP_REQUEST? = null
        ) : State()

        inner class Stopped internal constructor(
            val request: STOP_REQUEST? = null,
            val response: STOP_RESPONSE? = null
        ) : State()

        inner class WillStart internal constructor(
            val retryCount: Int
        ) : State()

        inner class Destroyed : State()
    }

    sealed class Event {
        object OnLifecycleStarted : Event()

        object OnLifecycleStopped : Event()

        object OnLifecycleDestroyed : Event()

        object OnShouldStart : Event()

        // task?
        data class OnWorkStarted<RESPONSE : Any>(
            val response: RESPONSE?
        ) : Event()

        data class OnWorkStopped<RESPONSE : Any>(
            val response: RESPONSE?
        ) : Event()

        data class OnWorkFailed(
            val throwable: Throwable
        ) : Event()
    }

    open inner class SideEffect {
        inner class ScheduleRetry(val retryCount: Int) : SideEffect()
        inner class UnscheduleRetry : SideEffect()
        inner class StartWork(val request: START_REQUEST? = null) : SideEffect()
        inner class StopWork(val request: STOP_REQUEST? = null) : SideEffect()
        inner class ForceStopWork(val request: STOP_REQUEST? = null) : SideEffect()
    }

    class Factory<START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any>(
    ) {

        fun create(
            startRequestFactory: RequestFactory<START_REQUEST>,
            stopRequestFactory: RequestFactory<STOP_REQUEST>,
            listener: (Transition<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>) -> Unit
        ): Worker<START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE> {
            return Worker(startRequestFactory, stopRequestFactory, listener)
        }
    }
}
