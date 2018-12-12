package com.tinder.scarlet.messageadapter.logansquare

import com.bluelinelabs.logansquare.LoganSquare
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.utils.getRawType
import java.lang.reflect.Type

class LoganSquareMessageAdapter<T> private constructor(
        private val klass: Class<T>) : MessageAdapter<T> {
    override fun fromMessage(message: Message): T {
        val stringValue = when (message) {
            is Message.Text -> message.value
            is Message.Bytes -> String(message.value)
        }

        return LoganSquare.parse(stringValue, klass)
    }

    override fun toMessage(data: T): Message {
        val stringValue = LoganSquare.serialize(data)
        return Message.Text(stringValue)
    }

    class Factory : MessageAdapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
            return LoganSquareMessageAdapter(type.getRawType())
        }

    }
}