/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.view

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tinder.R

class StockPriceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal val stockName = itemView.findViewById<TextView>(R.id.stockName)
    internal val stockPrice = itemView.findViewById<TextView>(R.id.stockPrice)
}
