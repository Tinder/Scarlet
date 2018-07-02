/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.domain

class TransactionBook(
    transactions: Map<Product, List<Transaction>> = emptyMap()
) {
    private val transactions = transactions.toMutableMap()

    fun getTransactions(product: Product): List<Transaction> = transactions[product] ?: emptyList()

    fun addingTransaction(product: Product, transaction: Transaction): TransactionBook {
        val newHistory = TransactionBook(transactions)
        newHistory.transactions[product] = (newHistory.transactions[product] ?: emptyList()) + transaction
        return newHistory
    }
}
