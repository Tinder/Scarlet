package com.tinder.streamadapter.coroutines

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.reactive.openSubscription

class ReceiveChannelStreamAdapter<T> : StreamAdapter<T, ReceiveChannel<T>> {

    override fun adapt(stream: Stream<T>) = stream.openSubscription()
}