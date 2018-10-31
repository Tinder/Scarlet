/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.socketio.chatroom.domain

import com.tinder.app.socketio.chatroom.api.AddUserTopic
import com.tinder.app.socketio.chatroom.api.ChatRoomService
import com.tinder.app.socketio.chatroom.api.NewMessageTopic
import com.tinder.app.socketio.chatroom.api.TypingStartedTopic
import com.tinder.app.socketio.chatroom.api.TypingStoppedTopic
import com.tinder.app.socketio.chatroom.api.UserJoinedTopic
import com.tinder.app.socketio.chatroom.api.UserLeftTopic
import com.tinder.app.socketio.chatroom.api.model.TypingStatusUpdate
import com.tinder.app.socketio.chatroom.domain.model.ChatMessage
import com.tinder.scarlet.Event
import com.tinder.scarlet.LifecycleState
import com.tinder.scarlet.State
import com.tinder.scarlet.socketio.SocketIoEvent
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.schedulers.Schedulers
import org.joda.time.DateTime
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class ChatMessageRepository(
    private val chatRoomService: ChatRoomService,
    private val addUserTopic: AddUserTopic,
    private val newMessageTopic: NewMessageTopic,
    private val typingStartedTopic: TypingStartedTopic,
    private val typingStoppedTopic: TypingStoppedTopic,
    private val userJoinedTopic: UserJoinedTopic,
    private val userLeftTopic: UserLeftTopic
) {
    private val messageCount = AtomicInteger()
    private val firstMessage =
        ChatMessage(
            generateMessageId(),
            FIRST_MESSAGE_DESCRIPTION,
            ChatMessage.Source.Received("Scarlet")
        )
    private val defaultMessages = listOf(firstMessage)
    private val messagesRef = AtomicReference<List<ChatMessage>>(defaultMessages)
    private val messagesProcessor = BehaviorProcessor.createDefault<List<ChatMessage>>(defaultMessages)

    init {
        chatRoomService.observeStateTransition()
            .observeOn(Schedulers.io())
            .subscribe({ stateTransition ->
                val event = stateTransition.event
                val description = when (event) {
                    is Event.OnLifecycleStateChange -> when (event.lifecycleState) {
                        LifecycleState.Started -> "\uD83C\uDF1D On Lifecycle Start"
                        LifecycleState.Stopped -> "\uD83C\uDF1A On Lifecycle Stop"
                        LifecycleState.Completed -> "\uD83D\uDCA5 On Lifecycle Terminate"
                    }
                    is Event.OnProtocolEvent -> {
                        when (stateTransition.toState) {
                            is State.WillConnect -> "\uD83D\uDCA4 WaitingToRetry"
                            is State.Connecting -> "⏳ Connecting"
                            is State.Connected -> "\uD83D\uDEEB Connected"
                            is State.Disconnecting -> "⏳ Disconnecting"
                            State.Disconnected -> "\uD83D\uDEEC Disconnected"
                            State.Destroyed -> "\uD83D\uDCA5 Destroyed"
                        }
                    }
                    Event.OnShouldConnect -> "⏰ Should Connect"
                }
                val chatMessage =
                    ChatMessage(
                        generateMessageId(),
                        description,
                        ChatMessage.Source.Received("Scarlet")
                    )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        chatRoomService.observeSocketIoEvent()
            .observeOn(Schedulers.io())
            .subscribe({ event ->
                val description = when (event) {
                    is SocketIoEvent.OnConnectionOpened -> "\uD83D\uDEF0️ On Connection Opened"
                    is SocketIoEvent.OnMessageReceived -> "\uD83D\uDEF0️ On Message Received"
                    is SocketIoEvent.OnConnectionClosed -> "\uD83D\uDEF0️ On Connection Closed"
                    is SocketIoEvent.OnConnectionFailed -> "\uD83D\uDEF0️ On Connection Failed"
                    else -> ""
                }
                val chatMessage =
                    ChatMessage(
                        generateMessageId(),
                        description,
                        ChatMessage.Source.Received("Scarlet")
                    )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        addUserTopic.observeSocketIoEvent()
            .filter { it is SocketIoEvent.OnConnectionOpened }
            .observeOn(Schedulers.io())
            .subscribe({
                addUserTopic.sendAddUser(USER_NAME)
            }, { e ->
                Timber.e(e)
            })

        newMessageTopic.observeNewMessage()
            .observeOn(Schedulers.io())
            .subscribe({ newMessage ->
                val chatMessage = ChatMessage(
                    generateMessageId(),
                    newMessage.message,
                    ChatMessage.Source.Received(newMessage.username),
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        typingStartedTopic.observeTypingStarted()
            .observeOn(Schedulers.io())
            .subscribe({ newMessage ->
                val chatMessage = ChatMessage(
                    generateMessageId(),
                    "Started typing",
                    ChatMessage.Source.Received(newMessage.username),
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })

        typingStoppedTopic.observeTypingStopped()
            .observeOn(Schedulers.io())
            .subscribe({ newMessage ->
                val chatMessage = ChatMessage(
                    generateMessageId(),
                    "Stopped typing",
                    ChatMessage.Source.Received(newMessage.username),
                    DateTime.now().plusMillis(50)
                )
                addChatMessage(chatMessage)
            }, { e ->
                Timber.e(e)
            })
    }

    fun observeChatMessage(): Flowable<List<ChatMessage>> = messagesProcessor

    fun sendNewMessage(text: String) {
        Completable.fromAction {
            val chatMessage = ChatMessage(generateMessageId(), text, ChatMessage.Source.Sent)
            addChatMessage(chatMessage)

            newMessageTopic.sendNewMessage(text)
        }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    fun clearAllMessages() {
        messagesRef.set(defaultMessages)
        messagesProcessor.onNext(defaultMessages)
    }

    fun sendTypingStarted() {
        Completable.fromAction {
            typingStartedTopic.sendTypingStarted(TypingStatusUpdate(USER_NAME))
        }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    fun sendTypingStopped() {
        Completable.fromAction {
            typingStoppedTopic.sendTypingStopped(TypingStatusUpdate(USER_NAME))
        }
            .subscribeOn(Schedulers.computation())
            .subscribe()
    }

    private fun addChatMessage(chatMessage: ChatMessage) {
        val existingMessages = messagesRef.get() ?: listOf()
        val messages = existingMessages + chatMessage
        messagesRef.set(messages)
        messagesProcessor.onNext(messages)
    }

    private fun generateMessageId(): Int = messageCount.getAndIncrement()
}

private const val USER_NAME = "scarlet"
private val FIRST_MESSAGE_DESCRIPTION = "Interact with this demo at https://socket-io-chat.now.sh/"