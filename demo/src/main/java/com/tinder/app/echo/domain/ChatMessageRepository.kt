/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.echo.domain

import android.graphics.Bitmap
import com.tinder.app.echo.api.EchoService
import com.tinder.app.echo.inject.EchoBotScope
import com.tinder.scarlet.Event
import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.State
import com.tinder.scarlet.WebSocket
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import org.joda.time.DateTime
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@EchoBotScope
class ChatMessageRepository @Inject constructor(
    private val echoService: EchoService
) {
    private val messageCount = AtomicInteger()
    private val messagesRef = AtomicReference<List<ChatMessage>>()
    private val messagesProcessor = BehaviorProcessor.create<List<ChatMessage>>()

    init {
        echoService.observeEvent()
            .observeOn(Schedulers.io())
            .subscribe({ event ->
                val description = when (event) {
                    is Event.OnLifecycle.StateChange<*> -> when (event.state) {
                        Lifecycle.State.Started -> "\uD83C\uDF1D On Lifecycle Start"
                        is Lifecycle.State.Stopped -> "\uD83C\uDF1A On Lifecycle Stop"
                        Lifecycle.State.Destroyed -> "\uD83D\uDCA5 On Lifecycle Terminate"
                    }
                    Event.OnLifecycle.Terminate -> "\uD83D\uDCA5 On Lifecycle Terminate"
                    is Event.OnWebSocket.Event<*> -> when (event.event) {
                        is WebSocket.Event.OnConnectionOpened<*> -> "\uD83D\uDEF0️ On WebSocket Connection Opened"
                        is WebSocket.Event.OnMessageReceived -> "\uD83D\uDEF0️ On WebSocket Message Received"
                        is WebSocket.Event.OnConnectionClosing -> "\uD83D\uDEF0️ On WebSocket Connection Closing"
                        is WebSocket.Event.OnConnectionClosed -> "\uD83D\uDEF0️ On WebSocket Connection Closed"
                        is WebSocket.Event.OnConnectionFailed -> "\uD83D\uDEF0️ On WebSocket Connection Failed"
                    }
                    Event.OnWebSocket.Terminate -> "\uD83D\uDEF0️ On WebSocket Terminate"
                    is Event.OnStateChange<*> -> when (event.state) {
                        is State.WaitingToRetry -> "\uD83D\uDCA4 WaitingToRetry"
                        is State.Connecting -> "⏳ Connecting"
                        is State.Connected -> "\uD83D\uDEEB Connected"
                        State.Disconnecting -> "⏳ Disconnecting"
                        State.Disconnected -> "\uD83D\uDEEC Disconnected"
                        State.Destroyed -> "\uD83D\uDCA5 Destroyed"
                    }
                    Event.OnRetry -> "⏰ On Retry"
                }
                val chatMessage = ChatMessage.Text(generateMessageId(), description, ChatMessage.Source.RECEIVED)
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        echoService.observeText()
            .observeOn(Schedulers.io())
            .subscribe({ text ->
                val chatMessage = ChatMessage.Text(
                    generateMessageId(),
                    text,
                    ChatMessage.Source.RECEIVED,
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        echoService.observeBitmap()
            .observeOn(Schedulers.io())
            .subscribe({ bitmap ->
                val chatMessage = ChatMessage.Image(
                    generateMessageId(),
                    bitmap,
                    ChatMessage.Source.RECEIVED,
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })
    }

    fun observeChatMessage(): Flowable<List<ChatMessage>> = messagesProcessor

    fun addTextMessage(text: String) {
        val chatMessage = ChatMessage.Text(generateMessageId(), text, ChatMessage.Source.SENT)
        addChatMessage(chatMessage)

        echoService.sendText(text)
    }

    fun addImageMessage(bitmap: Bitmap) {
        val chatMessage = ChatMessage.Image(generateMessageId(), bitmap, ChatMessage.Source.SENT)
        addChatMessage(chatMessage)

        echoService.sendBitmap(bitmap)
    }

    fun clearAllMessages() {
        messagesRef.set(listOf())
        messagesProcessor.onNext(listOf())
    }

    private fun addChatMessage(chatMessage: ChatMessage) {
        val existingMessages = messagesRef.get() ?: listOf()
        val messages = existingMessages + chatMessage
        messagesRef.set(messages)
        messagesProcessor.onNext(messages)
    }

    private fun generateMessageId(): Int = messageCount.getAndIncrement()
}
