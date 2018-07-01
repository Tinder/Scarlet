/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.rxjava

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import rx.Observable
import rx.Subscriber

class ObservableStreamAdapter<T> : StreamAdapter<T, Observable<T>> {

    override fun adapt(stream: Stream<T>): Observable<T> = Observable.unsafeCreate(StreamOnSubscribe(stream))

    private class StreamOnSubscribe<T>(private val stream: Stream<T>) : Observable.OnSubscribe<T> {

        override fun call(subscriber: Subscriber<in T>) {
            stream.start(StreamObserver(subscriber))
        }

        private class StreamObserver<in R> internal constructor(
            private val subscriber: Subscriber<in R>
        ) : Stream.Observer<R> {
            override fun onNext(data: R) {
                subscriber.onNext(data)
            }

            override fun onError(throwable: Throwable) {
                subscriber.onError(throwable)
            }

            override fun onComplete() {
                subscriber.onCompleted()
            }
        }
    }
}
