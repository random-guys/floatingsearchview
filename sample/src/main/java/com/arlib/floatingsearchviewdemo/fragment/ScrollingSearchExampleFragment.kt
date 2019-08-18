package com.arlib.floatingsearchviewdemo.fragment

import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import com.arlib.floatingsearchview.FloatingSearchView
import com.arlib.floatingsearchview.FloatingSearchView.*
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter
import com.arlib.floatingsearchview.suggestions.SearchSuggestion
import com.arlib.floatingsearchview.util.Util
import com.arlib.floatingsearchviewdemo.R
import com.arlib.floatingsearchviewdemo.data.ColorSuggestion
import com.arlib.floatingsearchviewdemo.data.ColorWrapper
import com.arlib.floatingsearchviewdemo.data.DataHelper
import com.google.android.material.appbar.AppBarLayout


class ScrollingSearchExampleFragment : BaseExampleFragment(), AppBarLayout.OnOffsetChangedListener {

    private var mSearchView: FloatingSearchView? = null

    private var mIsDarkSearchTheme = false

    private var mLastQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_scrolling_search_example, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSearchView = view.findViewById(R.id.floating_search_view)
        val mAppBar = view.findViewById<AppBarLayout>(R.id.appbar)

        mAppBar.addOnOffsetChangedListener(this)

        setupDrawer()
        setupSearchBar()
    }

    private fun setupSearchBar() {
        mSearchView!!.setOnQueryChangeListener(object : OnQueryChangeListener{
            override fun onSearchTextChanged(oldQuery: String, newQuery: String) {
                if (oldQuery != "" && newQuery == "") {
                    mSearchView!!.clearSuggestions()
                } else {

                    //this shows the top left circular progress
                    //you can call it where ever you want, but
                    //it makes sense to do it when loading something in
                    //the background.
                    mSearchView!!.showProgress()

                    //simulates a query call to a data source
                    //with a new query.
                    DataHelper.findSuggestions(activity!!, newQuery, 5,
                            FIND_SUGGESTION_SIMULATED_DELAY, object : DataHelper.OnFindSuggestionsListener {
                        override fun onResults(results: List<ColorSuggestion>) {
                            //this will swap the data and
                            //render the collapse/expand animations as necessary
                            mSearchView!!.swapSuggestions(results)

                            //let the users know that the background
                            //process has completed
                            mSearchView!!.hideProgress()
                        }

                    })
                }

                Log.d(TAG, "onSearchTextChanged()")
            }
        })

        mSearchView!!.setOnSearchListener(object : OnSearchListener {
            override fun onSuggestionClicked(searchSuggestion: SearchSuggestion) {

                val colorSuggestion = searchSuggestion as ColorSuggestion
                DataHelper.findColors(activity!!, colorSuggestion.body!!, object : DataHelper.OnFindColorsListener {
                    override fun onResults(results: List<ColorWrapper>) {

                    }
                })
                Log.d(TAG, "onSuggestionClicked()")

                mLastQuery = searchSuggestion.body
            }

            override fun onSearchAction(currentQuery: String) {
                mLastQuery = currentQuery

                DataHelper.findColors(activity!!, currentQuery, object : DataHelper.OnFindColorsListener {
                    override fun onResults(results: List<ColorWrapper>) {

                    }
                })
                Log.d(TAG, "onSearchAction()")
            }
        })

        mSearchView!!.setOnFocusChangeListener(object : OnFocusChangeListener {
            override fun onFocus() {

                //show suggestions when search bar gains focus (typically history suggestions)
                mSearchView?.swapSuggestions(DataHelper.getHistory(activity!!, 3))

                Log.d(TAG, "onFocus()")
            }

            override fun onFocusCleared() {

                //set the title of the bar so that when focus is returned a new query begins
                mSearchView?.setSearchBarTitle(mLastQuery)

                //you can also set setSearchText(...) to make keep the query there when not focused and when focus returns
                //mSearchView.setSearchText(searchSuggestion.getBody());

                Log.d(TAG, "onFocusCleared()")
            }
        })


        //handle menu clicks the same way as you would
        //in a regular activity
        mSearchView!!.setOnMenuItemClickListener(object : OnMenuItemClickListener {
            override fun onActionMenuItemSelected(item: MenuItem) {
                if (item.itemId == R.id.action_change_colors) {

                    mIsDarkSearchTheme = true

                    //demonstrate setting colors for items
                    mSearchView?.setBackgroundColor(Color.parseColor("#787878"))
                    mSearchView?.setViewTextColor(Color.parseColor("#e9e9e9"))
                    mSearchView?.setHintTextColor(Color.parseColor("#e9e9e9"))
                    mSearchView?.setActionMenuOverflowColor(Color.parseColor("#e9e9e9"))
                    mSearchView?.setMenuItemIconColor(Color.parseColor("#e9e9e9"))
                    mSearchView?.setLeftActionIconColor(Color.parseColor("#e9e9e9"))
                    mSearchView?.setClearBtnColor(Color.parseColor("#e9e9e9"))
                    mSearchView?.setDividerColor(Color.parseColor("#BEBEBE"))
                    mSearchView?.setLeftActionIconColor(Color.parseColor("#e9e9e9"))
                } else {

                    //just print action
                    Toast.makeText(activity!!.applicationContext, item.title,
                            Toast.LENGTH_SHORT).show()
                }
            }
        })

        //use this listener to listen to menu clicks when app:floatingSearch_leftAction="showHome"
        mSearchView?.setOnHomeActionClickListener(object : OnHomeActionClickListener {
            override fun onHomeClicked() {
                Log.d(TAG, "onHomeClicked()")
            }
        })

        /*
         * Here you have access to the left icon and the text of a given suggestion
         * item after as it is bound to the suggestion list. You can utilize this
         * callback to change some properties of the left icon and the text. For example, you
         * can load the left icon images using your favorite image loading library, or change text color.
         *
         *
         * Important:
         * Keep in mind that the suggestion list is a RecyclerView, so views are reused for different
         * items in the list.
         */
        mSearchView!!.setOnBindSuggestionCallback(object : SearchSuggestionsAdapter.OnBindSuggestionCallback {
            override fun onBindSuggestion(suggestionView: View, leftIcon: ImageView, textView: TextView, item: SearchSuggestion, itemPosition: Int) {
                val colorSuggestion = item as ColorSuggestion

                val textColor = if (mIsDarkSearchTheme) "#ffffff" else "#000000"
                val textLight = if (mIsDarkSearchTheme) "#bfbfbf" else "#787878"

                if (colorSuggestion.isHistory) {
                    leftIcon.setImageDrawable(ResourcesCompat.getDrawable(resources,
                            R.drawable.ic_history_black_24dp, null))

                    Util.setIconColor(leftIcon, Color.parseColor(textColor))
                    leftIcon.alpha = .36f
                } else {
                    leftIcon.alpha = 0.0f
                    leftIcon.setImageDrawable(null)
                }

                textView.setTextColor(Color.parseColor(textColor))
                val text = colorSuggestion.body.replaceFirst(mSearchView!!.query.toRegex(),
                        "<font color=\"" + textLight + "\">" + mSearchView!!.query + "</font>")
                textView.text = Html.fromHtml(text)
            }
        })
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        mSearchView!!.translationY = verticalOffset.toFloat()
    }

    override fun onActivityBackPress(): Boolean {
        //if mSearchView.setSearchFocused(false) causes the focused search
        //to close, then we don't want to close the activity. if mSearchView.setSearchFocused(false)
        //returns false, we know that the search was already closed so the call didn't change the focus
        //state and it makes sense to call supper onBackPressed() and close the activity
        return mSearchView!!.setSearchFocused(false)
    }

    private fun setupDrawer() {
        mSearchView?.let { attachSearchViewActivityDrawer(it) }
    }

    companion object {

        const val FIND_SUGGESTION_SIMULATED_DELAY: Long = 250
        private const val TAG = "BlankFragment"
    }
}
