/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.utils

import com.tinder.scarlet.Stream

class EmptyStreamObserver<in T> : Stream.Observer<T> {
    override fun onNext(data: T) {
    }

    override fun onComplete() {
    }

    override fun onError(throwable: Throwable) {
    }
}
