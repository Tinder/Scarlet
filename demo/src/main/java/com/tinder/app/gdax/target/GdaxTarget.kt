/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.target

import com.tinder.app.gdax.domain.Product
import com.tinder.app.gdax.domain.Transaction

interface GdaxTarget {

    fun showTransactions(product: Product, transactions: List<Transaction>)
}
