/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

interface RequestFactory<REQUEST: Any> {

    fun createRequest(): Any?
}
