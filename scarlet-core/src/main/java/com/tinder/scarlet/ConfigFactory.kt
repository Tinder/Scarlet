/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

interface ConfigFactory {


    fun createClientOpenOption(): Any?

    fun createClientCloseOption(): Any?

    fun createSendMessageOption(): Any?

}
