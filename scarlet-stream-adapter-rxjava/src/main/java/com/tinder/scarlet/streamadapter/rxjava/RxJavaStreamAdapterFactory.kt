/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.rxjava

import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import rx.Observable
import java.lang.reflect.Type

/**
 * A [stream adapter factory][StreamAdapter.Factory] that uses RxJava.
 */
class RxJavaStreamAdapterFactory : StreamAdapter.Factory {

    override fun create(type: Type): StreamAdapter<Any, Any> = when (type.getRawType()) {
        Observable::class.java -> ObservableStreamAdapter()
        else -> throw IllegalArgumentException()
    }
}
