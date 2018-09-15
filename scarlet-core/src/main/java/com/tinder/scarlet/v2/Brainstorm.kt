/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Stream
import java.lang.reflect.Type


interface Scarlet : ServiceFactory.Factory {

    data class Configuration(
        val protocolFactory: Protocol.Factory,
        val lifecycle: Lifecycle,
        val backoffStrategy: BackoffStrategy
    )

    interface Factory {
        fun create(configuration: Configuration): Scarlet
    }
}

interface ServiceFactory {

    fun <T> create(): T

    data class Configuration(
        val topic: Topic,
        val lifecycle: Lifecycle,
        val backoffStrategy: BackoffStrategy,
        val streamAdapters: List<Any>,
        val messageAdapters: List<Any>
    )

    interface Factory {
        fun create(configuration: ServiceFactory.Configuration): ServiceFactory
    }
}

// plugin
interface Protocol : Channel.Factory, EventAdapter.Factory {

    fun open(): Stream<Event>

    fun close()

    fun forceClose()

    sealed class Event {
        data class OnOpening(val protocol: Protocol, val request: OpenRequest) : Event()
        data class OnOpened(val protocol: Protocol, val request: OpenRequest, val response: OpenResponse) : Event()
        data class OnClosing(val protocol: Protocol, val request: CloseRequest) : Event()
        data class OnClosed(val protocol: Protocol, val request: CloseRequest, val response: CloseResponse) : Event()
        data class OnCanceled(val protocol: Protocol) : Event()
    }

    interface OpenRequest

    interface OpenResponse

    interface CloseRequest

    interface CloseResponse

    data class Configuration(
        val protocolRequestFactory: RequestFactory<OpenRequest, CloseRequest>,
        val channelRequestFactory: RequestFactory<Channel.OpenRequest, Channel.CloseRequest>
    )

    interface Factory {
        fun create(configuration: Configuration): Protocol
    }

}

interface Channel {
    val topic: Topic

    fun open(): Stream<Event>

    fun close()

    fun forceClose()

    fun send(message: Message)

    sealed class Event {
        data class OnOpening(val channel: Channel, val request: OpenRequest) : Event()
        data class OnOpened(val channel: Channel, val request: OpenRequest, val response: OpenResponse) : Event()
        data class OnMessageReceived(val channel: Channel, val message: Message) : Event()
        data class OnClosing(val channel: Channel, val request: CloseRequest) : Event()
        data class OnClosed(val channel: Channel, val request: CloseRequest, val response: CloseResponse) : Event()
        data class OnCanceled(val channel: Channel) : Event()
    }

    interface OpenRequest

    interface OpenResponse

    interface CloseRequest

    interface CloseResponse

    data class Configuration(
        val protocol: Protocol,
        val topic: Topic
    )

    interface Factory {
        fun create(configuration: Configuration): Channel
    }
}

interface EventAdapter<T> {
    fun fromProtocolEvent(event: Protocol.Event): T

    fun fromChannelEvent(event: Channel.Event): T

    interface Factory {
        fun create(type: Type, annotations: Array<Annotation>): EventAdapter<*>
    }
}

// plugin of plugin
interface RequestFactory<OPEN_REQUEST, CLOSE_REQUEST> {
    fun createOpenRequest(from: Any): OPEN_REQUEST

    fun createCloseRequest(from: Any): CLOSE_REQUEST
}

interface Topic {

}

interface Message {

}

// plugin
interface Lifecycle {

    // Event

    // onStart()

    // onStop()

}

// plugin
interface BackoffStrategy {
}
