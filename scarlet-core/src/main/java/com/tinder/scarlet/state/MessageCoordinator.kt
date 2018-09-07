/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Message
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.Topic
import com.tinder.scarlet.state.utils.GroupWorker
import com.tinder.scarlet.state.utils.Worker

internal class MessageCoordinator(
    serviceLocator: ServiceLocator
) : ServiceLocator by serviceLocator {
    private val messageGroupWorker =
        GroupWorker<Pair<Topic, Message>, Protocol, Unit, Unit, Unit, Unit>()

    fun start(protocol: Protocol) {
        messageGroupWorker.onLifecycleStarted(protocol)
    }

    fun stop() {
        messageGroupWorker.onLifecycleStopped()
    }

    fun sendAndRetry(topic: Topic, message: Message) {
        messageGroupWorker.add(topic to message, Unit, Unit) {
            val sideEffect = it.sideEffect!!
            when (sideEffect) {
                is Worker.SideEffect.ScheduleRetry -> {
                    // TODO timer
                }
                is Worker.SideEffect.UnscheduleRetry -> {
                    // TODO timer
                }
                is Worker.SideEffect.StartWork -> {
                    sideEffect.context.send(topic, message, null)
                }
                is Worker.SideEffect.StopWork -> {
                    engineCoordinator.finishSending(topic, message)
                }
            }
        }
    }

    fun sendAndRetry(messages: Map<Topic, List<Message>>) {
        messages.forEach { (topic, messagesInTopic) ->
            messagesInTopic.forEach { message ->
                sendAndRetry(topic, message)
            }
        }
    }

    fun clear(topic: Topic, message: Message) {
        messageGroupWorker.remove(topic to message)
    }

    fun clear(messages: Map<Topic, List<Message>>) {
        messages.forEach { (topic, messagesInTopic) ->
            messagesInTopic.forEach {
                clear(topic, it)
            }
        }
    }

    fun onMessageSent(topic: Topic, message: Message) {
        messageGroupWorker.onWorkStopped(topic to message)
    }

    fun onMessageFailedToSend(topic: Topic, message: Message) {
        messageGroupWorker.onWorkFailed(topic to message, RuntimeException())
    }
}
