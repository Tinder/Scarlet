/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.state

import com.tinder.scarlet.Protocol

// coordinator context?
internal interface ServiceLocator {

    val engineCoordinator: EngineCoordinator
    val protocolCoordinator: ProtocolCoordinator
    val topicCoordinator: TopicCoordinator
    val messageCoordinator: MessageCoordinator
    val protocolFactory: Protocol.Factory

}

