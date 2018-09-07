/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Protocol
import com.tinder.scarlet.Topic
import com.tinder.scarlet.state.utils.GroupWorker
import com.tinder.scarlet.state.utils.Worker

internal class TopicCoordinator(
    serviceLocator: ServiceLocator
) : ServiceLocator by serviceLocator {
    private val topicGroupWorker =
        GroupWorker<Topic, Protocol, Unit, Unit, Unit, Unit>()

    fun start(context: Protocol) {
        topicGroupWorker.onLifecycleStarted(context)
    }

    fun stop() {
        topicGroupWorker.onLifecycleStopped()
    }

    fun subscribeAndRetry(topic: Topic) {
        topicGroupWorker.add(
            topic,
            Unit, // generate meta info?
            Unit
        ) { transition ->
            val sideEffect = transition.sideEffect!!
            when (sideEffect) {
                is Worker.SideEffect.ScheduleRetry -> {
                    // TODO timer
                }
                is Worker.SideEffect.UnscheduleRetry -> {
                    // TODO timer
                }
                is Worker.SideEffect.StartWork -> {
                    sideEffect.context.subscribe(topic, null)
                }
                is Worker.SideEffect.StopWork -> {
                    sideEffect.context.unsubscribe(topic, null)
                }
                is Worker.SideEffect.ForceStopWork -> {
                    sideEffect.context.unsubscribe(topic, null)
                }
            }
        }
    }

    fun subscribeAndRetry(topics: Set<Topic>) {
        topics.forEach { topic ->
            subscribeAndRetry(topic)
        }
    }

    fun unsubscribe(topic: Topic) {
        topicGroupWorker.remove(topic)
    }

    fun unsubscribe(topics: Set<Topic>) {
        topics.forEach { topic -> unsubscribe(topic) }
    }

    fun onTopicSubscribed(topic: Topic) {
        topicGroupWorker.onWorkStarted(topic)
    }

    fun onTopicUnsubscribed(topic: Topic) {
        topicGroupWorker.onWorkStopped(topic)
    }
}
