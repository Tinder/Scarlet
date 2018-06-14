/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.app.gdax.domain

import org.joda.time.DateTime

data class Transaction(
    val price: Float,
    val timestamp: DateTime
)
