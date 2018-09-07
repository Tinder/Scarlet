/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Protocol

// coordinator context?
class ServiceLocator {

    val clientStateCoordinator: ClientStateCoordinator by lazy {
        ClientStateCoordinator(this)
    }
    lateinit var protocolCoordinator: ProtocolCoordinator
    lateinit var topicCoordinator: TopicCoordinator
    lateinit var messageCoordinator: MessageCoordinator

    lateinit var protocolFactory: Protocol.Factory

}
