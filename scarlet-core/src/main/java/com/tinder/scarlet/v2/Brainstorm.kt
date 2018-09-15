/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.Stream


interface Scarlet: ServiceFactory.Factory {

    data class Configuration(
        val protocolFactory: Protocol.Factory,
        val lifecycle: Lifecycle
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
        val streamAdapters: List<Any>,
        val messageAdatpers: List<Any>
    )

    interface Factory {
        fun create(configuration: ServiceFactory.Configuration): ServiceFactory
    }
}

interface Protocol {

    // TODO: options

    val channelFactory: Channel.Factory

    // event adapter

    fun open(request: Any): Stream<Event>

    fun close(request: Any)

    sealed class Event {
        data class OnOpening(val protocol: Protocol, val request: Any)
        data class OnOpened(val protocol: Protocol, val request: Any, val response: Any)
        data class OnClosing(val protocol: Protocol, val request: Any)
        data class OnClosed(val protocol: Protocol, val request: Any, val response: Any)
    }

    data class Configuration(
        val requestFactory: RequestFactory
    )

    interface Factory {
        fun create(configuration: Configuration): Protocol
    }

    interface Channel {
        val topic: Topic

        fun open(request: Any): Stream<Event>

        fun close(request: Any)

        fun send(message: Message)

        sealed class Event {
            data class OnOpening(val channel: Channel, val request: Any)
            data class OnOpened(val channel: Channel, val request: Any, val response: Any)
            data class OnMessageReceived(val channel: Channel, val message: Message)
            data class OnClosing(val channel: Channel, val request: Any)
            data class OnClosed(val channel: Channel, val request: Any, val response: Any)
        }

        data class Configuration(
            val protocol: Protocol,
            val topic: Topic
        )
        interface Factory {
            fun create(configuration: Configuration): Channel
        }
    }

}

interface RequestFactory {

}


interface Topic {

}

interface Message {

}


interface Lifecycle {

    // Event

    // onStart()

    // onStop()

}
