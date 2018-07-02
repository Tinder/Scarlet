/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.rxjava2

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import io.reactivex.Flowable

class FlowableStreamAdapter<T> : StreamAdapter<T, Flowable<T>> {

    override fun adapt(stream: Stream<T>): Flowable<T> = Flowable.fromPublisher(stream)
}
