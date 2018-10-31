/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.utils

import com.tinder.scarlet.Channel

class SimpleChannelFactory(
    private val createCallable: (listener: Channel.Listener, parent: Channel?) -> Channel?
) : Channel.Factory {
    override fun create(listener: Channel.Listener, parent: Channel?): Channel? {
        return createCallable(listener, parent)
    }
}