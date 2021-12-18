package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.Stream
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking

internal class ChannelForwarder<T> : Stream.Observer<T> {
    private val _channel = Channel<T>()
    val channel: ReceiveChannel<T> = _channel

    fun start(stream: Stream<T>): ReceiveChannel<T> {
        stream.start(this)
        return channel
    }

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
