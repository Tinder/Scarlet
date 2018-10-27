package com.tinder.app.socketio.chatroom.koin

import com.tinder.app.socketio.chatroom.api.ChatRoomService
import com.tinder.app.socketio.chatroom.api.NewMessageTopic
import com.tinder.app.socketio.chatroom.api.TypingStartedTopic
import com.tinder.app.socketio.chatroom.api.TypingStoppedTopic
import com.tinder.app.socketio.chatroom.api.UserJoinedTopic
import com.tinder.app.socketio.chatroom.api.UserLeftTopic
import com.tinder.app.socketio.chatroom.domain.ChatMessageRepository
import com.tinder.app.socketio.chatroom.view.ChatRoomViewModel
import com.tinder.scarlet.Protocol
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.Topic
import com.tinder.scarlet.messageadapter.moshi.MoshiMessageAdapter
import com.tinder.scarlet.socketio.SocketIo
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val chatRoomModule = module {

    // TODO sub module private

    single {
        SocketIo({ CHAT_SERVER_URL }) as Protocol
    }

    single {
        val configuration = Scarlet.Configuration(
            protocol = get(),
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )

        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<ChatRoomService>()
    }

    // TODO add protocol to factory(protocol)
    single {
        val configuration = Scarlet.Configuration(
            protocol = get(),
            topic = Topic.Simple("new message"),
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )

        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<NewMessageTopic>()
    }

    single {
        val configuration = Scarlet.Configuration(
            protocol = get(),
            topic = Topic.Simple("typing"),
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )

        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<TypingStartedTopic>()
    }

    single {
        val configuration = Scarlet.Configuration(
            protocol = get(),
            topic = Topic.Simple("stop typing"),
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )

        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<TypingStoppedTopic>()
    }

    single {
        val configuration = Scarlet.Configuration(
            protocol = get(),
            topic = Topic.Simple("user joined"),
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )

        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<UserJoinedTopic>()
    }

    single {
        val configuration = Scarlet.Configuration(
            protocol = get(),
            topic = Topic.Simple("user left"),
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )

        val scarlet = Scarlet.Factory().create(configuration)
        scarlet.create<UserLeftTopic>()
    }

    single { ChatMessageRepository(get(), get(), get(), get(), get(), get()) }

    viewModel { ChatRoomViewModel(get()) }

}

private const val CHAT_SERVER_URL = "https://socket-io-chat.now.sh/"
