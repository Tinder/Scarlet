/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.v3

import com.tinder.scarlet.Lifecycle
import com.tinder.scarlet.Protocol

internal class Core(
    private val lifecycle: Lifecycle
    // user interaction listener?
    // timer

): ServiceLocator {

    override val engineCoordinator by lazy { EngineCoordinator(this) }
    override val protocolCoordinator by lazy { ProtocolCoordinator(this) }
    override val topicCoordinator by lazy { TopicCoordinator(this) }
    override val messageCoordinator by lazy { MessageCoordinator(this) }
    override lateinit var protocolFactory: Protocol.Factory



    fun start() {
        lifecycle.subscribe()
    }

    interface Listener {

    }
}
