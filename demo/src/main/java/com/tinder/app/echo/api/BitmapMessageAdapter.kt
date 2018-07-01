/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.tinder.scarlet.Message
import com.tinder.scarlet.MessageAdapter
import java.io.ByteArrayOutputStream
import java.lang.reflect.Type

class BitmapMessageAdapter : MessageAdapter<Bitmap> {
    override fun fromMessage(message: Message): Bitmap {
        val (bytes) = message as Message.Bytes
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    override fun toMessage(data: Bitmap): Message {
        val outputStream = ByteArrayOutputStream()
        data.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
        return Message.Bytes(outputStream.toByteArray())
    }

    class Factory : MessageAdapter.Factory {
        override fun create(type: Type, annotations: Array<Annotation>): MessageAdapter<*> {
            if (type == Bitmap::class.java) {
                return BitmapMessageAdapter()
            }
            throw IllegalArgumentException()
        }
    }
}
