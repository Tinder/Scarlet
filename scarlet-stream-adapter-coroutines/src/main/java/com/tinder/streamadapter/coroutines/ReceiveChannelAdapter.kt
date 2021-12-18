package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import kotlinx.coroutines.channels.ReceiveChannel

class ReceiveChannelAdapter<T> : StreamAdapter<T, ReceiveChannel<T>> {

    override fun adapt(stream: Stream<T>): ReceiveChannel<T> {
        val channelForwarder = ChannelForwarder<T>()
        return channelForwarder.start(stream)
    }
}
