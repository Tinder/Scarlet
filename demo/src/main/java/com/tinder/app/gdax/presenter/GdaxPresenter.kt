/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.presenter

import com.tinder.app.gdax.domain.Product
import com.tinder.app.gdax.domain.TransactionBook
import com.tinder.app.gdax.domain.TransactionRepository
import com.tinder.app.gdax.target.GdaxTarget
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Flowables
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class GdaxPresenter @Inject constructor(
    private val transactionRepository: TransactionRepository
) {

    private lateinit var target: GdaxTarget
    private var currentProduct = Product.BTC
    private var transactionBook = TransactionBook()
    private val compositeDisposable = CompositeDisposable()

    fun takeTarget(target: GdaxTarget) {
        this.target = target

        val transactionBookSubscription = Flowables.combineLatest(
            Flowable.interval(UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS),
            transactionRepository.observeTransactionBook()
        )
            .map { (_, transactionBook) -> transactionBook }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                transactionBook = it
                showTransactionBook()
            }, Timber::e)
        showTransactionBook()

        compositeDisposable.addAll(transactionBookSubscription)
    }

    fun dropTarget() {
        compositeDisposable.clear()
    }

    fun handleShowBTC() {
        currentProduct = Product.BTC
        showTransactionBook()
    }

    fun handleShowETH() {
        currentProduct = Product.ETH
        showTransactionBook()
    }

    fun handleShowLTC() {
        currentProduct = Product.LTC
        showTransactionBook()
    }

    private fun showTransactionBook() {
        val transactions = transactionBook.getTransactions(currentProduct)
        target.showTransactions(currentProduct, transactions)
    }

    private companion object {
        private const val UPDATE_INTERVAL_SECONDS = 1L
    }
}
