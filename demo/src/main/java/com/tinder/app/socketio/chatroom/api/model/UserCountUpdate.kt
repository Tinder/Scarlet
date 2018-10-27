package com.tinder.app.socketio.chatroom.api.model

import com.squareup.moshi.Json

data class UserCountUpdate(
    val username: String,
    @Json(name = "numUsers")
    val numberOfUsers: Int
)