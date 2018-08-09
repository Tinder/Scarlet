/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.builtin

import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter

class ByteArrayMessageAdapter : MessageAdapter<ByteArray> {

    override fun fromMessage(message: Message): ByteArray = when (message) {
        is Message.Bytes -> message.value
        else -> throw IllegalArgumentException("This Message Adapter only supports bytes Messages")
    }

    override fun toMessage(data: ByteArray): Message = Message.Bytes(data)
}
