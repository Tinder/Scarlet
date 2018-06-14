/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.app.echo.api

import android.graphics.Bitmap
import com.tinder.scarlet.Event
import com.tinder.scarlet.State
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface EchoService {
    @Receive
    fun observeState(): Flowable<State>

    @Receive
    fun observeEvent(): Flowable<Event>

    @Receive
    fun observeText(): Flowable<String>

    @Receive
    fun observeBitmap(): Flowable<Bitmap>

    @Send
    fun sendText(message: String): Boolean

    @Send
    fun sendBitmap(message: Bitmap): Boolean
}
