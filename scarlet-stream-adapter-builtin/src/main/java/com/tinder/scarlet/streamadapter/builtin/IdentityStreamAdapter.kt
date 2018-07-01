/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.streamadapter.builtin

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter

class IdentityStreamAdapter<T> : StreamAdapter<T, Stream<T>> {

    override fun adapt(stream: Stream<T>): Stream<T> = stream
}
