/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import java.lang.reflect.Type

interface ProtocolEventAdapter {
    fun fromEvent(event: ProtocolEvent): ProtocolSpecificEvent =
        ProtocolSpecificEvent.Empty

    interface Factory {
        fun create(type: Type, annotations: Array<Annotation>): ProtocolEventAdapter = object :
            ProtocolEventAdapter {}
    }
}
