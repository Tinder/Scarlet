/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.internal.servicemethod

import com.tinder.scarlet.StreamAdapter
import io.reactivex.exceptions.CompositeException
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StreamAdapterResolver @Inject constructor(
    private val streamAdapterFactories: List<StreamAdapter.Factory>
) {

    fun resolve(type: Type): StreamAdapter<Any, Any> {
        val throwables = mutableListOf<Throwable>()
        for (adapterFactory in streamAdapterFactories) {
            try {
                return adapterFactory.create(type)
            } catch (e: Throwable) {
                // This type is not supported by this adapter
                throwables.add(e)
            }
        }
        val compositeException = CompositeException(*throwables.toTypedArray())
        throw IllegalStateException("Cannot resolve stream adapter for type $type.", compositeException)
    }

}
