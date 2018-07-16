/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.view

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tinder.R
import com.tinder.app.sse.domain.StockPrice

class StockPriceAdapter : RecyclerView.Adapter<StockPriceViewHolder>() {

    private var stockPrices: List<StockPrice> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockPriceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_stock_price_item, parent, false)
        return StockPriceViewHolder(view)
    }

    override fun getItemCount(): Int = stockPrices.size

    override fun onBindViewHolder(holder: StockPriceViewHolder, position: Int) {
        val stockPrice = stockPrices[position]
        holder.stockName.text = stockPrice.title
        holder.stockPrice.text = "\$${stockPrice.price}"
    }

    fun setStockPrices(stockPrices: List<StockPrice>) {
        this.stockPrices = stockPrices
        notifyDataSetChanged()
    }
}
