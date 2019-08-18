package com.arlib.floatingsearchview

/*
  Copyright (C) 2015 Ari C.
  <p/>
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

import android.animation.*
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.annotation.IntDef
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuBuilder.Callback
import androidx.appcompat.view.menu.MenuItemImpl
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListenerAdapter
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arlib.floatingsearchview.suggestions.SearchSuggestion
import com.arlib.floatingsearchview.suggestions.SearchSuggestionsAdapter
import com.arlib.floatingsearchview.util.Util
import com.arlib.floatingsearchview.util.Util.dpToPx
import com.arlib.floatingsearchview.util.adapter.GestureDetectorListenerAdapter
import com.arlib.floatingsearchview.util.adapter.OnItemTouchListenerAdapter
import com.arlib.floatingsearchview.util.adapter.TextWatcherAdapter
import com.arlib.floatingsearchview.util.view.MenuView
import com.arlib.floatingsearchview.util.view.SearchInputView
import com.bartoszlipinski.viewpropertyobjectanimator.ViewPropertyObjectAnimator
import java.util.*
import kotlin.math.abs

/**
 * A search UI widget that implements a floating search box also called persistent
 * search.
 */
class FloatingSearchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {

    private var mHostActivity: Activity? = null

    private var mMainLayout: View? = null
    private var mBackgroundDrawable: Drawable? = null
    private var mDimBackground: Boolean = false
    private var mDismissOnOutsideTouch = true
    var isSearchBarFocused: Boolean = false
        private set
    private var mFocusChangeListener: OnFocusChangeListener? = null
    private var mDismissFocusOnItemSelection = ATTRS_DISMISS_FOCUS_ON_ITEM_SELECTION_DEFAULT

    private var mQuerySection: CardView? = null
    private var mSearchListener: OnSearchListener? = null
    private var mSearchInput: SearchInputView? = null
    private var mQueryTextSize: Int = 0
    private var mCloseSearchOnSofteKeyboardDismiss: Boolean = false
    private var mTitleText: String? = null
    private var mIsTitleSet: Boolean = false
    private var mSearchInputTextColor = -1
    private var mSearchInputHintColor = -1
    private var mSearchInputParent: View? = null
    /**
     * Returns the current query text.
     *
     * @return the current query
     */
    var query = ""
        private set
    private var mQueryListener: OnQueryChangeListener? = null
    private var mLeftAction: ImageView? = null
    private var mOnMenuClickListener: OnLeftMenuClickListener? = null
    private var mOnHomeActionClickListener: OnHomeActionClickListener? = null
    private var mSearchProgress: ProgressBar? = null
    private var mMenuBtnDrawable: DrawerArrowDrawable? = null
    private var mIconBackArrow: Drawable? = null
    private var mIconSearch: Drawable? = null
    @LeftActionMode
    internal var mLeftActionMode = LEFT_ACTION_MODE_NOT_SET
    private var mLeftActionIconColor: Int = 0
    private var mSearchHint: String? = null
    private var mShowSearchKey: Boolean = false
    private var mMenuOpen = false
    private var mMenuView: MenuView? = null
    private var mMenuId = -1
    private var mActionMenuItemColor: Int = 0
    private var mOverflowIconColor: Int = 0
    private var mActionMenuItemListener: OnMenuItemClickListener? = null
    private var mClearButton: ImageView? = null
    private var mClearBtnColor: Int = 0
    private var mIconClear: Drawable? = null
    private var mBackgroundColor: Int = 0
    private var mSkipQueryFocusChangeEvent: Boolean = false
    private var mSkipTextChangeEvent: Boolean = false
    private val mLeftMenuClickListener: OnClickListener? = null

    private var mDivider: View? = null
    private var mDividerColor: Int = 0

    private var mSuggestionsSection: RelativeLayout? = null
    private var mSuggestionListContainer: View? = null
    private var mSuggestionsList: RecyclerView? = null
    private var mSuggestionTextColor = -1
    private var mSuggestionRightIconColor: Int = 0
    private var mSuggestionsAdapter: SearchSuggestionsAdapter? = null
    private var mOnBindSuggestionCallback: SearchSuggestionsAdapter.OnBindSuggestionCallback? = null
    private var mSuggestionsTextSizePx: Int = 0
    private var mIsInitialLayout = true
    private var mIsSuggestionsSectionHeightSet: Boolean = false
    private var mShowMoveUpSuggestion = ATTRS_SHOW_MOVE_UP_SUGGESTION_DEFAULT
    private var mOnSuggestionsListHeightChanged: OnSuggestionsListHeightChanged? = null
    private var mSuggestionSectionAnimDuration: Long = 0
    private var mOnClearSearchActionListener: OnClearSearchActionListener? = null

    private var mSuggestionSecHeightListener: OnSuggestionSecHeightSetListener? = null

    /**
     * Provides clients access to the menu items
     *
     * @return
     */
    val currentMenuItems: List<MenuItemImpl>?
        get() = mMenuView!!.currentMenuItems

    private val isRTL: Boolean
        get() {

            val config = resources.configuration
            return ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
        }

    private val mDrawerListener = DrawerListener()

    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    @IntDef(LEFT_ACTION_MODE_SHOW_HAMBURGER, LEFT_ACTION_MODE_SHOW_SEARCH, LEFT_ACTION_MODE_SHOW_HOME, LEFT_ACTION_MODE_NO_LEFT_ACTION, LEFT_ACTION_MODE_NOT_SET)
    annotation class LeftActionMode

    //An interface for implementing a listener that will get notified when the suggestions
    //section's height is set. This is to be used internally only.
    private interface OnSuggestionSecHeightSetListener {
        fun onSuggestionSecHeightSet()
    }

    /**
     * Interface for implementing a listener to listen to
     * changes in the suggestion list height that occur when the list is expands/shrinks
     * following calls to [FloatingSearchView.swapSuggestions]
     */
    interface OnSuggestionsListHeightChanged {

        fun onSuggestionsListHeightChanged(newHeight: Float)
    }

    /**
     * Interface for implementing a listener to listen
     * to state changes in the query text.
     */
    interface OnQueryChangeListener {

        /**
         * Called when the query has changed. It will
         * be invoked when one or more characters in the
         * query was changed.
         *
         * @param oldQuery the previous query
         * @param newQuery the new query
         */
        fun onSearchTextChanged(oldQuery: String, newQuery: String)
    }

    /**
     * Interface for implementing a listener to listen
     * to when the current search has completed.
     */
    interface OnSearchListener {

        /**
         * Called when a suggestion was clicked indicating
         * that the current search has completed.
         *
         * @param searchSuggestion
         */
        fun onSuggestionClicked(searchSuggestion: SearchSuggestion)

        /**
         * Called when the current search has completed
         * as a result of pressing search key in the keyboard.
         *
         *
         * Note: This will only get called if
         * [FloatingSearchView.setShowSearchKey]} is set to true.
         *
         * @param currentQuery the text that is currently set in the query TextView
         */
        fun onSearchAction(currentQuery: String)
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the left menu (navigation menu) is
     * clicked.
     *
     *
     * Note: This is only relevant when leftActionMode is
     * set to {@value #LEFT_ACTION_MODE_SHOW_HAMBURGER}
     */
    interface OnLeftMenuClickListener {

        /**
         * Called when the menu button was
         * clicked and the menu's state is now opened.
         */
        fun onMenuOpened()

        /**
         * Called when the back button was
         * clicked and the menu's state is now closed.
         */
        fun onMenuClosed()
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the home action button (the back arrow)
     * is clicked.
     *
     *
     * Note: This is only relevant when leftActionMode is
     * set to {@value #LEFT_ACTION_MODE_SHOW_HOME}
     */
    interface OnHomeActionClickListener {

        /**
         * Called when the home button was
         * clicked.
         */
        fun onHomeClicked()
    }

    /**
     * Interface for implementing a listener to listen
     * when an item in the action (the item can be presented as an action
     * ,or as a menu item in the overflow menu) menu has been selected.
     */
    interface OnMenuItemClickListener {

        /**
         * Called when a menu item in has been
         * selected.
         *
         * @param item the selected menu item.
         */
        fun onActionMenuItemSelected(item: MenuItem)
    }

    /**
     * Interface for implementing a listener to listen
     * to for focus state changes.
     */
    interface OnFocusChangeListener {

        /**
         * Called when the search bar has gained focus
         * and listeners are now active.
         */
        fun onFocus()

        /**
         * Called when the search bar has lost focus
         * and listeners are no more active.
         */
        fun onFocusCleared()
    }

    /**
     * Interface for implementing a callback to be
     * invoked when the clear search text action button
     * (the x to the right of the text) is clicked.
     */
    interface OnClearSearchActionListener {

        /**
         * Called when the clear search text button
         * was clicked.
         */
        fun onClearSearchClicked()
    }

    init {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {

        mHostActivity = Util.getHostActivity(context)

        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mMainLayout = View.inflate(context, R.layout.floating_search_layout, this)
        mBackgroundDrawable = ColorDrawable(Color.BLACK)

        mQuerySection = findViewById<View>(R.id.search_query_section) as CardView
        mClearButton = findViewById<View>(R.id.clear_btn) as ImageView
        mSearchInput = findViewById<View>(R.id.search_bar_text) as SearchInputView
        mSearchInputParent = findViewById(R.id.search_input_parent)
        mLeftAction = findViewById<View>(R.id.left_action) as ImageView
        mSearchProgress = findViewById<View>(R.id.search_bar_search_progress) as ProgressBar
        initDrawables()
        mClearButton!!.setImageDrawable(mIconClear)
        mMenuView = findViewById<View>(R.id.menu_view) as MenuView

        mDivider = findViewById(R.id.divider)

        mSuggestionsSection = findViewById<View>(R.id.search_suggestions_section) as RelativeLayout
        mSuggestionListContainer = findViewById(R.id.suggestions_list_container)
        mSuggestionsList = findViewById<View>(R.id.suggestions_list) as RecyclerView

        setupViews(attrs)
    }

    private fun initDrawables() {
        mMenuBtnDrawable = DrawerArrowDrawable(context)
        mIconClear = Util.getWrappedDrawable(context, R.drawable.ic_clear_black_24dp)
        mIconBackArrow = Util.getWrappedDrawable(context, R.drawable.ic_arrow_back_black_24dp)
        mIconSearch = Util.getWrappedDrawable(context, R.drawable.ic_search_black_24dp)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        if (mIsInitialLayout) {

            //we need to add 5dp to the mSuggestionsSection because we are
            //going to move it up by 5dp in order to cover the search bar's
            //shadow padding and rounded corners. We also need to add an additional 10dp to
            //mSuggestionsSection in order to hide mSuggestionListContainer's
            //rounded corners and shadow for both, top and bottom.
            val addedHeight = 3 * dpToPx(CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT)
            val finalHeight = mSuggestionsSection!!.height + addedHeight
            mSuggestionsSection!!.layoutParams.height = finalHeight
            mSuggestionsSection!!.requestLayout()
            val vto = mSuggestionListContainer!!.viewTreeObserver
            vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {

                    if (mSuggestionsSection!!.height == finalHeight) {
                        Util.removeGlobalLayoutObserver(mSuggestionListContainer!!, this)

                        mIsSuggestionsSectionHeightSet = true
                        moveSuggestListToInitialPos()
                        if (mSuggestionSecHeightListener != null) {
                            mSuggestionSecHeightListener!!.onSuggestionSecHeightSet()
                            mSuggestionSecHeightListener = null
                        }
                    }
                }
            })

            mIsInitialLayout = false

            refreshDimBackground()

            if (isInEditMode) {
                inflateOverflowMenu(mMenuId)
            }
        }
    }

    private fun setupViews(attrs: AttributeSet?) {

        mSuggestionsSection!!.isEnabled = false

        if (attrs != null) {
            applyXmlAttributes(attrs)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            background = mBackgroundDrawable
        } else {
            setBackgroundDrawable(mBackgroundDrawable)
        }

        setupQueryBar()

        if (!isInEditMode) {
            setupSuggestionSection()
        }
    }

    private fun applyXmlAttributes(attrs: AttributeSet) {

        val a = context.obtainStyledAttributes(attrs, R.styleable.FloatingSearchView)

        try {

            val searchBarWidth = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarWidth,
                    ViewGroup.LayoutParams.MATCH_PARENT)
            mQuerySection!!.layoutParams.width = searchBarWidth
            mDivider!!.layoutParams.width = searchBarWidth
            mSuggestionListContainer!!.layoutParams.width = searchBarWidth
            val searchBarLeftMargin = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarMarginLeft,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT)
            val searchBarTopMargin = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarMarginTop,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT)
            val searchBarRightMargin = a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchBarMarginRight,
                    ATTRS_SEARCH_BAR_MARGIN_DEFAULT)
            val querySectionLP = mQuerySection!!.layoutParams as FrameLayout.LayoutParams
            val dividerLP = mDivider!!.layoutParams as FrameLayout.LayoutParams
            val suggestListSectionLP = mSuggestionsSection!!.layoutParams as LinearLayout.LayoutParams
            val cardPadding = dpToPx(CARD_VIEW_TOP_BOTTOM_SHADOW_HEIGHT)
            querySectionLP.setMargins(searchBarLeftMargin, searchBarTopMargin,
                    searchBarRightMargin, 0)
            dividerLP.setMargins(searchBarLeftMargin + cardPadding, 0,
                    searchBarRightMargin + cardPadding,
                    (mDivider!!.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin)
            suggestListSectionLP.setMargins(searchBarLeftMargin, 0, searchBarRightMargin, 0)
            mQuerySection!!.layoutParams = querySectionLP
            mDivider!!.layoutParams = dividerLP
            mSuggestionsSection!!.layoutParams = suggestListSectionLP

            setQueryTextSize(a.getDimensionPixelSize(R.styleable.FloatingSearchView_floatingSearch_searchInputTextSize,
                    ATTRS_QUERY_TEXT_SIZE_SP_DEFAULT))
            setSearchHint(a.getString(R.styleable.FloatingSearchView_floatingSearch_searchHint))
            setShowSearchKey(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_showSearchKey,
                    ATTRS_SEARCH_BAR_SHOW_SEARCH_KEY_DEFAULT))
            setCloseSearchOnKeyboardDismiss(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_close_search_on_keyboard_dismiss,
                    ATTRS_DISMISS_ON_KEYBOARD_DISMISS_DEFAULT))
            setDismissOnOutsideClick(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_dismissOnOutsideTouch,
                    ATTRS_DISMISS_ON_OUTSIDE_TOUCH_DEFAULT))
            setDismissFocusOnItemSelection(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_dismissFocusOnItemSelection,
                    ATTRS_DISMISS_FOCUS_ON_ITEM_SELECTION_DEFAULT))
            setSuggestionItemTextSize(a.getDimensionPixelSize(
                    R.styleable.FloatingSearchView_floatingSearch_searchSuggestionTextSize,
                    Util.spToPx(ATTRS_SUGGESTION_TEXT_SIZE_SP_DEFAULT)))

            mLeftActionMode = a.getInt(R.styleable.FloatingSearchView_floatingSearch_leftActionMode,
                    ATTRS_SEARCH_BAR_LEFT_ACTION_MODE_DEFAULT)
            if (a.hasValue(R.styleable.FloatingSearchView_floatingSearch_menu)) {
                mMenuId = a.getResourceId(R.styleable.FloatingSearchView_floatingSearch_menu, -1)
            }
            setDimBackground(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_dimBackground,
                    ATTRS_SHOW_DIM_BACKGROUND_DEFAULT))
            setShowMoveUpSuggestion(a.getBoolean(R.styleable.FloatingSearchView_floatingSearch_showMoveSuggestionUp,
                    ATTRS_SHOW_MOVE_UP_SUGGESTION_DEFAULT))
            this.mSuggestionSectionAnimDuration = a.getInt(R.styleable.FloatingSearchView_floatingSearch_suggestionsListAnimDuration,
                    ATTRS_SUGGESTION_ANIM_DURATION_DEFAULT).toLong()
            setBackgroundColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_backgroundColor, Util.getColor(context, R.color.background)))
            setLeftActionIconColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_leftActionColor, Util.getColor(context, R.color.left_action_icon)))
            setActionMenuOverflowColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_actionMenuOverflowColor, Util.getColor(context, R.color.overflow_icon_color)))
            setMenuItemIconColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_menuItemIconColor, Util.getColor(context, R.color.menu_icon_color)))
            setDividerColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_dividerColor, Util.getColor(context, R.color.divider)))
            setClearBtnColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_clearBtnColor, Util.getColor(context, R.color.clear_btn_color)))
            val viewTextColor = a.getColor(R.styleable.FloatingSearchView_floatingSearch_viewTextColor, Util.getColor(context, R.color.dark_gray))
            setViewTextColor(viewTextColor)
            setQueryTextColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_viewSearchInputTextColor, viewTextColor))
            setSuggestionsTextColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_viewSuggestionItemTextColor, viewTextColor))
            setHintTextColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_hintTextColor, Util.getColor(context, R.color.hint_color)))
            setSuggestionRightIconColor(a.getColor(R.styleable.FloatingSearchView_floatingSearch_suggestionRightIconColor, Util.getColor(context, R.color.gray_active_icon)))
        } finally {
            a.recycle()
        }
    }

    private fun setupQueryBar() {

        mSearchInput!!.setTextColor(mSearchInputTextColor)
        mSearchInput!!.setHintTextColor(mSearchInputHintColor)

        if (!isInEditMode && mHostActivity != null) {
            mHostActivity!!.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        }

        val vto = mQuerySection?.viewTreeObserver
        vto?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Util.removeGlobalLayoutObserver(mQuerySection!!, this)
                inflateOverflowMenu(mMenuId)
            }
        })

        mMenuView!!.setMenuCallback(@SuppressLint("RestrictedApi")
        object : Callback {
            override fun onMenuItemSelected(menu: MenuBuilder, item: MenuItem): Boolean {

                if (mActionMenuItemListener != null) {
                    mActionMenuItemListener!!.onActionMenuItemSelected(item)
                }

                //todo check if we should care about this return or not
                return false
            }

            override fun onMenuModeChange(menu: MenuBuilder) {}

        })

        mMenuView!!.setOnVisibleWidthChanged(object : MenuView.OnVisibleWidthChangedListener {
            override fun onItemsMenuVisibleWidthChanged(newVisibleWidth: Int) {
                handleOnVisibleMenuItemsWidthChanged(newVisibleWidth)
            }
        })

        mMenuView!!.setActionIconColor(mActionMenuItemColor)
        mMenuView!!.setOverflowColor(mOverflowIconColor)

        mClearButton!!.visibility = View.INVISIBLE
        mClearButton!!.setOnClickListener {
            mSearchInput!!.setText("")
            if (mOnClearSearchActionListener != null) {
                mOnClearSearchActionListener!!.onClearSearchClicked()
            }
        }

        mSearchInput!!.addTextChangedListener(object : TextWatcherAdapter() {

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                //todo investigate why this is called twice when pressing back on the keyboard

                if (mSkipTextChangeEvent || !isSearchBarFocused) {
                    mSkipTextChangeEvent = false
                } else {
                    if (mSearchInput?.text.toString().isNotEmpty() && mClearButton?.visibility == View.INVISIBLE) {
                        mClearButton?.alpha = 0.0f
                        mClearButton?.visibility = View.VISIBLE
                        ViewCompat.animate(mClearButton!!).alpha(1.0f).setDuration(CLEAR_BTN_FADE_ANIM_DURATION).start()
                    } else if (mSearchInput?.text?.toString()?.isEmpty()!!) {
                        mClearButton!!.visibility = View.INVISIBLE
                    }

                    if (mQueryListener != null && isSearchBarFocused && query != mSearchInput!!.text.toString()) {
                        mQueryListener!!.onSearchTextChanged(query, mSearchInput!!.text.toString())
                    }

                }

                query = mSearchInput!!.text.toString()
            }

        })

        mSearchInput?.setOnFocusChangeListener { _, hasFocus ->
            if (mSkipQueryFocusChangeEvent) {
                mSkipQueryFocusChangeEvent = false
            } else if (hasFocus != isSearchBarFocused) {
                setSearchFocusedInternal(hasFocus)
            }
        }

        mSearchInput?.setOnKeyboardDismissedListener(object : SearchInputView.OnKeyboardDismissedListener {
            override fun onKeyboardDismissed() {
                if (mCloseSearchOnSofteKeyboardDismiss) {
                    setSearchFocusedInternal(false)
                }
            }
        })

        mSearchInput?.setOnSearchKeyListener(object : SearchInputView.OnKeyboardSearchKeyClickListener {
            override fun onSearchKeyClicked() {
                if (mSearchListener != null) {
                    mSearchListener!!.onSearchAction(query)
                }
                mSkipTextChangeEvent = true
                mSkipTextChangeEvent = true
                if (mIsTitleSet) {
                    setSearchBarTitle(query)
                } else {
                    setSearchText(query)
                }
                setSearchFocusedInternal(false)
            }
        })

        mLeftAction?.setOnClickListener {
            if (isSearchBarFocused) {
                setSearchFocusedInternal(false)
            } else {
                when (mLeftActionMode) {
                    LEFT_ACTION_MODE_SHOW_HAMBURGER -> if (mLeftMenuClickListener != null) {
                        mLeftMenuClickListener.onClick(mLeftAction)
                    } else {
                        toggleLeftMenu()
                    }
                    LEFT_ACTION_MODE_SHOW_SEARCH -> setSearchFocusedInternal(true)
                    LEFT_ACTION_MODE_SHOW_HOME -> if (mOnHomeActionClickListener != null) {
                        mOnHomeActionClickListener!!.onHomeClicked()
                    }
                    LEFT_ACTION_MODE_NO_LEFT_ACTION -> {
                    }
                }//do nothing
            }
        }

        refreshLeftIcon()
    }

    //ensures that the end margin of the search input is according to Material specs
    private fun handleOnVisibleMenuItemsWidthChanged(menuItemsWidth: Int) {
        if (menuItemsWidth == 0) {
            mClearButton!!.translationX = (-dpToPx(4)).toFloat()

            var paddingRight = dpToPx(4)
            paddingRight += if (isSearchBarFocused) dpToPx(CLEAR_BTN_WIDTH_DP) else dpToPx(14)

            mSearchInput!!.setPadding(0, 0, paddingRight, 0)
        } else {
            mClearButton!!.translationX = (-menuItemsWidth).toFloat()
            var paddingRight = menuItemsWidth
            if (isSearchBarFocused) {
                paddingRight += dpToPx(CLEAR_BTN_WIDTH_DP)
            }
            mSearchInput!!.setPadding(0, 0, paddingRight, 0)
        }
    }

    /**
     * Sets the menu button's color.
     *
     * @param color the color to be applied to the
     * left menu button.
     */
    fun setLeftActionIconColor(color: Int) {
        mLeftActionIconColor = color
        mMenuBtnDrawable!!.color = color
        DrawableCompat.setTint(mIconBackArrow!!, color)
        DrawableCompat.setTint(mIconSearch!!, color)
    }

    /**
     * If set, the left menu won't open or close and the client is assumed to handle its
     * clicks.
     *
     * @param onMenuClickListener
     */
    fun setOnMenuClickListener(onMenuClickListener: OnLeftMenuClickListener) {
        mOnMenuClickListener = onMenuClickListener
    }

    /**
     * Sets the clear button's color.
     *
     * @param color the color to be applied to the
     * clear button.
     */
    fun setClearBtnColor(color: Int) {
        mClearBtnColor = color
        DrawableCompat.setTint(mIconClear!!, mClearBtnColor)
    }

    /**
     * Sets the action menu icons' color.
     *
     * @param color the color to be applied to the
     * action menu items.
     */
    fun setMenuItemIconColor(color: Int) {
        this.mActionMenuItemColor = color
        if (mMenuView != null) {
            mMenuView!!.setActionIconColor(this.mActionMenuItemColor)
        }
    }

    /**
     * Sets the action menu overflow icon's color.
     *
     * @param color the color to be applied to the
     * overflow icon.
     */
    fun setActionMenuOverflowColor(color: Int) {
        this.mOverflowIconColor = color
        if (mMenuView != null) {
            mMenuView!!.setOverflowColor(this.mOverflowIconColor)
        }
    }

    /**
     * Sets the background color of the search
     * view including the suggestions section.
     *
     * @param color the color to be applied to the search bar and
     * the suggestion section background.
     */
    override fun setBackgroundColor(color: Int) {
        mBackgroundColor = color
        if (mQuerySection != null && mSuggestionsList != null) {
            mQuerySection!!.setCardBackgroundColor(color)
            mSuggestionsList!!.setBackgroundColor(color)
        }
    }

    /**
     * Sets the text color of the search
     * and suggestion text.
     *
     * @param color the color to be applied to the search and suggestion
     * text.
     */
    fun setViewTextColor(color: Int) {
        setSuggestionsTextColor(color)
        setQueryTextColor(color)
    }

    /**
     * Sets whether the search will lose focus when a suggestion item is clicked.
     *
     * @param dismissFocusOnItemSelection
     */
    fun setDismissFocusOnItemSelection(dismissFocusOnItemSelection: Boolean) {
        mDismissFocusOnItemSelection = dismissFocusOnItemSelection
    }

    /**
     * Sets the text color of suggestion text.
     *
     * @param color
     */
    fun setSuggestionsTextColor(color: Int) {
        mSuggestionTextColor = color
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter!!.setTextColor(mSuggestionTextColor)
        }
    }

    /**
     * Set the duration for the suggestions list expand/collapse
     * animation.
     *
     * @param duration
     */
    fun setSuggestionsAnimDuration(duration: Long) {
        this.mSuggestionSectionAnimDuration = duration
    }

    /**
     * Sets the text color of the search text.
     *
     * @param color
     */
    fun setQueryTextColor(color: Int) {
        mSearchInputTextColor = color
        if (mSearchInput != null) {
            mSearchInput!!.setTextColor(mSearchInputTextColor)
        }
    }

    /**
     * Set the text size of the text in the search box.
     *
     * @param sizePx
     */
    fun setQueryTextSize(sizePx: Int) {
        mQueryTextSize = sizePx
        mSearchInput!!.textSize = mQueryTextSize.toFloat()
    }

    /**
     * Sets the text color of the search
     * hint.
     *
     * @param color the color to be applied to the search hint.
     */
    fun setHintTextColor(color: Int) {
        mSearchInputHintColor = color
        if (mSearchInput != null) {
            mSearchInput!!.setHintTextColor(color)
        }
    }

    /**
     * Sets the color of the search divider that
     * divides the search section from the suggestions.
     *
     * @param color the color to be applied the divider.
     */
    fun setDividerColor(color: Int) {
        mDividerColor = color
        if (mDivider != null) {
            mDivider!!.setBackgroundColor(mDividerColor)
        }
    }

    /**
     * Set the tint of the suggestion items' right btn (move suggestion to
     * query)
     *
     * @param color
     */
    fun setSuggestionRightIconColor(color: Int) {
        this.mSuggestionRightIconColor = color
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter!!.setRightIconColor(this.mSuggestionRightIconColor)
        }
    }

    /**
     * Set the text size of the suggestion items.
     *
     * @param sizePx
     */
    private fun setSuggestionItemTextSize(sizePx: Int) {
        //todo implement dynamic suggestionTextSize setter and expose method
        this.mSuggestionsTextSizePx = sizePx
    }

    /**
     * Set the mode for the left action button.
     *
     * @param mode
     */
    fun setLeftActionMode(@LeftActionMode mode: Int) {
        mLeftActionMode = mode
        refreshLeftIcon()
    }

    private fun refreshLeftIcon() {
        val leftActionWidthAndMarginLeft = dpToPx(LEFT_MENU_WIDTH_AND_MARGIN_START_DP)
        var queryTranslationX = 0

        mLeftAction!!.visibility = View.VISIBLE
        when (mLeftActionMode) {
            LEFT_ACTION_MODE_SHOW_HAMBURGER -> {
                mLeftAction!!.setImageDrawable(mMenuBtnDrawable)
                mMenuBtnDrawable!!.progress = MENU_BUTTON_PROGRESS_HAMBURGER
            }
            LEFT_ACTION_MODE_SHOW_SEARCH -> mLeftAction!!.setImageDrawable(mIconSearch)
            LEFT_ACTION_MODE_SHOW_HOME -> {
                mLeftAction!!.setImageDrawable(mMenuBtnDrawable)
                mMenuBtnDrawable!!.progress = MENU_BUTTON_PROGRESS_ARROW
            }
            LEFT_ACTION_MODE_NO_LEFT_ACTION -> {
                mLeftAction!!.visibility = View.INVISIBLE
                queryTranslationX = -leftActionWidthAndMarginLeft
            }
        }
        mSearchInputParent!!.translationX = queryTranslationX.toFloat()
    }

    private fun toggleLeftMenu() {
        if (mMenuOpen) {
            closeMenu(true)
        } else {
            openMenu(true)
        }
    }

    /**
     *
     *
     * Enables clients to directly manipulate
     * the menu icon's progress.
     *
     *
     * Useful for custom animation/behaviors.
     *
     * @param progress the desired progress of the menu
     * icon's rotation: 0.0 == hamburger
     * shape, 1.0 == back arrow shape
     */
    fun setMenuIconProgress(progress: Float) {
        mMenuBtnDrawable!!.progress = progress
        if (progress == 0f) {
            closeMenu(false)
        } else if (progress.toDouble() == 1.0) {
            openMenu(false)
        }
    }

    /**
     * Mimics a menu click that opens the menu. Useful for navigation
     * drawers when they open as a result of dragging.
     */
    fun openMenu(withAnim: Boolean) {
        mMenuOpen = true
        openMenuDrawable(mMenuBtnDrawable, withAnim)
        if (mOnMenuClickListener != null) {
            mOnMenuClickListener!!.onMenuOpened()
        }
    }

    /**
     * Mimics a menu click that closes. Useful when fo navigation
     * drawers when they close as a result of selecting and item.
     *
     * @param withAnim true, will close the menu button with
     * the  Material animation
     */
    fun closeMenu(withAnim: Boolean) {
        mMenuOpen = false
        closeMenuDrawable(mMenuBtnDrawable, withAnim)
        if (mOnMenuClickListener != null) {
            mOnMenuClickListener!!.onMenuClosed()
        }
    }

    /**
     * Set the hamburger menu to open or closed without
     * animating hamburger to arrow and without calling listeners.
     *
     * @param isOpen
     */
    fun setLeftMenuOpen(isOpen: Boolean) {
        mMenuOpen = isOpen
        mMenuBtnDrawable!!.progress = if (isOpen) 1.0f else 0.0f
    }

    /**
     * Shows a circular progress on top of the
     * menu action button.
     *
     *
     * Call hidProgress()
     * to change back to normal and make the menu
     * action visible.
     */
    @SuppressLint("ObjectAnimatorBinding")
    fun showProgress() {
        mLeftAction!!.visibility = View.GONE
        mSearchProgress!!.alpha = 0.0f
        mSearchProgress!!.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(mSearchProgress, "alpha", 0.0f, 1.0f).start()
    }

    /**
     * Hides the progress bar after
     * a prior call to showProgress()
     */
    @SuppressLint("ObjectAnimatorBinding")
    fun hideProgress() {
        mSearchProgress!!.visibility = View.GONE
        mLeftAction!!.alpha = 0.0f
        mLeftAction!!.visibility = View.VISIBLE
        ObjectAnimator.ofFloat(mLeftAction, "alpha", 0.0f, 1.0f).start()
    }

    /**
     * Inflates the menu items from
     * an xml resource.
     *
     * @param menuId a menu xml resource reference
     */
    fun inflateOverflowMenu(menuId: Int) {
        mMenuId = menuId
        mMenuView!!.reset(menuId, actionMenuAvailWidth())
        if (isSearchBarFocused) {
            mMenuView!!.hideIfRoomItems(false)
        }
    }

    private fun actionMenuAvailWidth(): Int {
        return if (isInEditMode) {
            mQuerySection!!.measuredWidth / 2
        } else mQuerySection!!.width / 2
    }

    /**
     * Set a hint that will appear in the
     * search input. Default hint is R.string.abc_search_hint
     * which is "search..." (when device language is set to english)
     *
     * @param searchHint
     */
    @SuppressLint("PrivateResource")
    fun setSearchHint(searchHint: String?) {
        mSearchHint = searchHint ?: resources.getString(R.string.abc_search_hint)
        mSearchInput!!.hint = mSearchHint
    }

    /**
     * Sets whether the the button with the search icon
     * will appear in the soft-keyboard or not.
     *
     * @param show to show the search button in
     * the soft-keyboard.
     */
    fun setShowSearchKey(show: Boolean) {
        mShowSearchKey = show
        if (show) {
            mSearchInput!!.imeOptions = EditorInfo.IME_ACTION_SEARCH
        } else {
            mSearchInput!!.imeOptions = EditorInfo.IME_ACTION_NONE
        }
    }


    /**
     * Sets whether the search will lose focus when the softkeyboard
     * gets closed from a back press
     *
     * @param closeSearchOnKeyboardDismiss
     */
    fun setCloseSearchOnKeyboardDismiss(closeSearchOnKeyboardDismiss: Boolean) {
        this.mCloseSearchOnSofteKeyboardDismiss = closeSearchOnKeyboardDismiss
    }

    /**
     * Set whether a touch outside of the
     * search bar's bounds will cause the search bar to
     * loos focus.
     *
     * @param enable true to dismiss on outside touch, false otherwise.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun setDismissOnOutsideClick(enable: Boolean) {

        mDismissOnOutsideTouch = enable
        mSuggestionsSection?.setOnTouchListener { _, _ ->
            //todo check if this is called twice
            if (mDismissOnOutsideTouch && isSearchBarFocused) {
                setSearchFocusedInternal(false)
            }

            true
        }
    }

    /**
     * Sets whether a dim background will show when the search is focused
     *
     * @param dimEnabled True to show dim
     */
    fun setDimBackground(dimEnabled: Boolean) {
        this.mDimBackground = dimEnabled
        refreshDimBackground()
    }

    private fun refreshDimBackground() {
        if (this.mDimBackground && isSearchBarFocused) {
            mBackgroundDrawable!!.alpha = BACKGROUND_DRAWABLE_ALPHA_SEARCH_FOCUSED
        } else {
            mBackgroundDrawable!!.alpha = BACKGROUND_DRAWABLE_ALPHA_SEARCH_NOT_FOCUSED
        }
    }

    /**
     * Sets the arrow up of suggestion items to be enabled and visible or
     * disabled and invisible.
     *
     * @param show
     */
    fun setShowMoveUpSuggestion(show: Boolean) {
        mShowMoveUpSuggestion = show
        refreshShowMoveUpSuggestion()
    }

    private fun refreshShowMoveUpSuggestion() {
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter!!.setShowMoveUpIcon(mShowMoveUpSuggestion)
        }
    }

    /**
     * Wrapper implementation for EditText.setFocusable(boolean focusable)
     *
     * @param focusable true, to make search focus when
     * clicked.
     */
    fun setSearchFocusable(focusable: Boolean) {
        mSearchInput!!.isFocusable = focusable
        mSearchInput!!.isFocusableInTouchMode = focusable
    }

    /**
     * Sets the title for the search bar.
     *
     *
     * Note that after the title is set, when
     * the search gains focus, the title will be replaced
     * by the search hint.
     *
     * @param title the title to be shown when search
     * is not focused
     */
    fun setSearchBarTitle(title: CharSequence) {
        this.mTitleText = title.toString()
        mIsTitleSet = true
        mSearchInput!!.setText(title)
    }

    /**
     * Sets the search text.
     *
     *
     * Note that this is the different from
     * [setSearchBarTitle][.setSearchBarTitle] in
     * that it keeps the text when the search gains focus.
     *
     * @param text the text to be set for the search
     * input.
     */
    fun setSearchText(text: CharSequence) {
        mIsTitleSet = false
        setQueryText(text)
    }

    fun clearQuery() {
        mSearchInput!!.setText("")
    }

    /**
     * Sets whether the search is focused or not.
     *
     * @param focused true, to set the search to be active/focused.
     * @return true if the search was focused and will now become not focused. Useful for
     * calling supper.onBackPress() in the hosting activity only if this method returns false
     */
    fun setSearchFocused(focused: Boolean): Boolean {

        val updatedToNotFocused = !focused && this.isSearchBarFocused

        if (focused != this.isSearchBarFocused && mSuggestionSecHeightListener == null) {
            if (mIsSuggestionsSectionHeightSet) {
                setSearchFocusedInternal(focused)
            } else {
                mSuggestionSecHeightListener = object : OnSuggestionSecHeightSetListener {
                    override fun onSuggestionSecHeightSet() {
                        setSearchFocusedInternal(focused)
                        mSuggestionSecHeightListener = null
                    }
                }
            }
        }
        return updatedToNotFocused
    }

    private fun setupSuggestionSection() {

        val layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        mSuggestionsList!!.layoutManager = layoutManager
        mSuggestionsList!!.itemAnimator = null

        val gestureDetector = GestureDetector(context,
                object : GestureDetectorListenerAdapter() {

                    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                        if (mHostActivity != null) {
                            Util.closeSoftKeyboard(mHostActivity!!)
                        }
                        return false
                    }
                })
        mSuggestionsList!!.addOnItemTouchListener(object : OnItemTouchListenerAdapter() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                gestureDetector.onTouchEvent(e)
                return false
            }
        })

        mSuggestionsAdapter = SearchSuggestionsAdapter(context, mSuggestionsTextSizePx,
                object : SearchSuggestionsAdapter.Listener {

                    override fun onItemSelected(item: SearchSuggestion) {
                        if (mSearchListener != null) {
                            mSearchListener!!.onSuggestionClicked(item)
                        }

                        if (mDismissFocusOnItemSelection) {
                            isSearchBarFocused = false

                            mSkipTextChangeEvent = true
                            if (mIsTitleSet) {
                                setSearchBarTitle(item.body)
                            } else {
                                setSearchText(item.body)
                            }

                            setSearchFocusedInternal(false)
                        }
                    }

                    override fun onMoveItemToSearchClicked(item: SearchSuggestion) {

                        setQueryText(item.body)
                    }
                })
        refreshShowMoveUpSuggestion()
        mSuggestionsAdapter!!.setTextColor(this.mSuggestionTextColor)
        mSuggestionsAdapter!!.setRightIconColor(this.mSuggestionRightIconColor)

        mSuggestionsList!!.adapter = mSuggestionsAdapter

        val cardViewBottomPadding = dpToPx(CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT)
        //move up the suggestions section enough to cover the search bar
        //card's bottom left and right corners
        mSuggestionsSection!!.translationY = (-cardViewBottomPadding).toFloat()
    }

    private fun setQueryText(text: CharSequence) {
        mSearchInput!!.setText(text)
        //move cursor to end of text
        mSearchInput!!.setSelection(mSearchInput!!.text.length)
    }

    private fun moveSuggestListToInitialPos() {
        //move the suggestions list to the collapsed position
        //which is translationY of -listContainerHeight
        mSuggestionListContainer!!.translationY = (-mSuggestionListContainer!!.height).toFloat()
    }

    /**
     * Clears the current suggestions and replaces it
     * with the provided list of new suggestions.
     *
     * @param newSearchSuggestions a list containing the new suggestions
     */
    fun swapSuggestions(newSearchSuggestions: List<SearchSuggestion>) {
        swapSuggestions(newSearchSuggestions, true)
    }

    private fun swapSuggestions(newSearchSuggestions: List<SearchSuggestion>,
                                withAnim: Boolean) {

        mSuggestionsList?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                Util.removeGlobalLayoutObserver(mSuggestionsList!!, this)
                val isSuggestionItemsFillRecyclerView = updateSuggestionsSectionHeight(newSearchSuggestions, withAnim)

                //we only need to employ the reverse layout technique if the items don't fill up the RecyclerView
                val suggestionsListLm = mSuggestionsList?.layoutManager as LinearLayoutManager?
                if (isSuggestionItemsFillRecyclerView) {
                    suggestionsListLm?.reverseLayout = false
                } else {
                    mSuggestionsAdapter?.reverseList()
                    suggestionsListLm?.reverseLayout = true
                }
                mSuggestionsList?.alpha = 1f
            }
        })
        mSuggestionsList?.adapter = mSuggestionsAdapter//workaround to avoid list retaining scroll pos
        mSuggestionsList?.alpha = 0f
        mSuggestionsAdapter?.swapData(newSearchSuggestions)

        mDivider!!.visibility = if (newSearchSuggestions.isNotEmpty()) View.VISIBLE else View.GONE
    }

    //returns true if the suggestion items occupy the full RecyclerView's height, false otherwise
    private fun updateSuggestionsSectionHeight(newSearchSuggestions: List<SearchSuggestion>, withAnim: Boolean): Boolean {

        val cardTopBottomShadowPadding = dpToPx(CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT)
        val cardRadiusSize = dpToPx(CARD_VIEW_TOP_BOTTOM_SHADOW_HEIGHT)

        val visibleSuggestionHeight = calculateSuggestionItemsHeight(newSearchSuggestions,
                mSuggestionListContainer!!.height)
        val diff = mSuggestionListContainer!!.height - visibleSuggestionHeight

        val addedTranslationYForShadowOffsets = when {
            diff <= cardTopBottomShadowPadding -> -(cardTopBottomShadowPadding - diff)
            diff < mSuggestionListContainer!!.height - cardTopBottomShadowPadding -> cardRadiusSize
            else -> 0
        }

        val newTranslationY = (-mSuggestionListContainer!!.height +
                visibleSuggestionHeight + addedTranslationYForShadowOffsets).toFloat()

        //todo go over
        val fullyInvisibleTranslationY = (-mSuggestionListContainer!!.height + cardRadiusSize).toFloat()

        ViewCompat.animate(mSuggestionListContainer!!).cancel()
        if (withAnim) {
            ViewCompat.animate(mSuggestionListContainer!!).setInterpolator(SUGGEST_ITEM_ADD_ANIM_INTERPOLATOR).setDuration(mSuggestionSectionAnimDuration).translationY(newTranslationY)
                    .setUpdateListener { view ->
                        if (mOnSuggestionsListHeightChanged != null) {
                            val newSuggestionsHeight = abs(view.translationY - fullyInvisibleTranslationY)
                            mOnSuggestionsListHeightChanged!!.onSuggestionsListHeightChanged(newSuggestionsHeight)
                        }
                    }
                    .setListener(object : ViewPropertyAnimatorListenerAdapter() {
                        override fun onAnimationCancel(view: View?) {
                            mSuggestionListContainer!!.translationY = newTranslationY
                        }
                    }).start()
        } else {
            mSuggestionListContainer!!.translationY = newTranslationY
            if (mOnSuggestionsListHeightChanged != null) {
                val newSuggestionsHeight = abs(mSuggestionListContainer!!.translationY - fullyInvisibleTranslationY)
                mOnSuggestionsListHeightChanged!!.onSuggestionsListHeightChanged(newSuggestionsHeight)
            }
        }

        return mSuggestionListContainer!!.height == visibleSuggestionHeight
    }

    //returns the cumulative height that the current suggestion items take up or the given max if the
    //results is >= max. The max option allows us to avoid doing unnecessary and potentially long calculations.
    private fun calculateSuggestionItemsHeight(suggestions: List<SearchSuggestion>, max: Int): Int {

        //todo
        // 'i < suggestions.size()' in the below 'for' seems unneeded, investigate if there is a use for it.
        var visibleItemsHeight = 0
        var i = 0
        while (i < suggestions.size && i < mSuggestionsList!!.childCount) {
            visibleItemsHeight += mSuggestionsList!!.getChildAt(i).height
            if (visibleItemsHeight > max) {
                visibleItemsHeight = max
                break
            }
            i++
        }
        return visibleItemsHeight
    }

    /**
     * Set a callback that will be called after each suggestion view in the suggestions recycler
     * list is bound. This allows for customized binding for specific items in the list.
     *
     * @param callback A callback to be called after a suggestion is bound by the suggestions list's
     * adapter.
     */
    fun setOnBindSuggestionCallback(callback: SearchSuggestionsAdapter.OnBindSuggestionCallback) {
        this.mOnBindSuggestionCallback = callback
        if (mSuggestionsAdapter != null) {
            mSuggestionsAdapter!!.setOnBindSuggestionCallback(mOnBindSuggestionCallback!!)
        }
    }

    /**
     * Collapses the suggestions list and
     * then clears its suggestion items.
     */
    fun clearSuggestions() {
        swapSuggestions(ArrayList())
    }

    fun clearSearchFocus() {
        setSearchFocusedInternal(false)
    }

    private fun setSearchFocusedInternal(focused: Boolean) {
        this.isSearchBarFocused = focused

        if (focused) {
            mSearchInput!!.requestFocus()
            moveSuggestListToInitialPos()
            mSuggestionsSection!!.visibility = View.VISIBLE
            if (mDimBackground) {
                fadeInBackground()
            }
            handleOnVisibleMenuItemsWidthChanged(0)//this must be called before  mMenuView.hideIfRoomItems(...)
            mMenuView!!.hideIfRoomItems(true)
            transitionInLeftSection(true)
            Util.showSoftKeyboard(context, mSearchInput!!)
            if (mMenuOpen) {
                closeMenu(false)
            }
            if (mIsTitleSet) {
                mSkipTextChangeEvent = true
                mSearchInput!!.setText("")
            } else {
                mSearchInput!!.setSelection(mSearchInput?.text?.length!!)
            }
            mSearchInput!!.isLongClickable = true
            mClearButton!!.visibility = if (mSearchInput?.text?.toString()?.isEmpty()!!)
                View.INVISIBLE
            else
                View.VISIBLE
            if (mFocusChangeListener != null) {
                mFocusChangeListener!!.onFocus()
            }
        } else {
            mMainLayout!!.requestFocus()
            clearSuggestions()
            if (mDimBackground) {
                fadeOutBackground()
            }
            handleOnVisibleMenuItemsWidthChanged(0)//this must be called before  mMenuView.hideIfRoomItems(...)
            mMenuView!!.showIfRoomItems(true)
            transitionOutLeftSection(true)
            mClearButton!!.visibility = View.GONE
            if (mHostActivity != null) {
                Util.closeSoftKeyboard(mHostActivity!!)
            }
            if (mIsTitleSet) {
                mSkipTextChangeEvent = true
                mSearchInput!!.setText(mTitleText)
            }
            mSearchInput!!.isLongClickable = false
            if (mFocusChangeListener != null) {
                mFocusChangeListener!!.onFocusCleared()
            }
        }

        //if we don't have focus, we want to allow the client's views below our invisible
        //screen-covering view to handle touches
        mSuggestionsSection!!.isEnabled = focused
    }

    private fun changeIcon(imageView: ImageView, newIcon: Drawable?, withAnim: Boolean) {
        imageView.setImageDrawable(newIcon)
        if (withAnim) {
            val fadeInVoiceInputOrClear = ObjectAnimator.ofFloat(imageView, "alpha", 0.0f, 1.0f)
            fadeInVoiceInputOrClear.start()
        } else {
            imageView.alpha = 1.0f
        }
    }

    private fun transitionInLeftSection(withAnim: Boolean) {

        if (mSearchProgress!!.visibility != View.VISIBLE) {
            mLeftAction!!.visibility = View.VISIBLE
        } else {
            mLeftAction!!.visibility = View.INVISIBLE
        }

        when (mLeftActionMode) {
            LEFT_ACTION_MODE_SHOW_HAMBURGER -> {
                openMenuDrawable(mMenuBtnDrawable, withAnim)
                if (!mMenuOpen) return
            }
            LEFT_ACTION_MODE_SHOW_SEARCH -> {
                mLeftAction!!.setImageDrawable(mIconBackArrow)
                if (withAnim) {
                    mLeftAction!!.rotation = 45f
                    mLeftAction!!.alpha = 0.0f
                    val rotateAnim = ViewPropertyObjectAnimator.animate(mLeftAction).rotation(0f).get()
                    val fadeAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(1.0f).get()
                    val animSet = AnimatorSet()
                    animSet.duration = 500
                    animSet.playTogether(rotateAnim, fadeAnim)
                    animSet.start()
                }
            }
            LEFT_ACTION_MODE_SHOW_HOME -> {
            }
            LEFT_ACTION_MODE_NO_LEFT_ACTION -> {
                mLeftAction!!.setImageDrawable(mIconBackArrow)

                if (withAnim) {
                    val searchInputTransXAnim = ViewPropertyObjectAnimator
                            .animate(mSearchInputParent).translationX(0f).get()

                    mLeftAction!!.scaleX = 0.5f
                    mLeftAction!!.scaleY = 0.5f
                    mLeftAction!!.alpha = 0.0f
                    mLeftAction!!.translationX = dpToPx(8).toFloat()
                    val transXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).translationX(1.0f).get()
                    val scaleXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleX(1.0f).get()
                    val scaleYArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleY(1.0f).get()
                    val fadeArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(1.0f).get()
                    transXArrowAnim.startDelay = 150
                    scaleXArrowAnim.startDelay = 150
                    scaleYArrowAnim.startDelay = 150
                    fadeArrowAnim.startDelay = 150

                    val animSet = AnimatorSet()
                    animSet.duration = 500
                    animSet.playTogether(searchInputTransXAnim, transXArrowAnim, scaleXArrowAnim, scaleYArrowAnim, fadeArrowAnim)
                    animSet.start()
                } else {
                    mSearchInputParent!!.translationX = 0f
                }
            }
        }//do nothing
    }

    private fun transitionOutLeftSection(withAnim: Boolean) {

        when (mLeftActionMode) {
            LEFT_ACTION_MODE_SHOW_HAMBURGER -> closeMenuDrawable(mMenuBtnDrawable, withAnim)
            LEFT_ACTION_MODE_SHOW_SEARCH -> changeIcon(mLeftAction!!, mIconSearch, withAnim)
            LEFT_ACTION_MODE_SHOW_HOME -> {
            }
            LEFT_ACTION_MODE_NO_LEFT_ACTION -> {
                mLeftAction?.setImageDrawable(mIconBackArrow)

                if (withAnim) {
                    val searchInputTransXAnim = ViewPropertyObjectAnimator.animate(mSearchInputParent)
                            .translationX((-dpToPx(LEFT_MENU_WIDTH_AND_MARGIN_START_DP)).toFloat()).get()

                    val scaleXArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleX(0.5f).get()
                    val scaleYArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).scaleY(0.5f).get()
                    val fadeArrowAnim = ViewPropertyObjectAnimator.animate(mLeftAction).alpha(0.5f).get()
                    scaleXArrowAnim.duration = 300
                    scaleYArrowAnim.duration = 300
                    fadeArrowAnim.duration = 300
                    scaleXArrowAnim.addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            //restore normal state
                            mLeftAction?.scaleX = 1.0f
                            mLeftAction?.scaleY = 1.0f
                            mLeftAction?.alpha = 1.0f
                            mLeftAction?.visibility = View.INVISIBLE
                        }
                    })

                    val animSet = AnimatorSet()
                    animSet.duration = 350
                    animSet.playTogether(scaleXArrowAnim, scaleYArrowAnim, fadeArrowAnim, searchInputTransXAnim)
                    animSet.start()
                } else {
                    mLeftAction!!.visibility = View.INVISIBLE
                }
            }
        }//do nothing
    }

    /**
     * Sets the listener that will be notified when the suggestion list's height
     * changes.
     *
     * @param onSuggestionsListHeightChanged the new suggestions list's height
     */
    fun setOnSuggestionsListHeightChanged(onSuggestionsListHeightChanged: OnSuggestionsListHeightChanged) {
        this.mOnSuggestionsListHeightChanged = onSuggestionsListHeightChanged
    }

    /**
     * Sets the listener that will listen for query
     * changes as they are being typed.
     *
     * @param listener listener for query changes
     */
    fun setOnQueryChangeListener(listener: OnQueryChangeListener) {
        this.mQueryListener = listener
    }

    /**
     * Sets the listener that will be called when
     * an action that completes the current search
     * session has occurred and the search lost focus.
     *
     *
     *
     * When called, a client would ideally grab the
     * search or suggestion query from the callback parameter or
     * from [getquery][.getQuery] and perform the necessary
     * query against its data source.
     *
     * @param listener listener for query completion
     */
    fun setOnSearchListener(listener: OnSearchListener) {
        this.mSearchListener = listener
    }

    /**
     * Sets the listener that will be called when the focus
     * of the search has changed.
     *
     * @param listener listener for search focus changes
     */
    fun setOnFocusChangeListener(listener: OnFocusChangeListener) {
        this.mFocusChangeListener = listener
    }

    /**
     * Sets the listener that will be called when the
     * left/start menu (or navigation menu) is clicked.
     *
     *
     *
     * Note that this is different from the overflow menu
     * that has a separate listener.
     *
     * @param listener
     */
    fun setOnLeftMenuClickListener(listener: OnLeftMenuClickListener?) {
        this.mOnMenuClickListener = listener
    }

    /**
     * Sets the listener that will be called when the
     * left/start home action (back arrow) is clicked.
     *
     * @param listener
     */
    fun setOnHomeActionClickListener(listener: OnHomeActionClickListener) {
        this.mOnHomeActionClickListener = listener
    }

    /**
     * Sets the listener that will be called when
     * an item in the overflow menu is clicked.
     *
     * @param listener listener to listen to menu item clicks
     */
    fun setOnMenuItemClickListener(listener: OnMenuItemClickListener) {
        this.mActionMenuItemListener = listener
        //todo reset menu view listener
    }

    /**
     * Sets the listener that will be called when the
     * clear search text action button (the x to the right
     * of the search text) is clicked.
     *
     * @param listener
     */
    fun setOnClearSearchActionListener(listener: OnClearSearchActionListener) {
        this.mOnClearSearchActionListener = listener
    }

    private fun openMenuDrawable(drawerArrowDrawable: DrawerArrowDrawable?, withAnim: Boolean) {
        if (withAnim) {
            val anim = ValueAnimator.ofFloat(0.0f, 1.0f)
            anim.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                drawerArrowDrawable!!.progress = value
            }
            anim.duration = MENU_ICON_ANIM_DURATION.toLong()
            anim.start()
        } else {
            drawerArrowDrawable!!.progress = 1.0f
        }
    }

    private fun closeMenuDrawable(drawerArrowDrawable: DrawerArrowDrawable?, withAnim: Boolean) {
        if (withAnim) {
            val anim = ValueAnimator.ofFloat(1.0f, 0.0f)
            anim.addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                drawerArrowDrawable!!.progress = value
            }
            anim.duration = MENU_ICON_ANIM_DURATION.toLong()
            anim.start()
        } else {
            drawerArrowDrawable!!.progress = 0.0f
        }
    }

    private fun fadeOutBackground() {
        val anim = ValueAnimator.ofInt(BACKGROUND_DRAWABLE_ALPHA_SEARCH_FOCUSED, BACKGROUND_DRAWABLE_ALPHA_SEARCH_NOT_FOCUSED)
        anim.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            mBackgroundDrawable!!.alpha = value
        }
        anim.duration = BACKGROUND_FADE_ANIM_DURATION.toLong()
        anim.start()
    }

    private fun fadeInBackground() {
        val anim = ValueAnimator.ofInt(BACKGROUND_DRAWABLE_ALPHA_SEARCH_NOT_FOCUSED, BACKGROUND_DRAWABLE_ALPHA_SEARCH_FOCUSED)
        anim.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            mBackgroundDrawable!!.alpha = value
        }
        anim.duration = BACKGROUND_FADE_ANIM_DURATION.toLong()
        anim.start()
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = superState?.let { SavedState(it) }
        savedState?.let {
            it.suggestions = mSuggestionsAdapter?.dataSet as ArrayList<SearchSuggestion>
            savedState.isFocused = isSearchBarFocused
            savedState.query = query
            savedState.suggestionTextSize = mSuggestionsTextSizePx
            savedState.searchHint = mSearchHint.toString()
            savedState.dismissOnOutsideClick = mDismissOnOutsideTouch
            savedState.showMoveSuggestionUpBtn = mShowMoveUpSuggestion
            savedState.showSearchKey = mShowSearchKey
            savedState.isTitleSet = mIsTitleSet
            savedState.backgroundColor = mBackgroundColor
            savedState.suggestionsTextColor = mSuggestionTextColor
            savedState.queryTextColor = mSearchInputTextColor
            savedState.searchHintTextColor = mSearchInputHintColor
            savedState.actionOverflowMenuColor = mOverflowIconColor
            savedState.menuItemIconColor = mActionMenuItemColor
            savedState.leftIconColor = mLeftActionIconColor
            savedState.clearBtnColor = mClearBtnColor
            savedState.suggestionUpBtnColor = mSuggestionTextColor
            savedState.dividerColor = mDividerColor
            savedState.menuId = mMenuId
            savedState.leftActionMode = mLeftActionMode
            savedState.queryTextSize = mQueryTextSize
            savedState.dimBackground = mDimBackground
            savedState.dismissOnSoftKeyboardDismiss = mDismissOnOutsideTouch
            savedState.dismissFocusOnSuggestionItemClick = mDismissFocusOnItemSelection
        }
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        val savedState = state as SavedState
        super.onRestoreInstanceState(savedState.superState)
        isSearchBarFocused = savedState.isFocused
        mIsTitleSet = savedState.isTitleSet
        mMenuId = savedState.menuId
        query = savedState.query
        setSearchText(query)
        mSuggestionSectionAnimDuration = savedState.suggestionsSectionAnimSuration
        setSuggestionItemTextSize(savedState.suggestionTextSize)
        setDismissOnOutsideClick(savedState.dismissOnOutsideClick)
        setShowMoveUpSuggestion(savedState.showMoveSuggestionUpBtn)
        setShowSearchKey(savedState.showSearchKey)
        setSearchHint(savedState.searchHint)
        setBackgroundColor(savedState.backgroundColor)
        setSuggestionsTextColor(savedState.suggestionsTextColor)
        setQueryTextColor(savedState.queryTextColor)
        setQueryTextSize(savedState.queryTextSize)
        setHintTextColor(savedState.searchHintTextColor)
        setActionMenuOverflowColor(savedState.actionOverflowMenuColor)
        setMenuItemIconColor(savedState.menuItemIconColor)
        setLeftActionIconColor(savedState.leftIconColor)
        setClearBtnColor(savedState.clearBtnColor)
        setSuggestionRightIconColor(savedState.suggestionUpBtnColor)
        setDividerColor(savedState.dividerColor)
        setLeftActionMode(savedState.leftActionMode)
        setDimBackground(savedState.dimBackground)
        setCloseSearchOnKeyboardDismiss(savedState.dismissOnSoftKeyboardDismiss)
        setDismissFocusOnItemSelection(savedState.dismissFocusOnSuggestionItemClick)

        mSuggestionsSection!!.isEnabled = isSearchBarFocused
        if (isSearchBarFocused) {

            mBackgroundDrawable!!.alpha = BACKGROUND_DRAWABLE_ALPHA_SEARCH_FOCUSED
            mSkipTextChangeEvent = true
            mSkipQueryFocusChangeEvent = true

            mSuggestionsSection!!.visibility = View.VISIBLE

            //restore suggestions list when suggestion section's height is fully set
            mSuggestionSecHeightListener = object : OnSuggestionSecHeightSetListener {
                override fun onSuggestionSecHeightSet() {
                    swapSuggestions(savedState.suggestions, false)
                    mSuggestionSecHeightListener = null

                    //todo refactor move to a better location
                    transitionInLeftSection(false)
                }
            }

            mClearButton!!.visibility = if (savedState.query.isEmpty()) View.INVISIBLE else View.VISIBLE
            mLeftAction!!.visibility = View.VISIBLE

            mSearchInput?.let { Util.showSoftKeyboard(context, it) }
        }
    }

    class SavedState : BaseSavedState {

        var suggestions = ArrayList<SearchSuggestion>()
        var isFocused: Boolean = false
        var query: String = ""
        var queryTextSize: Int = 0
        var suggestionTextSize: Int = 0
        var searchHint: String = ""
        var dismissOnOutsideClick: Boolean = false
        var showMoveSuggestionUpBtn: Boolean = false
        var showSearchKey: Boolean = false
        var isTitleSet: Boolean = false
        var backgroundColor: Int = 0
        var suggestionsTextColor: Int = 0
        var queryTextColor: Int = 0
        var searchHintTextColor: Int = 0
        var actionOverflowMenuColor: Int = 0
        var menuItemIconColor: Int = 0
        var leftIconColor: Int = 0
        var clearBtnColor: Int = 0
        var suggestionUpBtnColor: Int = 0
        var dividerColor: Int = 0
        var menuId: Int = 0
        var leftActionMode: Int = 0
        var dimBackground: Boolean = false
        var suggestionsSectionAnimSuration: Long = 0L
        var dismissOnSoftKeyboardDismiss: Boolean = false
        var dismissFocusOnSuggestionItemClick: Boolean = false

        constructor(superState: Parcelable) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            `in`.readList(suggestions as MutableList<Any?>, javaClass.classLoader)
            isFocused = `in`.readInt() != 0
            query = `in`.readString()!!
            queryTextSize = `in`.readInt()
            suggestionTextSize = `in`.readInt()
            searchHint = `in`.readString()!!
            dismissOnOutsideClick = `in`.readInt() != 0
            showMoveSuggestionUpBtn = `in`.readInt() != 0
            showSearchKey = `in`.readInt() != 0
            isTitleSet = `in`.readInt() != 0
            backgroundColor = `in`.readInt()
            suggestionsTextColor = `in`.readInt()
            queryTextColor = `in`.readInt()
            searchHintTextColor = `in`.readInt()
            actionOverflowMenuColor = `in`.readInt()
            menuItemIconColor = `in`.readInt()
            leftIconColor = `in`.readInt()
            clearBtnColor = `in`.readInt()
            suggestionUpBtnColor = `in`.readInt()
            dividerColor = `in`.readInt()
            menuId = `in`.readInt()
            leftActionMode = `in`.readInt()
            dimBackground = `in`.readInt() != 0
            suggestionsSectionAnimSuration = `in`.readLong()
            dismissOnSoftKeyboardDismiss = `in`.readInt() != 0
            dismissFocusOnSuggestionItemClick = `in`.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeList(suggestions as MutableList<Any?>)
            out.writeInt(if (isFocused) 1 else 0)
            out.writeString(query)
            out.writeInt(queryTextSize)
            out.writeInt(suggestionTextSize)
            out.writeString(searchHint)
            out.writeInt(if (dismissOnOutsideClick) 1 else 0)
            out.writeInt(if (showMoveSuggestionUpBtn) 1 else 0)
            out.writeInt(if (showSearchKey) 1 else 0)
            out.writeInt(if (isTitleSet) 1 else 0)
            out.writeInt(backgroundColor)
            out.writeInt(suggestionsTextColor)
            out.writeInt(queryTextColor)
            out.writeInt(searchHintTextColor)
            out.writeInt(actionOverflowMenuColor)
            out.writeInt(menuItemIconColor)
            out.writeInt(leftIconColor)
            out.writeInt(clearBtnColor)
            out.writeInt(suggestionUpBtnColor)
            out.writeInt(dividerColor)
            out.writeInt(menuId)
            out.writeInt(leftActionMode)
            out.writeInt(if (dimBackground) 1 else 0)
            out.writeLong(suggestionsSectionAnimSuration)
            out.writeInt(if (dismissOnSoftKeyboardDismiss) 1 else 0)
            out.writeInt(if (dismissFocusOnSuggestionItemClick) 1 else 0)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

    }

    fun attachNavigationDrawerToMenuButton(drawerLayout: DrawerLayout) {
        drawerLayout.addDrawerListener(mDrawerListener)
        setOnLeftMenuClickListener(NavDrawerLeftMenuClickListener(drawerLayout))
    }

    fun detachNavigationDrawerFromMenuButton(drawerLayout: DrawerLayout) {
        drawerLayout.removeDrawerListener(mDrawerListener)
        setOnLeftMenuClickListener(null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        //remove any ongoing animations to prevent leaks
        //todo investigate if correct
        ViewCompat.animate(mSuggestionListContainer!!).cancel()
    }

    private inner class DrawerListener : DrawerLayout.DrawerListener {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            setMenuIconProgress(slideOffset)
        }

        override fun onDrawerOpened(drawerView: View) {

        }

        override fun onDrawerClosed(drawerView: View) {

        }

        override fun onDrawerStateChanged(newState: Int) {

        }
    }

    private inner class NavDrawerLeftMenuClickListener(internal var mDrawerLayout: DrawerLayout) : OnLeftMenuClickListener {

        override fun onMenuOpened() {
            mDrawerLayout.openDrawer(GravityCompat.START)
        }

        override fun onMenuClosed() {
            //do nothing
        }
    }

    companion object {

        private val TAG = FloatingSearchView::class.java.simpleName
        //The CardView's top or bottom height used for its shadow
        private const val CARD_VIEW_TOP_BOTTOM_SHADOW_HEIGHT = 3
        //The CardView's (default) corner radius height
        private const val CARD_VIEW_CORNERS_HEIGHT = 2
        private const val CARD_VIEW_CORNERS_AND_TOP_BOTTOM_SHADOW_HEIGHT = CARD_VIEW_TOP_BOTTOM_SHADOW_HEIGHT + CARD_VIEW_CORNERS_HEIGHT

        private const val CLEAR_BTN_FADE_ANIM_DURATION: Long = 500
        private const val CLEAR_BTN_WIDTH_DP = 48

        private const val LEFT_MENU_WIDTH_AND_MARGIN_START_DP = 52

        private const val MENU_BUTTON_PROGRESS_ARROW = 1.0f
        private const val MENU_BUTTON_PROGRESS_HAMBURGER = 0.0f

        private const val BACKGROUND_DRAWABLE_ALPHA_SEARCH_FOCUSED = 150
        private const val BACKGROUND_DRAWABLE_ALPHA_SEARCH_NOT_FOCUSED = 0
        private const val BACKGROUND_FADE_ANIM_DURATION = 250

        private const val MENU_ICON_ANIM_DURATION = 250

        private val SUGGEST_ITEM_ADD_ANIM_INTERPOLATOR = LinearInterpolator()

        const val LEFT_ACTION_MODE_SHOW_HAMBURGER = 1
        const val LEFT_ACTION_MODE_SHOW_SEARCH = 2
        const val LEFT_ACTION_MODE_SHOW_HOME = 3
        const val LEFT_ACTION_MODE_NO_LEFT_ACTION = 4
        private const val LEFT_ACTION_MODE_NOT_SET = -1

        @LeftActionMode
        private val ATTRS_SEARCH_BAR_LEFT_ACTION_MODE_DEFAULT = LEFT_ACTION_MODE_NO_LEFT_ACTION
        private const val ATTRS_SHOW_MOVE_UP_SUGGESTION_DEFAULT = false
        private const val ATTRS_DISMISS_ON_OUTSIDE_TOUCH_DEFAULT = true
        private const val ATTRS_DISMISS_ON_KEYBOARD_DISMISS_DEFAULT = false
        private const val ATTRS_SEARCH_BAR_SHOW_SEARCH_KEY_DEFAULT = true
        private const val ATTRS_QUERY_TEXT_SIZE_SP_DEFAULT = 18
        private const val ATTRS_SUGGESTION_TEXT_SIZE_SP_DEFAULT = 18
        private const val ATTRS_SHOW_DIM_BACKGROUND_DEFAULT = true
        private const val ATTRS_SUGGESTION_ANIM_DURATION_DEFAULT = 250
        private const val ATTRS_SEARCH_BAR_MARGIN_DEFAULT = 0
        private const val ATTRS_DISMISS_FOCUS_ON_ITEM_SELECTION_DEFAULT = false
    }
}
