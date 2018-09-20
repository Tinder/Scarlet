/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.v3

import com.tinder.scarlet.Message
import com.tinder.scarlet.Topic
import com.tinder.scarlet.v2.v3.utils.Engine

internal class EngineCoordinator(
    serviceLocator: ServiceLocator
) : ServiceLocator by serviceLocator {

    private val client = Engine.Factory().create { transition ->
        val sideEffect = transition.sideEffect ?: return@create
        when (sideEffect) {
            is Engine.SideEffect.SendMessage -> {
                messageCoordinator.sendAndRetry(sideEffect.topic, sideEffect.message)
            }
            is Engine.SideEffect.FinishSendingMessage -> {
                messageCoordinator.clear(sideEffect.topic, sideEffect.message)
            }
            is Engine.SideEffect.ReceiveMessage -> {
                // TODO write to subject?
            }
            is Engine.SideEffect.Open -> {
                protocolCoordinator.openAndRetry()
                topicCoordinator.subscribeAndRetry(sideEffect.topics)
                messageCoordinator.sendAndRetry(sideEffect.messages)
            }
            is Engine.SideEffect.Close -> {
                protocolCoordinator.close()
                topicCoordinator.unsubscribe(sideEffect.topics)
                messageCoordinator.clear(sideEffect.messages)
            }
            is Engine.SideEffect.Subscribe -> {
                topicCoordinator.subscribeAndRetry(sideEffect.topic)
            }
            is Engine.SideEffect.Unsubscribe -> {
                topicCoordinator.unsubscribe(sideEffect.topic)
            }
        }
    }

    fun start() {
        protocolCoordinator.start()
    }

    fun publish(topic: Topic, message: Message) {
        client.onShouldSendMessage(topic, message)
    }

    fun finishSending(topic: Topic, message: Message) {
        client.onShouldFinishSendingMessage(topic, message)
    }

    fun receive(topic: Topic, message: Message) {
        client.onShouldReceiveMessage(topic, message)
    }

    fun connect() {
        client.onShouldOpen()
    }

    fun disconnect() {
        client.onShouldClose()
    }

    fun subscribe(topic: Topic) {
        client.onShouldSubscribe(
            topic
        )
    }

    fun unsubscribe(topic: Topic) {
        client.onShouldUnsubscribe(topic)
    }

}
