/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.domain

import org.joda.time.DateTime

data class Transaction(
    val price: Float,
    val timestamp: DateTime
)
