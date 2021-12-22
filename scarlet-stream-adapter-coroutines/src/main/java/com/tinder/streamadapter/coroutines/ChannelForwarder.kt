package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.Stream
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.trySendBlocking

internal class ChannelForwarder<T>(bufferSize: Int) : Stream.Observer<T> {
    private val channel = Channel<T>(bufferSize, BufferOverflow.DROP_OLDEST)
    private var disposable: Stream.Disposable? = null

    fun start(stream: Stream<T>): ReceiveChannel<T> {
        disposable = stream.start(this)
        return channel
    }

    override fun onComplete() {
        channel.close()
        disposable?.dispose()
    }

    override fun onError(throwable: Throwable) {
        channel.close(throwable)
        disposable?.dispose()
    }

    override fun onNext(data: T) {
        channel.trySendBlocking(data)
            .exceptionOrNull() ?.let { throw it }
    }
}
