/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.gdax.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.tinder.R
import com.tinder.app.gdax.domain.Product
import com.tinder.app.gdax.domain.Transaction
import com.tinder.app.gdax.inject.GdaxComponent
import com.tinder.app.gdax.presenter.GdaxPresenter
import com.tinder.app.gdax.target.GdaxTarget
import org.joda.time.DateTime
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat
import java.text.DecimalFormat
import javax.inject.Inject

class GdaxFragment : Fragment(), GdaxTarget {

    @Inject
    lateinit var presenter: GdaxPresenter
    private lateinit var chart: LineChart
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (context!!.applicationContext as GdaxComponent.ComponentProvider).gdaxComponent
            .inject(this)

        val view = inflater.inflate(R.layout.fragment_gdax, container, false) as View

        chart = view.findViewById(R.id.chart)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        with(menu) {
            val subMenu = addSubMenu(Menu.NONE, CURRENCIES_SUB_MENU_ID, Menu.NONE, CURRENCIES_SUB_MENU_TITLE)
            subMenu.item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            with(subMenu) {
                add(BTC_USD_MENU_ITEM_TITLE)
                add(ETH_USD_MENU_ITEM_TITLE)
                add(LTC_USD_MENU_ITEM_TITLE)
            }
        }
        this.menu = menu
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.title) {
            BTC_USD_MENU_ITEM_TITLE -> presenter.handleShowBTC()
            ETH_USD_MENU_ITEM_TITLE -> presenter.handleShowETH()
            LTC_USD_MENU_ITEM_TITLE -> presenter.handleShowLTC()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showTransactions(product: Product, transactions: List<Transaction>) {
        val chartTitle = "$product-USD"
        menu?.findItem(CURRENCIES_SUB_MENU_ID)?.title = chartTitle

        val now = DateTime.now()
        val minutesAgo = now.minusMinutes(DISPLAYED_HISTORY_DURATION_MINUTES)

        val priceEntries = transactions
            .filter { it.timestamp.isAfter(minutesAgo) }
            .map { Entry(it.timestamp.millisOfDay.toFloat(), it.price) }
            .toMutableList()
            .apply {
                add(
                    0,
                    Entry(minutesAgo.millisOfDay.toFloat(), if (isEmpty()) 0F else this[0].y)
                )
            }
        val priceLineDataSet = LineDataSet(priceEntries, chartTitle)
            .apply {
                mode = LineDataSet.Mode.LINEAR
                setDrawFilled(true)
                isHighlightEnabled = false
                setDrawValues(false)
            }

        val minPrice = priceEntries.minBy { it.y }?.y ?: 0F
        val minPriceEntries = listOf(
            Entry(minutesAgo.millisOfDay.toFloat(), minPrice),
            Entry(now.millisOfDay.toFloat(), minPrice)
        )
        val minPriceLineDataSet = LineDataSet(minPriceEntries, MIN_DATA_SET_LABEL)
            .apply {
                val minPriceColor = MIN_PRICE_COLOR
                mode = LineDataSet.Mode.STEPPED
                setCircleColorHole(Color.TRANSPARENT)
                color = minPriceColor
                setCircleColor(minPriceColor)
                isHighlightEnabled = false
                setDrawValues(false)
            }

        with(chart) {
            axisRight.isEnabled = false
            with(xAxis) {
                setValueFormatter { value, _ ->
                    LocalTime.fromMillisOfDay(value.toLong()).toString(DateTimeFormat.shortTime())
                }
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 10000F
            }

            with(axisLeft) {
                granularity = 0.01F
                setDrawGridLines(false)
                setValueFormatter { value, _ ->
                    PRICE_FORMAT.format(value)
                }
            }

            description.isEnabled = false

            setTouchEnabled(false)

            data = LineData(priceLineDataSet, minPriceLineDataSet)
            invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.takeTarget(this)
    }

    override fun onPause() {
        super.onPause()
        presenter.dropTarget()
    }

    companion object {
        private const val DISPLAYED_HISTORY_DURATION_MINUTES = 3

        private const val MIN_DATA_SET_LABEL = "Min"
        private val MIN_PRICE_COLOR = Color.argb(50, 140, 234, 255)

        private val PRICE_FORMAT = DecimalFormat("\$##.00")

        private const val CURRENCIES_SUB_MENU_TITLE = ""
        private const val CURRENCIES_SUB_MENU_ID = 300
        private const val BTC_USD_MENU_ITEM_TITLE = "BTC-USD"
        private const val ETH_USD_MENU_ITEM_TITLE = "ETH-USD"
        private const val LTC_USD_MENU_ITEM_TITLE = "LTC-USD"
    }
}
