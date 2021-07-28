/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Messenger
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.tinder.R
import com.tinder.app.socketio.chatroom.api.AddUserTopic
import com.tinder.app.socketio.chatroom.api.ChatRoomService
import com.tinder.app.socketio.chatroom.api.NewMessageTopic
import com.tinder.app.socketio.chatroom.api.TypingStartedTopic
import com.tinder.app.socketio.chatroom.api.TypingStoppedTopic
import com.tinder.app.socketio.chatroom.api.UserJoinedTopic
import com.tinder.app.socketio.chatroom.api.UserLeftTopic
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.socketio.SocketIoEvent
import com.tinder.scarlet.socketio.client.SocketIoClient
import com.tinder.scarlet.socketio.client.SocketIoEventName
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

class ChatRoomSocketIoService : LifecycleService() {

    var incomingMessageCount = 0
    private val messenger = Messenger(IncomingHandler())

    @SuppressLint("CheckResult")
    override fun onCreate() {
        super.onCreate()

        val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        val config = Scarlet.Configuration(
            lifecycle = AndroidLifecycle.ofLifecycleServiceStarted(application, this),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory(moshi)),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
        val serverUrl = "https://socket-io-chat.now.sh/"

        val scarlet = Scarlet(
            SocketIoClient({ serverUrl }),
            config
        )

        val chatRoomService = scarlet.create<ChatRoomService>()
        val addUserTopic = scarlet
            .child(SocketIoEventName("add user"), config)
            .create<AddUserTopic>()
        val newMessageTopic = scarlet
            .child(SocketIoEventName("new message"), config)
            .create<NewMessageTopic>()
        val typingStartedTopic = scarlet
            .child(SocketIoEventName("typing"), config)
            .create<TypingStartedTopic>()
        val typingStoppedTopic = scarlet
            .child(SocketIoEventName("stop typing"), config)
            .create<TypingStoppedTopic>()
        val userJoinedTopic = scarlet
            .child(SocketIoEventName("user joined"), config)
            .create<UserJoinedTopic>()
        val userLeftTopic = scarlet
            .child(SocketIoEventName("user left"), config)
            .create<UserLeftTopic>()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel =
                NotificationChannel(channelId, "Default", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        Timber.d("chatroom scarlet created")

        chatRoomService.observeStateTransition()
            .subscribe {
                Timber.d("chatroom service: $it")

                val notification = createNotification {
                    setContentText("State Transition ${it.toState}")
                }
                notificationManager.notify(notificationIdCurrentState, notification)
            }

        val username = "scarlet service"

        addUserTopic.observeSocketIoEvent()
            .filter { it is SocketIoEvent.OnConnectionOpened }
            .observeOn(Schedulers.io())
            .subscribe(
                {
                    addUserTopic.sendAddUser(username)

                    Timber.d("chatroom added user: $it")
                    val notification = createNotification {
                        setContentText("Joined chatroom")
                    }
                    notificationManager.notify(notificationIdCurrentState, notification)
                },
                { e ->
                    Timber.e(e)
                })

        Flowable.fromPublisher(AndroidLifecycle.ofLifecycleServiceStarted(application, this))
            .subscribe {
                Timber.d("chatroom lifecycle: $it")

                val notification = createNotification {
                    setContentText("Lifecycle State: $it")
                }
                notificationManager.notify(notificationIdCurrentState, notification)
            }

        Flowable.merge(
            newMessageTopic.observeNewMessage()
                .map { "${it.username}: ${it.message}" },
            typingStartedTopic.observeTypingStarted()
                .map { "${it.username} started typing" },
            typingStoppedTopic.observeTypingStopped()
                .map { "${it.username} stopped typing" }
        ).subscribe {
            Timber.d("chatroom new message: $it")

            val notification = createNotification {
                setContentText(it)
            }

            val notificationId = notificationIdIncomingMessage + incomingMessageCount
            incomingMessageCount += 1
            notificationManager.notify(notificationId, notification)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        Timber.d("chatroom onbind")
        return messenger.binder
    }

    private fun createNotification(builder: NotificationCompat.Builder.() -> Unit): Notification {
        return NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_action_info)
            .setWhen(System.currentTimeMillis())
            .setContentTitle("Chat Room")
            .setAutoCancel(true)
            .setOngoing(true)
            .apply(builder)
            .build()
    }

    private inner class IncomingHandler : Handler()

    companion object {
        const val notificationIdCurrentState = 10000
        const val notificationIdIncomingMessage = 10001
        const val channelId = "default"
    }
}
