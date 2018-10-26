/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.sse.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tinder.R
import com.tinder.app.sse.domain.MarketSnapshot
import com.tinder.app.sse.presenter.SsePresenter
import com.tinder.app.sse.target.SseTarget
import org.koin.android.ext.android.inject

class SseFragment : Fragment(), SseTarget {

    val presenter: SsePresenter by inject()

    private lateinit var marketSnapshotView: MarketSnapshotView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_sse, container, false) as View
        marketSnapshotView = view.findViewById(R.id.marketSnapshot)
        return view
    }

    override fun onResume() {
        super.onResume()
        presenter.takeTarget(this)
    }

    override fun onPause() {
        super.onPause()
        presenter.dropTarget()
    }

    override fun showMarketSnapshot(marketSnapshot: MarketSnapshot) {
        marketSnapshotView.showMarketSnapshot(marketSnapshot)
    }
}
