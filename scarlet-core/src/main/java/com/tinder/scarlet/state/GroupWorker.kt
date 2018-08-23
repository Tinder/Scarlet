/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.StateMachine
import com.tinder.scarlet.Message
import com.tinder.scarlet.RequestFactory
import com.tinder.scarlet.state.Worker.Event.OnLifecycleDestroyed
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStarted
import com.tinder.scarlet.state.Worker.Event.OnLifecycleStopped
import com.tinder.scarlet.state.Worker.Event.OnShouldStart
import com.tinder.scarlet.state.Worker.Event.OnWorkFailed
import com.tinder.scarlet.state.Worker.Event.OnWorkStarted
import com.tinder.scarlet.state.Worker.Event.OnWorkStopped
import com.tinder.scarlet.state.Worker.SideEffect.ForceStopWork
import com.tinder.scarlet.state.Worker.SideEffect.ScheduleRetry
import com.tinder.scarlet.state.Worker.SideEffect.StartWork
import com.tinder.scarlet.state.Worker.SideEffect.StopWork
import com.tinder.scarlet.state.Worker.SideEffect.UnscheduleRetry
import com.tinder.scarlet.state.Worker.State.Destroyed
import com.tinder.scarlet.state.Worker.State.Started
import com.tinder.scarlet.state.Worker.State.Starting
import com.tinder.scarlet.state.Worker.State.Stopped
import com.tinder.scarlet.state.Worker.State.Stopping
import com.tinder.scarlet.state.Worker.State.WillStart

internal object GroupWorker {

    fun createMessager(
        // request factory
        message: Message,
        listener: (StateMachine.Transition.Valid<Worker.State, Worker.Event, Worker.SideEffect>) -> Unit
    ): StateMachine<Worker.State, Worker.Event, Worker.SideEffect> {
        return Worker.create<Message, Unit, Unit, Unit>(
            object : RequestFactory<Message> {
                override fun createRequest() = message
            },
            object : RequestFactory<Unit> {
                override fun createRequest() = Unit
            },
            listener
        )
    }

    fun <START_REQUEST : Any, START_RESPONSE : Any, STOP_REQUEST : Any, STOP_RESPONSE : Any> create(
        // request factory
        startRequestFactory: RequestFactory<START_REQUEST>,
        stopRequestFactory: RequestFactory<STOP_REQUEST>,
        listener: (StateMachine.Transition.Valid<Worker.State, Worker.Event, Worker.SideEffect>) -> Unit
    ): StateMachine<Worker.State, Worker.Event, Worker.SideEffect> {
        return StateMachine.create {
            initialState(Stopped<STOP_REQUEST, STOP_RESPONSE>())
            state<Stopped<STOP_REQUEST, STOP_RESPONSE>> {
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
                    val clientOption = startRequestFactory.createRequest()
                    transitionTo(Starting(retryCount, clientOption), StartWork(clientOption))
                }
                on<OnLifecycleStopped> {
                    transitionTo(Stopped<STOP_REQUEST, STOP_RESPONSE>(), UnscheduleRetry)
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, UnscheduleRetry)
                }
            }
            state<Starting<START_REQUEST>> {
                on<OnWorkStarted<START_REQUEST>> {
                    transitionTo(Started<START_REQUEST, START_RESPONSE>())
                }
                on<OnWorkFailed> {
                    transitionTo(
                        WillStart(retryCount + 1),
                        ScheduleRetry(retryCount)
                    )
                }
            }
            state<Started<START_REQUEST, START_RESPONSE>> {
                on<OnLifecycleStopped> {
                    val clientOption = stopRequestFactory.createRequest()
                    transitionTo(Stopping(clientOption), StopWork(clientOption))
                }
                on<OnLifecycleDestroyed> {
                    transitionTo(Destroyed, ForceStopWork<STOP_REQUEST>())
                }
                on<OnWorkFailed> {
                    transitionTo(
                        WillStart(retryCount = 0),
                        ScheduleRetry(0)
                    )
                }
            }
            state<Stopping<STOP_REQUEST>> {
                on<OnWorkStopped<STOP_RESPONSE>> {
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
}
