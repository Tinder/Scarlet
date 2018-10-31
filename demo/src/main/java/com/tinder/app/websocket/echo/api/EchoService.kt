/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.websocket.echo.api

import android.graphics.Bitmap
import com.tinder.scarlet.StateTransition
import com.tinder.scarlet.websocket.WebSocketEvent
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import io.reactivex.Flowable

interface EchoService {
    @Receive
    fun observeStateTransition(): Flowable<StateTransition>

    @Receive
    fun observeWebSocketEvent(): Flowable<WebSocketEvent>

    @Receive
    fun observeText(): Flowable<String>

    @Receive
    fun observeBitmap(): Flowable<Bitmap>

    @Send
    fun sendText(message: String): Boolean

    @Send
    fun sendBitmap(message: Bitmap): Boolean
}
