/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.StateMachine
import com.tinder.StateMachine.Companion.create
import com.tinder.scarlet.ConfigFactory
import com.tinder.scarlet.state.Worker.Event.OnLifecycleDestroyed
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStarted
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStopped
import com.tinder.scarlet.state.Worker.Event.OnShouldStart
import com.tinder.scarlet.state.Worker.Event.OnWorkFailed
import com.tinder.scarlet.state.Worker.Event.OnWorkStarted
import com.tinder.scarlet.state.Worker.Event.OnWorkStopped
import com.tinder.scarlet.state.Worker.SideEffect.ScheduleRetry
import com.tinder.scarlet.state.Worker.State.Destroyed
import com.tinder.scarlet.state.Worker.State.Started
import com.tinder.scarlet.state.Worker.State.Starting
import com.tinder.scarlet.state.Worker.State.Stopped
import com.tinder.scarlet.state.Worker.State.Stopping
import com.tinder.scarlet.state.Worker.State.WillStart

internal object Worker {

    fun <REQUEST : Any, RESPONSE : Any> create(
        // request factory
        configFactory: ConfigFactory,
        listener: (StateMachine.Transition.Valid<Worker.State<REQUEST, RESPONSE>, Worker.Event<REQUEST, RESPONSE>, Worker.SideEffect<REQUEST, RESPONSE>>) -> Unit
    ): StateMachine<State<REQUEST, RESPONSE>, Event<REQUEST, RESPONSE>, SideEffect<REQUEST, RESPONSE>> {
        return create {
            initialState(Stopped())
            state<Stopped<REQUEST, RESPONSE>> {
                on<OnLifecycleStarted<REQUEST, RESPONSE>> {
                    transitionTo(
                        WillStart(retryCount = 0),
                        ScheduleRetry(0)
                    )
                }
                on<OnLifecycleDestroyed<REQUEST, RESPONSE>> {
                    transitionTo(Destroyed())
                }
            }
            state<WillStart<REQUEST, RESPONSE>> {
                on<OnShouldStart<REQUEST, RESPONSE>> {
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

    sealed class State<REQUEST : Any, RESPONSE : Any> {
        data class Starting<REQUEST : Any, RESPONSE : Any> internal constructor(
            val retryCount: Int,
            val request: REQUEST? = null
        ) : State<REQUEST, RESPONSE>()

        data class Started<REQUEST : Any, RESPONSE : Any> internal constructor(
            val request: REQUEST? = null,
            val response: RESPONSE? = null
        ) : State<REQUEST, RESPONSE>()

        data class Stopping<REQUEST : Any, RESPONSE : Any> internal constructor(
            val request: REQUEST? = null
        ) : State<REQUEST, RESPONSE>()

        data class Stopped<REQUEST : Any, RESPONSE : Any> internal constructor(
            val request: REQUEST? = null,
            val response: RESPONSE? = null
        ) : State<REQUEST, RESPONSE>()

        data class WillStart<REQUEST : Any, RESPONSE : Any> internal constructor(
            val retryCount: Int
        ) : State<REQUEST, RESPONSE>()

        class Destroyed<REQUEST : Any, RESPONSE : Any> : State<REQUEST, RESPONSE>()
    }

    sealed class Event<REQUEST : Any, RESPONSE : Any> {
        class OnLifecycleStarted<REQUEST : Any, RESPONSE : Any> : Event<REQUEST, RESPONSE>()

        class OnLifecycleStopped<REQUEST : Any, RESPONSE : Any> : Event<REQUEST, RESPONSE>()

        class OnLifecycleDestroyed<REQUEST : Any, RESPONSE : Any> : Event<REQUEST, RESPONSE>()

        class OnShouldStart<REQUEST : Any, RESPONSE : Any> : Event<REQUEST, RESPONSE>()

        // task?
        data class OnWorkStarted<REQUEST : Any, RESPONSE : Any>(
            val response: RESPONSE?
        ) : Event<REQUEST, RESPONSE>()

        data class OnWorkStopped<REQUEST : Any, RESPONSE : Any>(
            val response: RESPONSE?
        ) : Event<REQUEST, RESPONSE>()

        data class OnWorkFailed<REQUEST : Any, RESPONSE : Any>(
            val throwable: Throwable
        ) : Event<REQUEST, RESPONSE>()

    }

    sealed class SideEffect<REQUEST : Any, RESPONSE : Any> {
        data class ScheduleRetry<REQUEST: Any, RESPONSE: Any> (val retryCount: Int) : SideEffect<REQUEST, RESPONSE>()
        class UnscheduleRetry<REQUEST: Any, RESPONSE: Any>  : SideEffect<REQUEST, RESPONSE>()

        data class StartWork<REQUEST : Any, RESPONSE : Any>(val request: REQUEST? = null) : SideEffect<REQUEST, RESPONSE>()
        data class StopWork<REQUEST : Any, RESPONSE : Any>(val request: REQUEST? = null) : SideEffect<REQUEST, RESPONSE>()
        data class ForceStopWork<REQUEST : Any, RESPONSE : Any>(val request: REQUEST? = null) :
            SideEffect<REQUEST, RESPONSE>()
    }

}
