/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.view

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.tinder.R

class StockPriceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal val stockName = itemView.findViewById<TextView>(R.id.stockName)
    internal val stockPrice = itemView.findViewById<TextView>(R.id.stockPrice)
}
