/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.stockprice.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.tinder.R
import com.tinder.app.sse.stockprice.domain.MarketSnapshot
import org.koin.android.ext.android.inject

class StockPriceFragment : Fragment() {

    private val viewModel: StockPriceViewModel by inject()

    private lateinit var marketSnapshotView: MarketSnapshotView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sse, container, false) as View
        marketSnapshotView = view.findViewById(R.id.marketSnapshot)

        viewModel.marketSnapshot.observe(this, Observer<MarketSnapshot> {
            marketSnapshotView.showMarketSnapshot(it)
        })
        return view
    }

}
