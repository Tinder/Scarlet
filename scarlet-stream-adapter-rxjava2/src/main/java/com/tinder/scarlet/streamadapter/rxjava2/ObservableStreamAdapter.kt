/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.rxjava2

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import io.reactivex.Observable

class ObservableStreamAdapter<T> : StreamAdapter<T, Observable<T>> {

    override fun adapt(stream: Stream<T>): Observable<T> = Observable.fromPublisher(stream)
}
