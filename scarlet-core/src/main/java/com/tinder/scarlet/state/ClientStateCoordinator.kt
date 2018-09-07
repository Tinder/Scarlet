/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Message
import com.tinder.scarlet.Topic
import com.tinder.scarlet.state.utils.Client

class ClientStateCoordinator(
    private val serviceLocator: ServiceLocator
) {
    private val clientStateMachine =
        Client.create { transition ->
            val sideEffect = transition.sideEffect!!
            when (sideEffect) {
                is Client.SideEffect.SendMessage -> {
                    serviceLocator.messageCoordinator.sendAndRetry(
                        sideEffect.topic,
                        sideEffect.message
                    )
                }
                is Client.SideEffect.FinishSendingMessage -> {
                    serviceLocator.messageCoordinator.clear(sideEffect.topic, sideEffect.message)
                }

                is Client.SideEffect.ReceiveMessage -> {
                    // TODO write to subject?
                }
                is Client.SideEffect.Open -> {
                    serviceLocator.protocolCoordinator.openAndRetry()
                    serviceLocator.topicCoordinator.subscribeAndRetry(sideEffect.topics)
                    serviceLocator.messageCoordinator.sendAndRetry(sideEffect.messages)

                }
                is Client.SideEffect.Close -> {
                    serviceLocator.protocolCoordinator.close()
                    serviceLocator.topicCoordinator.unsubscribe(sideEffect.topics)
                    serviceLocator.messageCoordinator.clear(sideEffect.messages)
                }
                is Client.SideEffect.Subscribe -> {
                    serviceLocator.topicCoordinator.subscribeAndRetry(sideEffect.topic)
                }
                is Client.SideEffect.Unsubscribe -> {
                    serviceLocator.topicCoordinator.unsubscribe(sideEffect.topic)
                }
            }
        }

    fun start() {
        serviceLocator.protocolCoordinator.start()
    }

    fun observeEvent() {

    }

    fun publish(topic: Topic, message: Message) {
        clientStateMachine.transition(
            Client.Event.OnShouldSendMessage(
                topic,
                message
            )
        )
    }

    fun finishSending(topic: Topic, message: Message) {
        clientStateMachine.transition(
            Client.Event.OnShouldFinishSendingMessage(
                topic,
                message
            )
        )
    }


    fun receive(topic: Topic, message: Message) {
        clientStateMachine.transition(
            Client.Event.OnShouldReceiveMessage(
                topic,
                message
            )
        )
    }

    fun connect() {
        clientStateMachine.transition(Client.Event.OnShouldOpen)
    }

    fun disconnect() {
        clientStateMachine.transition(Client.Event.OnShouldClose)
    }

    fun subscribe(topic: Topic) {
        clientStateMachine.transition(
            Client.Event.OnShouldSubscribe(
                topic
            )
        )
    }

    fun unsubscribe(topic: Topic) {
        clientStateMachine.transition(
            Client.Event.OnShouldUnsubscribe(
                topic
            )
        )
    }

}
