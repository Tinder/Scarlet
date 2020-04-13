package com.tinder.scarlet.streamadapter.rxjava3

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import io.reactivex.rxjava3.core.Flowable

class FlowableStreamAdapter<T> : StreamAdapter<T, Flowable<T>> {

    override fun adapt(stream: Stream<T>): Flowable<T> = Flowable.fromPublisher(stream)
}
