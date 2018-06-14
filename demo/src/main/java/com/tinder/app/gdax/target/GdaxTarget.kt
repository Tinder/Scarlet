/*
 * © 2013 - 2018 Tinder, Inc., ALL RIGHTS RESERVED
 */

package com.tinder.app.gdax.target

import com.tinder.app.gdax.domain.Product
import com.tinder.app.gdax.domain.Transaction

interface GdaxTarget {

    fun showTransactions(product: Product, transactions: List<Transaction>)

}
