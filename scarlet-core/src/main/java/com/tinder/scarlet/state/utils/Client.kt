/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state.utils

import com.tinder.StateMachine
import com.tinder.scarlet.Message
import com.tinder.scarlet.Topic

internal object Client {

    fun create(
        listener: (StateMachine.Transition.Valid<State, Event, SideEffect>) -> Unit
    ): StateMachine<State, Event, SideEffect> {
        return StateMachine.create {
            initialState(State())
            state<State> {
                on<Event.OnShouldSendMessage> {
                    val messagesInTopic = messages[it.topic] ?: emptyList()
                    transitionTo(
                        copy(messages = messages + (it.topic to messagesInTopic + it.message)),
                        SideEffect.SendMessage(
                            it.topic,
                            it.message
                        )
                    )
                }
                on<Event.OnShouldFinishSendingMessage> {
                    val messagesInTopic = messages[it.topic] ?: emptyList()
                    val currentMessagesInTopic = messagesInTopic - it.message
                    val currentMessages = if (currentMessagesInTopic.isEmpty()) {
                        messages - it.topic
                    } else {
                        messages + (it.topic to currentMessagesInTopic)
                    }
                    transitionTo(
                        copy(messages = currentMessages),
                        SideEffect.FinishSendingMessage(
                            it.topic,
                            it.message
                        )
                    )
                }
                on<Event.OnShouldReceiveMessage> {
                    transitionTo(
                        this,
                        SideEffect.ReceiveMessage(
                            it.topic,
                            it.message
                        )
                    )
                }
                on<Event.OnShouldOpen> {
                    when (isOpen) {
                        true -> dontTransition()
                        false -> transitionTo(
                            copy(isOpen = true),
                            SideEffect.Open(topics, messages)
                        )
                    }
                }
                on<Event.OnShouldClose> {
                    when (isOpen) {
                        true -> transitionTo(
                            copy(isOpen = false),
                            SideEffect.Close(topics, messages)
                        )
                        false -> dontTransition()
                    }
                }
                on<Event.OnShouldSubscribe> {
                    transitionTo(
                        copy(topics = topics + it.topic),
                        SideEffect.Subscribe(it.topic)
                    )
                }
                on<Event.OnShouldUnsubscribe> {
                    transitionTo(
                        copy(topics = topics - it.topic),
                        SideEffect.Unsubscribe(it.topic)
                    )
                }
            }
            onTransition {
                if (it is StateMachine.Transition.Valid) {
                    listener(it)
                }
            }
        }
    }

    data class State(
        val isOpen: Boolean = false,
        val topics: Set<Topic> = emptySet(),
        val messages: Map<Topic, List<Message>> = emptyMap()
    )

    sealed class Event {
        data class OnShouldSendMessage(val topic: Topic, val message: Message) : Event()
        data class OnShouldFinishSendingMessage(val topic: Topic, val message: Message) : Event()
        data class OnShouldReceiveMessage(val topic: Topic, val message: Message) : Event()

        object OnShouldOpen : Event()
        object OnShouldClose : Event()

        data class OnShouldSubscribe(val topic: Topic) : Event()
        data class OnShouldUnsubscribe(val topic: Topic) : Event()
    }

    sealed class SideEffect {
        data class SendMessage(val topic: Topic, val message: Message) : SideEffect()
        data class FinishSendingMessage(val topic: Topic, val message: Message) : SideEffect()

        data class ReceiveMessage(val topic: Topic, val message: Message) : SideEffect()

        data class Open(val topics: Set<Topic>, val messages: Map<Topic, List<Message>>) : SideEffect()
        data class Close(val topics: Set<Topic>, val messages: Map<Topic, List<Message>>) : SideEffect()

        data class Subscribe(val topic: Topic) : SideEffect()
        data class Unsubscribe(val topic: Topic) : SideEffect()
    }
}
