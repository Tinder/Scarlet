/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

import java.lang.reflect.Type

interface ProtocolSpecificEventAdapter {
    fun fromEvent(event: ProtocolEvent): ProtocolSpecificEvent =
        ProtocolSpecificEvent.Empty

    interface Factory {
        fun create(type: Type, annotations: Array<Annotation>): ProtocolSpecificEventAdapter {
            return object : ProtocolSpecificEventAdapter {}
        }
    }
}
