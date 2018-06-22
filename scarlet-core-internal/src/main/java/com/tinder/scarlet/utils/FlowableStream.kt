/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.utils

import com.tinder.scarlet.Stream
import io.reactivex.Flowable
import io.reactivex.disposables.Disposable
import org.reactivestreams.Publisher

class FlowableStream<T>(
    private val flowable: Flowable<T>
) : Stream<T>, Publisher<T> by flowable {

    override fun start(observer: Stream.Observer<T>): Stream.Disposable {
        val disposable = flowable.subscribe(observer::onNext, observer::onError, observer::onComplete)
        return FlowableStreamDisposable(disposable)
    }

    class FlowableStreamDisposable(
        private val disposable: Disposable
    ) : Stream.Disposable {
        override fun dispose() = disposable.dispose()

        override fun isDisposed(): Boolean = disposable.isDisposed
    }
}
