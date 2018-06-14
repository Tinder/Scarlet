/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.scarlet

/**
 * Represents the result of deserializing a message.
 */
sealed class Deserialization<T> {

    data class Success<T>(val value: T) : Deserialization<T>()

    data class Error<T>(val throwable: Throwable) : Deserialization<T>()
}
