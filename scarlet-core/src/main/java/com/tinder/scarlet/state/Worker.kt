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

internal class Worker<CONTEXT : Any, START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any>(
    private val startRequestFactory: RequestFactory<START_REQUEST>,
    private val stopRequestFactory: RequestFactory<STOP_REQUEST>,
    listener: (Transition<CONTEXT, START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>) -> Unit
) {

    private val sideEffect = SideEffect()
    private val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State().Stopped())
        state<Stopped> {
            on<OnLifecycleStarted<CONTEXT>> {
                transitionTo(
                    WillStart(context = it.context, retryCount = 0),
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
                transitionTo(
                    Starting(context, retryCount, request),
                    sideEffect.StartWork(context, request)
                )
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
                transitionTo(Started(context, request, it.response))
            }
            on<OnWorkFailed> {
                transitionTo(
                    WillStart(context, retryCount + 1),
                    sideEffect.ScheduleRetry(retryCount)
                )
            }
        }
        state<Started> {
            on<OnLifecycleStopped> {
                val request = stopRequestFactory.createRequest()
                transitionTo(Stopping(context, request), sideEffect.StopWork(context, request))
            }
            on<OnLifecycleDestroyed> {
                transitionTo(Destroyed(), sideEffect.ForceStopWork(context))
            }
            on<OnWorkFailed> {
                transitionTo(
                    WillStart(context, retryCount = 0),
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

    fun onLifecycleStarted(context: CONTEXT) {
        stateMachine.transition(OnLifecycleStarted(context))
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

    data class Transition<CONTEXT : Any, START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any>(
        val fromState: Worker<CONTEXT, START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>.State,
        val event: Event,
        val toState: Worker<CONTEXT, START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>.State,
        val sideEffect: Worker<CONTEXT, START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>.SideEffect?
    )

    open inner class State {
        inner class Starting internal constructor(
            val context: CONTEXT,
            val retryCount: Int,
            val request: START_REQUEST? = null
        ) : State()

        inner class Started internal constructor(
            val context: CONTEXT,
            val request: START_REQUEST? = null,
            val response: START_RESPONSE? = null
        ) : State()

        inner class Stopping internal constructor(
            val context: CONTEXT,
            val request: STOP_REQUEST? = null
        ) : State()

        inner class Stopped internal constructor(
            val request: STOP_REQUEST? = null,
            val response: STOP_RESPONSE? = null
        ) : State()

        inner class WillStart internal constructor(
            val context: CONTEXT,
            val retryCount: Int
        ) : State()

        inner class Destroyed : State()
    }

    sealed class Event {
        data class OnLifecycleStarted<CONTEXT : Any>(val context: CONTEXT) : Event()

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
        inner class StartWork(val context: CONTEXT, val request: START_REQUEST? = null) : SideEffect()
        inner class StopWork(val context: CONTEXT, val request: STOP_REQUEST? = null) : SideEffect()
        inner class ForceStopWork(val context: CONTEXT, val request: STOP_REQUEST? = null) : SideEffect()
    }

    class Factory<CONTEXT : Any, START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any>(
    ) {

        fun create(
            startRequestFactory: RequestFactory<START_REQUEST>,
            stopRequestFactory: RequestFactory<STOP_REQUEST>,
            listener: (Transition<CONTEXT, START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE>) -> Unit
        ): Worker<CONTEXT, START_REQUEST, START_RESPONSE, STOP_REQUEST, STOP_RESPONSE> {
            return Worker(startRequestFactory, stopRequestFactory, listener)
        }
    }
}
