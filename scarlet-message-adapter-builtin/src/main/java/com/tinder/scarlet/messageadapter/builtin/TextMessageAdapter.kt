/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.messageadapter.builtin

import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter

class TextMessageAdapter : MessageAdapter<String> {

    override fun fromMessage(message: Message): String = when (message) {
        is Message.Text -> message.value
        else -> throw IllegalArgumentException("This Message Adapter only supports text Messages")
    }

    override fun toMessage(data: String): Message = Message.Text(data)
}
