/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.internal.coordinator

import com.tinder.scarlet.Event

internal interface EventCallback {

    fun onEvent(event: Event)
}
