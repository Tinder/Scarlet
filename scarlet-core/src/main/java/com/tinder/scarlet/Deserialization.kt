/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

/**
 * Represents the result of deserializing a message.
 */
sealed class Deserialization<T> {

    data class Success<T>(val value: T, val incomingMessage: Message) : Deserialization<T>()

    data class Error<T>(val throwable: Throwable, val incomingMessage: Message) :
        Deserialization<T>()
}
