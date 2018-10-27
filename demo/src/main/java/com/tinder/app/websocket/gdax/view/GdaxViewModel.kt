package com.tinder.app.websocket.gdax.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tinder.app.websocket.gdax.domain.Product
import com.tinder.app.websocket.gdax.domain.Transaction
import com.tinder.app.websocket.gdax.domain.TransactionRepository
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import java.util.concurrent.TimeUnit

class GdaxViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val currentProduct = MutableLiveData<Product>()
        .apply { postValue(Product.BTC) }

    private val transactionBook = LiveDataReactiveStreams.fromPublisher(
        Flowables.combineLatest(
            Flowable.interval(UPDATE_INTERVAL_SECONDS, TimeUnit.SECONDS),
            transactionRepository.observeTransactionBook()
        )
            .map { (_, transactionBook) -> transactionBook }
    )

    val currentTransactions: LiveData<CurrentTransactions> = MediatorLiveData<CurrentTransactions>()
        .apply {
            postValue(DEFAULT_CURRENT_TRANSACRIONS)
            addSource(currentProduct) { product ->
                val currentTransactions = value ?: DEFAULT_CURRENT_TRANSACRIONS
                postValue(currentTransactions.copy(product = product))
            }
            addSource(transactionBook) { transactionBook ->
                val currentTransactions = value ?: DEFAULT_CURRENT_TRANSACRIONS
                postValue(
                    currentTransactions.copy(
                        transactions = transactionBook.getTransactions(
                            currentTransactions.product
                        )
                    )
                )
            }
        }

    fun handleShowBTC() {
        currentProduct.postValue(Product.BTC)
    }

    fun handleShowETH() {
        currentProduct.postValue(Product.ETH)
    }

    fun handleShowLTC() {
        currentProduct.postValue(Product.LTC)
    }

    data class CurrentTransactions(
        val product: Product,
        val transactions: List<Transaction>
    )

    private companion object {
        private const val UPDATE_INTERVAL_SECONDS = 1L
        private val DEFAULT_CURRENT_TRANSACRIONS = CurrentTransactions(Product.BTC, emptyList())
    }
}