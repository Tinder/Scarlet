/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Message
import java.lang.reflect.Type


interface Scarlet : ServiceFactory.Factory {

    data class Configuration(
        val protocol: Protocol,
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

sealed class ConnectionEvent {
    data class OnOpening(
        val connection: Connection, val request: Connection.OpenRequest
    ) : ConnectionEvent()

    data class OnOpened(
        val connection: Connection,
        val request: Connection.OpenRequest,
        val response: Connection.OpenResponse
    ) : ConnectionEvent()

    data class OnClosing(
        val connection: Connection, val request: Connection.CloseRequest
    ) : ConnectionEvent()

    data class OnClosed(
        val connection: Connection,
        val request: Connection.CloseRequest,
        val response: Connection.CloseResponse
    ) : ConnectionEvent()

    data class OnCanceled(val connection: Connection) : ConnectionEvent()
}

sealed class ChannelEvent {
    data class OnOpening(
        val channel: Channel, val request: Channel.OpenRequest
    ) : ChannelEvent()

    data class OnOpened(
        val channel: Channel, val request: Channel.OpenRequest, val response: Channel.OpenResponse
    ) : ChannelEvent()

    data class OnMessageReceived(val channel: Channel, val message: Message) : ChannelEvent()
    data class OnMessageDelivered(val channel: Channel, val message: Message) : ChannelEvent()
    data class OnClosing(val channel: Channel, val request: Channel.CloseRequest) : ChannelEvent()
    data class OnClosed(
        val channel: Channel, val request: Channel.CloseRequest, val response: Channel.CloseResponse
    ) : ChannelEvent()

    data class OnCanceled(val channel: Channel) : ChannelEvent()
}


// plugin
interface Protocol {
    fun createConnectionFactory(): Connection.Factory

    fun createConnectionRequestFactory(): Connection.RequestFactory

    fun createChannelFactory(): Channel.Factory

    fun createChannelRequestFactory(): Channel.RequestFactory

    fun createEventAdapterFactory(): EventAdapter.Factory
}

// TODO Remove this??
// TODO Unify with channel?
interface Connection {

    val defaultTopic: Topic

    fun open(openRequest: OpenRequest)

    fun close(closeRequest: CloseRequest)

    fun forceClose()

    interface Listener {
        fun onOpening(connection: Connection)
        fun onOpened(connection: Connection, response: OpenResponse)
        fun onClosing(connection: Connection)
        fun onClosed(connection: Connection, response: CloseResponse)
        fun onCanceled(connection: Connection, throwable: Throwable)
    }

    interface OpenRequest

    interface OpenResponse

    interface CloseRequest

    interface CloseResponse

    interface RequestFactory {
        fun createOpenRequest(connection: Connection): OpenRequest

        fun createCloseRequest(connection: Connection): CloseRequest
    }

    interface Factory {
        fun create(listener: Listener): Connection
    }
}

interface Channel {
    val topic: Topic

    fun open(openRequest: OpenRequest)

    fun close(closeRequest: CloseRequest)

    fun forceClose()

    fun send(message: Message)

    interface Listener {
        fun onOpening(channel: Channel)
        fun onOpened(channel: Channel, response: OpenResponse)
        fun onMessageReceived(channel: Channel, message: Message)
        fun onClosing(channel: Channel)
        fun onClosed(channel: Channel, response: CloseResponse)
        fun onCanceled(channel: Channel, throwable: Throwable?)
    }

    interface OpenRequest

    interface OpenResponse

    interface MessageWrapper

    interface CloseRequest

    interface CloseResponse

    interface RequestFactory {
        fun createOpenRequest(channel: Channel): OpenRequest

        fun createCloseRequest(channel: Channel): CloseRequest
    }

    data class Configuration(
        val connection: Connection,
        val topic: Topic,
        val listener: Listener
    )

    interface Factory {
        fun create(configuration: Configuration): Channel
    }
}

interface EventAdapter<T> {
    fun fromConnectionEvent(event: ConnectionEvent): T

    fun fromChannelEvent(event: ChannelEvent): T

    interface Factory {
        fun create(type: Type, annotations: Array<Annotation>): EventAdapter<*>
    }
}

interface Topic {

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
