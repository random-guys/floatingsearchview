package com.arlib.floatingsearchviewdemo.fragment

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
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
import androidx.core.animation.doOnEnd
import androidx.core.content.res.ResourcesCompat
import com.arlib.floatingsearchview.FloatingSearchView
import com.arlib.floatingsearchview.FloatingSearchView.*
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter
import com.arlib.floatingsearchview.suggestions.SearchSuggestion
import com.arlib.floatingsearchview.util.Util
import com.arlib.floatingsearchviewdemo.R
import com.arlib.floatingsearchviewdemo.data.ColorSuggestion
import com.arlib.floatingsearchviewdemo.data.DataHelper


class SlidingSearchViewExampleFragment : BaseExampleFragment() {

    private var mHeaderView: View? = null
    private var mDimSearchViewBackground: View? = null
    private var mDimDrawable: ColorDrawable? = null
    private var mSearchView: FloatingSearchView? = null

    private var mIsDarkSearchTheme = false

    private var mLastQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sliding_search_example, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mSearchView = view.findViewById<View>(R.id.floating_search_view) as FloatingSearchView
        mHeaderView = view.findViewById(R.id.header_view)

        mDimSearchViewBackground = view.findViewById(R.id.dim_background)
        mDimDrawable = ColorDrawable(Color.BLACK)
        mDimDrawable?.alpha = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mDimSearchViewBackground?.background = mDimDrawable
        } else {
            mDimSearchViewBackground?.setBackgroundDrawable(mDimDrawable)
        }

        setupFloatingSearch()
        setupDrawer()
    }

    private fun setupFloatingSearch() {
        mSearchView?.setOnQueryChangeListener(object : OnQueryChangeListener {
            override fun onSearchTextChanged(oldQuery: String, newQuery: String) {
                if (oldQuery != "" && newQuery == "") {
                    mSearchView?.clearSuggestions()
                } else {

                    //this shows the top left circular progress
                    //you can call it where ever you want, but
                    //it makes sense to do it when loading something in
                    //the background.
                    mSearchView?.showProgress()

                    //simulates a query call to a data source
                    //with a new query.
                    DataHelper.findSuggestions(activity!!, newQuery, 5, FIND_SUGGESTION_SIMULATED_DELAY, object : DataHelper.OnFindSuggestionsListener {
                        override fun onResults(results: List<ColorSuggestion>) {
                            //this will swap the data and
                            //render the collapse/expand animations as necessary
                            mSearchView?.swapSuggestions(results)

                            //let the users know that the background
                            //process has completed
                            mSearchView?.hideProgress()
                        }
                    })
                }

                Log.d(TAG, "onSearchTextChanged()")
            }
        })

        mSearchView?.setOnSearchListener(object : OnSearchListener {
            override fun onSuggestionClicked(searchSuggestion: SearchSuggestion) {

                mLastQuery = searchSuggestion.body
            }

            override fun onSearchAction(query: String) {
                mLastQuery = query

                Log.d(TAG, "onSearchAction()")
            }
        })

        mSearchView?.setOnFocusChangeListener(object : OnFocusChangeListener {
            @SuppressLint("ObjectAnimatorBinding")
            override fun onFocus() {
                val headerHeight = resources.getDimensionPixelOffset(R.dimen.sliding_search_view_header_height)
                val anim = ObjectAnimator.ofFloat(mSearchView, "translationY", headerHeight.toFloat(), 0f)
//                val anim = ValueAnimator.ofFloat(0f, headerHeight.toFloat())
//                anim.addUpdateListener { mSearchView?.translationY = it.animatedValue as Float }
                anim.duration = 350
                fadeDimBackground(0, 150, null)
                anim.doOnEnd {
                    //show suggestions when search bar gains focus (typically history suggestions)
                    mSearchView?.swapSuggestions(DataHelper.getHistory(activity!!, 3))
                }

//                anim.addListener(object : AnimatorListenerAdapter() {
//
//                    override fun onAnimationEnd(animation: Animator) {
//                        //show suggestions when search bar gains focus (typically history suggestions)
//                        mSearchView?.swapSuggestions(DataHelper.getHistory(activity!!, 3))
//
//                    }
//                })
                anim.start()

                Log.d(TAG, "onFocus()")
            }

            @SuppressLint("ObjectAnimatorBinding")
            override fun onFocusCleared() {
                val headerHeight = resources.getDimensionPixelOffset(R.dimen.sliding_search_view_header_height)
                val anim = ObjectAnimator.ofFloat(mSearchView, "translationY", 0f, headerHeight.toFloat())
//                val anim = ValueAnimator.ofFloat(0f, headerHeight.toFloat())
//                anim.addUpdateListener { mSearchView?.translationY = it.animatedValue as Float }
                anim.duration = 350
                anim.start()
                fadeDimBackground(150, 0, null)

                //set the title of the bar so that when focus is returned a new query begins
                mSearchView?.setSearchBarTitle(mLastQuery)

                //you can also set setSearchText(...) to make keep the query there when not focused and when focus returns
                //mSearchView.setSearchText(searchSuggestion.getBody());

                Log.d(TAG, "onFocusCleared()")
            }
        })


        //handle menu clicks the same way as you would
        //in a regular activity
        mSearchView?.setOnMenuItemClickListener(object : OnMenuItemClickListener {
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
                    Toast.makeText(activity?.applicationContext, item.title,
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
        mSearchView?.setOnBindSuggestionCallback(object : SearchSuggestionsAdapter.OnBindSuggestionCallback {
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
                val text = colorSuggestion.body?.replaceFirst(mSearchView?.query?.toRegex()!!,
                        "<font color=\"" + textLight + "\">" + mSearchView?.query + "</font>")
                textView.text = Html.fromHtml(text)
            }
        })

        /*
         * When the user types some text into the search field, a clear button (and 'x' to the
         * right) of the search text is shown.
         *
         * This listener provides a callback for when this button is clicked.
         */
        mSearchView?.setOnClearSearchActionListener(object : OnClearSearchActionListener {
            override fun onClearSearchClicked() {
                Log.d(TAG, "onClearSearchClicked()")
            }
        })
    }


    override fun onActivityBackPress(): Boolean {
        //if mSearchView.setSearchFocused(false) causes the focused search
        //to close, then we don't want to close the activity. if mSearchView.setSearchFocused(false)
        //returns false, we know that the search was already closed so the call didn't change the focus
        //state and it makes sense to call supper onBackPressed() and close the activity
        return mSearchView?.setSearchFocused(false)!!
    }

    private fun setupDrawer() {
        attachSearchViewActivityDrawer(mSearchView!!)
    }

    private fun fadeDimBackground(from: Int, to: Int, listener: Animator.AnimatorListener?) {
        val anim = ValueAnimator.ofInt(from, to)
        anim.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            mDimDrawable?.alpha = value
        }
        if (listener != null) {
            anim.addListener(listener)
        }
        anim.duration = ANIM_DURATION
        anim.start()
    }

    companion object {

        const val FIND_SUGGESTION_SIMULATED_DELAY: Long = 250

        private const val ANIM_DURATION: Long = 350
        private const val TAG = "BlankFragment"
    }
}
