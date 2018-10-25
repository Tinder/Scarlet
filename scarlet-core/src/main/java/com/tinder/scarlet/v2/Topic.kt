/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

interface Topic {
    val id: String

    object Main : Topic {
        override val id = ""
    }
}
