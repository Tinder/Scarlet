/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.StateMachine
import com.tinder.StateMachine.Companion.create
import com.tinder.scarlet.ConfigFactory
import com.tinder.scarlet.state.Worker.Event.OnConnectionClosed
import com.tinder.scarlet.state.Worker.Event.OnConnectionFailed
import com.tinder.scarlet.state.Worker.Event.OnConnectionOpened
import com.tinder.scarlet.state.Worker.Event.OnLifecycleDestroyed
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStarted
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStopped
import com.tinder.scarlet.state.Worker.Event.OnShouldOpenConnection
import com.tinder.scarlet.state.Worker.SideEffect.CloseConnection
import com.tinder.scarlet.state.Worker.SideEffect.ForceCloseConnection
import com.tinder.scarlet.state.Worker.SideEffect.OpenConnection
import com.tinder.scarlet.state.Worker.SideEffect.ScheduleRetry
import com.tinder.scarlet.state.Worker.SideEffect.UnscheduleRetry
import com.tinder.scarlet.state.Worker.State.Closed
import com.tinder.scarlet.state.Worker.State.Closing
import com.tinder.scarlet.state.Worker.State.Destroyed
import com.tinder.scarlet.state.Worker.State.Opened
import com.tinder.scarlet.state.Worker.State.Opening
import com.tinder.scarlet.state.Worker.State.WillOpen

internal object Worker {

    fun create(
        configFactory: ConfigFactory,
        listener: (StateMachine.Transition.Valid<Worker.State, Worker.Event, Worker.SideEffect>) -> Unit
    ): StateMachine<State, Event, SideEffect> {
        return create {
            initialState(Closed())
            state<Closed> {
                on<OnLifecycleStarted> {
                    transitionTo(
                        WillOpen(retryCount = 0),
                        ScheduleRetry(0)
                    )
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed)
                }
            }
            state<WillOpen> {
                on<OnShouldOpenConnection> {
                    val clientOption = configFactory.createClientOpenOption()
                    transitionTo(Opening(retryCount, clientOption), OpenConnection(clientOption))
                }
                on<OnLifecycleStopped> {
                    transitionTo(Closed(), UnscheduleRetry)
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, UnscheduleRetry)
                }
            }
            state<Opening> {
                on<OnConnectionOpened> {
                    transitionTo(Opened())
                }
                on<OnConnectionFailed> {
                    transitionTo(
                        WillOpen(retryCount + 1),
                        ScheduleRetry(retryCount)
                    )
                }
            }
            state<Opened> {
                on<OnLifecycleStopped> {
                    val clientOption = configFactory.createClientCloseOption()
                    transitionTo(Closing(clientOption), CloseConnection(clientOption))
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, ForceCloseConnection())
                }
                on<OnConnectionFailed> {
                    transitionTo(
                        WillOpen(retryCount = 0),
                        ScheduleRetry(0)
                    )
                }
            }
            state<Closing> {
                on<OnConnectionClosed> {
                    transitionTo(Closed(clientOption, it.serverOption))
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
        data class Opening internal constructor(
            val retryCount: Int,
            val clientOption: Any? = null
        ) : State()

        data class Opened internal constructor(
            val clientOption: Any? = null,
            val serverOption: Any? = null
        ) : State()

        data class Closing internal constructor(
            val clientOption: Any? = null
        ) : State()

        data class Closed internal constructor(
            val clientOption: Any? = null,
            val serverOption: Any? = null
        ) : State()

        data class WillOpen internal constructor(
            val retryCount: Int
        ) : State()

        object Destroyed : State()
    }

    sealed class Event {
        object OnLifecycleStarted : Event()

        object OnLifecycleStopped : Event()

        object OnLifecycleDestroyed : Event()

        object OnShouldOpenConnection : Event()

        data class OnConnectionOpened(
            val clientOption: Any?,
            val serverOption: Any?
        ) : Event()

        data class OnConnectionClosed(
            val clientOption: Any?,
            val serverOption: Any?
        ) : Event()

        data class OnConnectionFailed(
            val throwable: Throwable
        ) : Event()

    }

    sealed class SideEffect {
        data class ScheduleRetry(val retryCount: Int) : SideEffect()
        object UnscheduleRetry : SideEffect()

        data class OpenConnection(val option: Any? = null) : SideEffect()
        data class CloseConnection(val option: Any? = null) : SideEffect()
        data class ForceCloseConnection(val option: Any? = null) : SideEffect()
    }

}
