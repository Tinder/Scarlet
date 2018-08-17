/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet

// used to pass information around
interface ClientOpenOption
interface ClientCloseOption
interface ServerOpenOption
interface ServerCloseOption
interface ClientMessageInfo
interface ServerMessageInfo

object Null

interface Topic {
    val id: String
}
