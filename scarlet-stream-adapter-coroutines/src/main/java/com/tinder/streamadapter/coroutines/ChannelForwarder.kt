package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.Stream
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking

internal class ChannelForwarder<T> : Stream.Observer<T> {
    private val _channel = Channel<T>()
    val channel: ReceiveChannel<T> = _channel
    private var disposable: Stream.Disposable? = null

    fun start(stream: Stream<T>): ReceiveChannel<T> {
        disposable = stream.start(this)
        return channel
    }

    override fun onComplete() {
        _channel.close()
        disposable?.dispose()
    }

    override fun onError(throwable: Throwable) {
        _channel.close(throwable)
        disposable?.dispose()
    }

    override fun onNext(data: T) {
        println("$data is ")
        _channel.sendBlocking(data)
    }
}
