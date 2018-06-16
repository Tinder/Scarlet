/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet.streamadapter.builtin

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter

class IdentityStreamAdapter<T> : StreamAdapter<T, Stream<T>> {

    override fun adapt(stream: Stream<T>): Stream<T> = stream
}
