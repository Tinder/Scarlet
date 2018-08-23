/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.StateMachine
import com.tinder.StateMachine.Companion.create
import com.tinder.scarlet.ConfigFactory
import com.tinder.scarlet.state.Worker.Event.OnWorkStopped
import com.tinder.scarlet.state.Worker.Event.OnWorkFailed
import com.tinder.scarlet.state.Worker.Event.OnWorkStarted
import com.tinder.scarlet.state.Worker.Event.OnLifecycleDestroyed
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStarted
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStopped
import com.tinder.scarlet.state.Worker.Event.OnShouldStart
import com.tinder.scarlet.state.Worker.SideEffect.StopWork
import com.tinder.scarlet.state.Worker.SideEffect.ForceStopWork
import com.tinder.scarlet.state.Worker.SideEffect.StartWork
import com.tinder.scarlet.state.Worker.SideEffect.ScheduleRetry
import com.tinder.scarlet.state.Worker.SideEffect.UnscheduleRetry
import com.tinder.scarlet.state.Worker.State.Stopped
import com.tinder.scarlet.state.Worker.State.Stopping
import com.tinder.scarlet.state.Worker.State.Destroyed
import com.tinder.scarlet.state.Worker.State.Started
import com.tinder.scarlet.state.Worker.State.Starting
import com.tinder.scarlet.state.Worker.State.WillStart

internal object Worker {

    fun create(
        // request factory
        configFactory: ConfigFactory,
        listener: (StateMachine.Transition.Valid<Worker.State, Worker.Event, Worker.SideEffect>) -> Unit
    ): StateMachine<State, Event, SideEffect> {
        return create {
            initialState(Stopped())
            state<Stopped> {
                on<OnLifecycleStarted> {
                    transitionTo(
                        WillStart(retryCount = 0),
                        ScheduleRetry(0)
                    )
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed)
                }
            }
            state<WillStart> {
                on<OnShouldStart> {
                    val clientOption = configFactory.createClientOpenOption()
                    transitionTo(Starting(retryCount, clientOption), StartWork(clientOption))
                }
                on<OnLifecycleStopped> {
                    transitionTo(Stopped(), UnscheduleRetry)
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, UnscheduleRetry)
                }
            }
            state<Starting> {
                on<OnWorkStarted> {
                    transitionTo(Started())
                }
                on<OnWorkFailed> {
                    transitionTo(
                        WillStart(retryCount + 1),
                        ScheduleRetry(retryCount)
                    )
                }
            }
            state<Started> {
                on<OnLifecycleStopped> {
                    val clientOption = configFactory.createClientCloseOption()
                    transitionTo(Stopping(clientOption), StopWork(clientOption))
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, ForceStopWork())
                }
                on<OnWorkFailed> {
                    transitionTo(
                        WillStart(retryCount = 0),
                        ScheduleRetry(0)
                    )
                }
            }
            state<Stopping> {
                on<OnWorkStopped> {
                    transitionTo(Stopped(request, it.response))
                }
            }
            state<Destroyed> {
            }
            onTransition {
                if (it is StateMachine.Transition.Valid) {
                    listener(it)
                }
            }
        }
    }

    sealed class State {
        data class Starting internal constructor(
            val retryCount: Int,
            val request: Any? = null
        ) : State()

        data class Started internal constructor(
            val request: Any? = null,
            val response: Any? = null
        ) : State()

        data class Stopping internal constructor(
            val request: Any? = null
        ) : State()

        data class Stopped internal constructor(
            val request: Any? = null,
            val response: Any? = null
        ) : State()

        data class WillStart internal constructor(
            val retryCount: Int
        ) : State()

        object Destroyed : State()
    }

    sealed class Event {
        object OnLifecycleStarted : Event()

        object OnLifecycleStopped : Event()

        object OnLifecycleDestroyed : Event()

        object OnShouldStart : Event()

        // task?
        data class OnWorkStarted(
            val response: Any?
        ) : Event()

        data class OnWorkStopped(
            val response: Any?
        ) : Event()

        data class OnWorkFailed(
            val throwable: Throwable
        ) : Event()

    }

    sealed class SideEffect {
        data class ScheduleRetry(val retryCount: Int) : SideEffect()
        object UnscheduleRetry : SideEffect()

        data class StartWork(val request: Any? = null) : SideEffect()
        data class StopWork(val request: Any? = null) : SideEffect()
        data class ForceStopWork(val request: Any? = null) : SideEffect()
    }

}
