/*
 * © 2018 Match Group, LLC.
 */
package com.tinder.scarlet.stomp.okhttp.core

interface IdGenerator {

    /**
     * Generate a new identifier.
     */
    fun generateId(): String
}