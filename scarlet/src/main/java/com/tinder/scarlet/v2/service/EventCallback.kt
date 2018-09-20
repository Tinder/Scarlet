/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2.service

import com.tinder.scarlet.v2.Event


internal interface EventCallback {

    fun onEvent(event: Event)
}
