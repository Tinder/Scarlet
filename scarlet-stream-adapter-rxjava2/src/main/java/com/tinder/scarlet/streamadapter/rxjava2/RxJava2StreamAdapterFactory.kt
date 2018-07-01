/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.rxjava2

import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import io.reactivex.Flowable
import io.reactivex.Observable
import java.lang.reflect.Type

/**
 * A [stream adapter factory][StreamAdapter.Factory] that uses RxJava2.
 */
class RxJava2StreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> = when (type.getRawType()) {
        Flowable::class.java -> FlowableStreamAdapter()
        Observable::class.java -> ObservableStreamAdapter()
        else -> throw IllegalArgumentException("$type is not supported by this StreamAdapterFactory")
    }
}
