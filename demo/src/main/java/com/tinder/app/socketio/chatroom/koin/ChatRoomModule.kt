package com.tinder.app.socketio.chatroom.koin

import com.tinder.app.socketio.chatroom.api.AddUserTopic
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
import com.tinder.scarlet.socketio.SocketIoClient
import com.tinder.scarlet.streamadapter.rxjava2.RxJava2StreamAdapterFactory
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val chatRoomModule = module {

    // TODO sub module private

    single {
        SocketIoClient({ CHAT_SERVER_URL }) as Protocol
    }

    factory("default") {
        Scarlet.Configuration(
            lifecycle = get("foreground"),
            messageAdapterFactories = listOf(MoshiMessageAdapter.Factory()),
            streamAdapterFactories = listOf(RxJava2StreamAdapterFactory())
        )
    }

    single {
        Scarlet.Factory().create(get(), get("default"))
            .create<ChatRoomService>()
    }

    // TODO add protocol to factory(protocol)
    single {
        Scarlet.Factory()
            .create(
                get(),
                get<Scarlet.Configuration>("default")
                    .copy(topic = Topic.Simple("add user"))
            )
            .create<AddUserTopic>()
    }

    single {
        Scarlet.Factory()
            .create(
                get(),
                get<Scarlet.Configuration>("default")
                    .copy(topic = Topic.Simple("new message"))

            )
            .create<NewMessageTopic>()
    }

    single {
        Scarlet.Factory()
            .create(
                get(),
                get<Scarlet.Configuration>("default")
                    .copy(topic = Topic.Simple("typing"))

            )
            .create<TypingStartedTopic>()
    }

    single {
        Scarlet.Factory()
            .create(
                get(),
                get<Scarlet.Configuration>("default")
                    .copy(topic = Topic.Simple("stop typing"))

            )
            .create<TypingStoppedTopic>()
    }

    single {
        Scarlet.Factory()
            .create(
                get(),
                get<Scarlet.Configuration>("user joined")
                    .copy(topic = Topic.Simple("stop typing"))

            )
            .create<UserJoinedTopic>()
    }

    single {
        Scarlet.Factory()
            .create(
                get(),
                get<Scarlet.Configuration>("user left")
                    .copy(topic = Topic.Simple("stop typing"))

            )
            .create<UserLeftTopic>()
    }

    single { ChatMessageRepository(get(), get(), get(), get(), get(), get(), get()) }

    viewModel { ChatRoomViewModel(get()) }

}

private const val CHAT_SERVER_URL = "https://socket-io-chat.now.sh/"
