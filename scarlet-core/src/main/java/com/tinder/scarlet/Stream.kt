/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import org.reactivestreams.Publisher

/**
 * An sequence of asynchronous values of type `T` that is used to emit [WebSocket.Event] and [Message].
 *
 * A Stream is lazy. It may be started by [start].
 */
interface Stream<T> : Publisher<T> {

    fun start(observer: Observer<T>): Disposable

    /**
     * Observes a [Stream].
     */
    interface Observer<in T> {
        /**
         * Data notification.
         */
        fun onNext(data: T)

        /**
         * Failed terminal state.
         */
        fun onError(throwable: Throwable)

        /**
         * Successful terminal state.
         */
        fun onComplete()
    }

    interface Disposable {
        fun dispose()

        fun isDisposed(): Boolean
    }
}
