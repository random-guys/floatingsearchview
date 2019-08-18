package com.arlib.floatingsearchviewdemo

/*
 * Copyright (C) 2015 Ari C.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import android.view.MenuItem

import com.arlib.floatingsearchview.FloatingSearchView
import com.arlib.floatingsearchviewdemo.fragment.BaseExampleFragment
import com.arlib.floatingsearchviewdemo.fragment.ScrollingSearchExampleFragment
import com.arlib.floatingsearchviewdemo.fragment.SlidingSearchResultsExampleFragment
import com.arlib.floatingsearchviewdemo.fragment.SlidingSearchViewExampleFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), BaseExampleFragment.BaseExampleFragmentCallbacks, NavigationView.OnNavigationItemSelectedListener {

    private val TAG = "MainActivity"

    private var mDrawerLayout: DrawerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        showFragment(SlidingSearchResultsExampleFragment())
    }

    override fun onAttachSearchViewToDrawer(searchView: FloatingSearchView) {
        searchView.attachNavigationDrawerToMenuButton(mDrawerLayout!!)
    }

    override fun onBackPressed() {
        val fragments = supportFragmentManager.fragments
        val currentFragment = fragments[fragments.size - 1] as BaseExampleFragment

        if (!currentFragment.onActivityBackPress()) {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        mDrawerLayout!!.closeDrawer(GravityCompat.START)
        when (menuItem.itemId) {
            R.id.sliding_list_example -> {
                showFragment(SlidingSearchResultsExampleFragment())
                return true
            }
            R.id.sliding_search_bar_example -> {
                showFragment(SlidingSearchViewExampleFragment())
                return true
            }
            R.id.scrolling_search_bar_example -> {
                showFragment(ScrollingSearchExampleFragment())
                return true
            }
            else -> return true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, fragment).commit()
    }
}
