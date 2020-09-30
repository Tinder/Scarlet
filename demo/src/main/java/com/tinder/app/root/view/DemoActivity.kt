/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.app.root.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.tinder.R
import com.tinder.app.socketio.chatroom.view.ChatRoomFragment
import com.tinder.app.socketio.chatroomservice.view.ChatRoomServiceFragment
import com.tinder.app.sse.stockprice.view.StockPriceFragment
import com.tinder.app.websocket.echo.view.EchoBotFragment
import com.tinder.app.websocket.gdax.view.GdaxFragment

class DemoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_demo)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        TAB_ITEMS.forEach { (name, _) -> tabLayout.addTab(tabLayout.newTab().setText(name)) }
        tabLayout.tabGravity = TabLayout.GRAVITY_CENTER

        val viewPager = findViewById<ViewPager>(R.id.view_pager)
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager, tabLayout.tabCount)
        viewPager.adapter = viewPagerAdapter

        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(viewPager))
    }

    private class ViewPagerAdapter(
        fm: FragmentManager,
        private val pageCount: Int
    ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            val (_, createFragment) = TAB_ITEMS[position]
            return createFragment()
        }

        override fun getCount(): Int {
            return pageCount
        }
    }

    companion object {
        private val TAB_ITEMS = listOf(
            "WS - Echo Bot" to { EchoBotFragment() },
            "WS - GDAX" to { GdaxFragment() },
            "SSE - Stock Price" to { StockPriceFragment() },
            "SocketIo - Chat Room" to { ChatRoomFragment() },
            "SocketIo - Chat Room Service" to { ChatRoomServiceFragment() }
        )
    }
}
