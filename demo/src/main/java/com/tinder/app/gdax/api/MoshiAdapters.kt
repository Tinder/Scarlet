/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import com.tinder.app.gdax.api.model.ProductId
import com.tinder.app.gdax.api.model.Subscribe

class MoshiAdapters {

    @FromJson
    fun productIdFromJson(string: String): ProductId {
        return ProductId.values().find { it.text == string }!!
    }

    @ToJson
    fun productIdToJson(data: ProductId): String {
        return data.text
    }

    @FromJson
    fun subscribeTypeFromJson(string: String): Subscribe.Type {
        return Subscribe.Type.values().find { it.text == string }!!
    }

    @ToJson
    fun subscribeTypeToJson(data: Subscribe.Type): String {
        return data.text
    }

    @FromJson
    fun subscribeChannelFromJson(string: String): Subscribe.Channel {
        return Subscribe.Channel.values().find { it.text == string }!!
    }

    @ToJson
    fun subscribeChanneloJson(data: Subscribe.Channel): String {
        return data.text
    }
}
