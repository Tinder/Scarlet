/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.sse

import com.tinder.scarlet.Message
import com.tinder.scarlet.utils.getRawType
import com.tinder.scarlet.ProtocolEvent
import com.tinder.scarlet.ProtocolSpecificEventAdapter
import com.tinder.scarlet.ProtocolSpecificEvent
import okhttp3.Response
import okhttp3.sse.EventSource
import java.lang.reflect.Type

sealed class EventSourceEvent : ProtocolSpecificEvent {
    data class OnConnectionOpened(val eventSource: EventSource, val okHttpResponse: Response) : EventSourceEvent()

    data class OnMessageReceived(val message: Message, val id: String?, val type: String?) : EventSourceEvent()

    object OnConnectionClosed : EventSourceEvent()

    data class OnConnectionFailed(val throwable: Throwable) : EventSourceEvent()

    class Adapter : ProtocolSpecificEventAdapter {
        override fun fromEvent(event: ProtocolEvent): EventSourceEvent {
            return when (event) {
                is ProtocolEvent.OnOpened -> {
                    val response = event.response as OkHttpEventSource.OpenResponse
                    EventSourceEvent.OnConnectionOpened(response.eventSource, response.okHttpResponse)
                }
                is ProtocolEvent.OnMessageReceived -> {
                    val messageMetaData = event.messageMetaData as OkHttpEventSource.ReceivedMessageMetaData
                    EventSourceEvent.OnMessageReceived(event.message, messageMetaData.id, messageMetaData.type)
                }
                is ProtocolEvent.OnClosed -> {
                    EventSourceEvent.OnConnectionClosed
                }
                is ProtocolEvent.OnFailed -> {
                    EventSourceEvent.OnConnectionFailed(event.throwable ?: Throwable())
                }
                else -> throw IllegalArgumentException()
            }
        }

        class Factory : ProtocolSpecificEventAdapter.Factory {
            override fun create(type: Type, annotations: Array<Annotation>): ProtocolSpecificEventAdapter {
                val receivingClazz = type.getRawType()
                require(EventSourceEvent::class.java.isAssignableFrom(receivingClazz)) {
                    "Only subclasses of EventSourceEvent are supported"
                }
                return Adapter()
            }
        }
    }
}
