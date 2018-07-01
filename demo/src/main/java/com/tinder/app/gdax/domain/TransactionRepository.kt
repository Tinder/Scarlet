/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.domain

import com.tinder.app.gdax.api.GdaxService
import com.tinder.app.gdax.api.model.ProductId
import com.tinder.app.gdax.api.model.Subscribe
import com.tinder.app.gdax.inject.GdaxScope
import com.tinder.scarlet.WebSocket
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor
import org.joda.time.format.ISODateTimeFormat
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@GdaxScope
class TransactionRepository @Inject constructor(
    private val gdaxService: GdaxService
) {

    private val transactionBookRef = AtomicReference(TransactionBook())
    private val transactionBookProcessor = BehaviorProcessor.create<TransactionBook>()

    init {
        gdaxService.observeWebSocketEvent()
            .filter { it is WebSocket.Event.OnConnectionOpened<*> }
            .subscribe({
                val subscribe = Subscribe(
                    type = Subscribe.Type.SUBSCRIBE,
                    productIds = listOf(ProductId.BTC_USD, ProductId.ETH_USD, ProductId.LTC_USD),
                    channels = listOf(Subscribe.Channel.TICKER)
                )

                gdaxService.sendSubscribe(subscribe)
            }, { e ->
                Timber.e(e)
            })

        gdaxService.observeTicker()
            .filter { it.type == "ticker" }
            .map { ticker ->
                val product = when (ticker.product) {
                    ProductId.BTC_USD -> Product.BTC
                    ProductId.ETH_USD -> Product.ETH
                    ProductId.LTC_USD -> Product.LTC
                }
                val price = ticker.price.toFloat()
                val time = ISODateTimeFormat.dateTime().parseDateTime(ticker.time)
                product to Transaction(price, time)
            }
            .subscribe({ (product, transaction) ->
                addTransaction(product, transaction)
            }, { e ->
                Timber.e(e)
            })
    }

    fun observeTransactionBook(): Flowable<TransactionBook> {
        return transactionBookProcessor
    }

    private fun addTransaction(product: Product, transaction: Transaction) {
        val transactionBook = transactionBookRef.get()
            .addingTransaction(product, transaction)
        transactionBookRef.set(transactionBook)
        transactionBookProcessor.onNext(transactionBook)
    }
}
