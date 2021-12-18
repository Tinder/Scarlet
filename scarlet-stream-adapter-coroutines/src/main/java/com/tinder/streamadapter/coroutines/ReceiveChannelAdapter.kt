package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking

class ReceiveChannelAdapter<T> : StreamAdapter<T, ReceiveChannel<T>> {

    override fun adapt(stream: Stream<T>): ReceiveChannel<T> {
        val channelForwarder = ChannelForwarder<T>()
        stream.start(channelForwarder)
        return channelForwarder.channel
    }

    private class ChannelForwarder<T> : Stream.Observer<T> {
        private val _channel = Channel<T>()
        val channel: ReceiveChannel<T> = _channel

        override fun onComplete() {
            _channel.close()
        }

        override fun onError(throwable: Throwable) {
            _channel.close(throwable)
        }

        override fun onNext(data: T) {
            println("$data is ")
            _channel.sendBlocking(data)
        }
    }
}
