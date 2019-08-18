package com.arlib.floatingsearchview.util

/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Parcelable
import android.view.*
import android.view.View.MeasureSpec
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.PopupWindow
import androidx.appcompat.view.menu.*
import androidx.appcompat.widget.ListPopupWindow
import com.arlib.floatingsearchview.R

/**
 * Presents a menu as a small, simple popup anchored to another view.
 */
@SuppressLint("RestrictedApi")
class MenuPopupHelper @JvmOverloads constructor(private val mContext: Context, private val mMenu: MenuBuilder, private var mAnchorView: View? = null,
                                                private val mOverflowOnly: Boolean = false, private val mPopupStyleAttr: Int = R.attr.popupMenuStyle, private val mPopupStyleRes: Int = 0) : AdapterView.OnItemClickListener, View.OnKeyListener, ViewTreeObserver.OnGlobalLayoutListener, PopupWindow.OnDismissListener, MenuPresenter {
    private val mInflater: LayoutInflater
    private val mAdapter: MenuAdapter?
    private val mPopupMaxWidth: Int
    var popup: ListPopupWindow? = null
        private set
    private var mTreeObserver: ViewTreeObserver? = null
    private var mPresenterCallback: MenuPresenter.Callback? = null
    internal var mForceShowIcon: Boolean = false
    private var mMeasureParent: ViewGroup? = null
    /** Whether the cached content width value is valid.  */
    private var mHasContentWidth: Boolean = false
    /** Cached content width from [.measureContentWidth].  */
    private var mContentWidth: Int = 0
    var gravity = Gravity.NO_GRAVITY

    var mOffsetX: Float = 0.toFloat()

    var mOffsetY: Float = 0.toFloat()
    val isShowing: Boolean
        get() = popup != null && popup!!.isShowing

    init {
        mInflater = LayoutInflater.from(mContext)
        mAdapter = MenuAdapter(mMenu)
        val res = mContext.resources
        mPopupMaxWidth = Math.max(res.displayMetrics.widthPixels / 2,
                res.getDimensionPixelSize(R.dimen.abc_config_prefDialogWidth))
        // Present the menu using our context, not the menu builder's context.
        mMenu.addMenuPresenter(this, mContext)
    }

    fun setOffsetX(x: Float) {
        this.mOffsetX = x
    }

    fun setOffsetY(y: Float) {
        this.mOffsetY = y
    }

    fun setAnchorView(anchor: View) {
        mAnchorView = anchor
    }

    fun setForceShowIcon(forceShow: Boolean) {
        mForceShowIcon = forceShow
    }

    fun show() {
        if (!tryShow()) {
            throw IllegalStateException("MenuPopupHelper cannot be used without an anchor")
        }
    }

    fun tryShow(): Boolean {
        popup = ListPopupWindow(mContext, null, mPopupStyleAttr, mPopupStyleRes)
        popup!!.setOnDismissListener(this)
        popup!!.setOnItemClickListener(this)
        popup!!.setAdapter(mAdapter)
        popup!!.isModal = true
        val anchor = mAnchorView
        if (anchor != null) {
            val addGlobalListener = mTreeObserver == null
            mTreeObserver = anchor.viewTreeObserver // Refresh to latest
            if (addGlobalListener) mTreeObserver!!.addOnGlobalLayoutListener(this)
            popup!!.anchorView = anchor
            popup!!.setDropDownGravity(gravity)
        } else {
            return false
        }
        if (!mHasContentWidth) {
            mContentWidth = measureContentWidth()
            mHasContentWidth = true
        }
        popup!!.setContentWidth(mContentWidth)
        popup!!.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED

        var vertOffset = -mAnchorView!!.height + Util.dpToPx(4)
        var horizontalOffset = -mContentWidth + mAnchorView!!.width
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            vertOffset = -mAnchorView!!.height - Util.dpToPx(4)
            horizontalOffset = -mContentWidth + mAnchorView!!.width - Util.dpToPx(8)
        }
        popup!!.verticalOffset = vertOffset
        popup!!.horizontalOffset = horizontalOffset
        popup!!.show()
        popup!!.listView!!.setOnKeyListener(this)
        return true
    }

    fun dismiss() {
        if (isShowing) {
            popup!!.dismiss()
        }
    }

    override fun onDismiss() {
        popup = null
        mMenu.close()
        if (mTreeObserver != null) {
            if (!mTreeObserver!!.isAlive) mTreeObserver = mAnchorView!!.viewTreeObserver
            mTreeObserver!!.removeGlobalOnLayoutListener(this)
            mTreeObserver = null
        }
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val adapter = mAdapter
        adapter?.mAdapterMenu?.performItemAction(adapter.getItem(position), 0)
    }

    override fun onKey(v: View, keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return false
    }

    private fun measureContentWidth(): Int {
        // Menus don't tend to be long, so this is more sane than it looks.
        var maxWidth = 0
        var itemView: View? = null
        var itemType = 0
        val adapter = mAdapter
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val count = adapter!!.count
        for (i in 0 until count) {
            val positionType = adapter.getItemViewType(i)
            if (positionType != itemType) {
                itemType = positionType
                itemView = null
            }
            if (mMeasureParent == null) {
                mMeasureParent = FrameLayout(mContext)
            }
            itemView = adapter.getView(i, itemView, mMeasureParent!!)
            itemView.measure(widthMeasureSpec, heightMeasureSpec)
            val itemWidth = itemView.measuredWidth
            if (itemWidth >= mPopupMaxWidth) {
                return mPopupMaxWidth
            } else if (itemWidth > maxWidth) {
                maxWidth = itemWidth
            }
        }
        return maxWidth
    }

    override fun onGlobalLayout() {
        if (isShowing) {
            val anchor = mAnchorView
            if (anchor == null || !anchor.isShown) {
                dismiss()
            } else if (isShowing) {
                // Recompute window size and position
                popup!!.show()
            }
        }
    }

    override fun initForMenu(context: Context, menu: MenuBuilder) {
        // Don't need to do anything; we added as a presenter in the constructor.
    }

    override fun getMenuView(root: ViewGroup): MenuView {
        throw UnsupportedOperationException("MenuPopupHelpers manage their own views")
    }

    override fun updateMenuView(cleared: Boolean) {
        mHasContentWidth = false
        mAdapter?.notifyDataSetChanged()
    }

    override fun setCallback(cb: MenuPresenter.Callback?) {
        mPresenterCallback = cb
    }

    override fun onSubMenuSelected(subMenu: SubMenuBuilder): Boolean {
        if (subMenu.hasVisibleItems()) {
            val subPopup = MenuPopupHelper(mContext, subMenu, mAnchorView)
            subPopup.setCallback(mPresenterCallback)
            var preserveIconSpacing = false
            val count = subMenu.size()
            for (i in 0 until count) {
                val childItem = subMenu.getItem(i)
                if (childItem.isVisible && childItem.icon != null) {
                    preserveIconSpacing = true
                    break
                }
            }
            subPopup.setForceShowIcon(preserveIconSpacing)
            if (subPopup.tryShow()) {
                if (mPresenterCallback != null) {
                    mPresenterCallback!!.onOpenSubMenu(subMenu)
                }
                return true
            }
        }
        return false
    }

    override fun onCloseMenu(menu: MenuBuilder, allMenusAreClosing: Boolean) {
        // Only care about the (sub)menu we're presenting.
        if (menu !== mMenu) return
        dismiss()
        if (mPresenterCallback != null) {
            mPresenterCallback!!.onCloseMenu(menu, allMenusAreClosing)
        }
    }

    override fun flagActionItems(): Boolean {
        return false
    }

    override fun expandItemActionView(menu: MenuBuilder, item: MenuItemImpl): Boolean {
        return false
    }

    override fun collapseItemActionView(menu: MenuBuilder, item: MenuItemImpl): Boolean {
        return false
    }

    override fun getId(): Int {
        return 0
    }

    override fun onSaveInstanceState(): Parcelable? {
        return null
    }

    override fun onRestoreInstanceState(state: Parcelable) {}
    inner class MenuAdapter(val mAdapterMenu: MenuBuilder) : BaseAdapter() {
        private var mExpandedIndex = -1

        init {
            findExpandedIndex()
        }

        override fun getCount(): Int {
            val items = if (mOverflowOnly)
                mAdapterMenu.nonActionItems
            else
                mAdapterMenu.visibleItems
            return if (mExpandedIndex < 0) {
                items.size
            } else items.size - 1
        }

        override fun getItem(position: Int): MenuItemImpl {
            var newPosition = position
            val items = if (mOverflowOnly)
                mAdapterMenu.nonActionItems
            else
                mAdapterMenu.visibleItems
            if (mExpandedIndex in 0..newPosition) {
                newPosition++
            }
            return items[newPosition]
        }

        override fun getItemId(position: Int): Long {
            // Since a menu item's ID is optional, we'll use the position as an
            // ID for the item in the AdapterView
            return position.toLong()
        }

        @SuppressLint("RestrictedApi")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var newConvertView = convertView
            if (newConvertView == null) {
                newConvertView = mInflater.inflate(ITEM_LAYOUT, parent, false)
            }
            val itemView = newConvertView as MenuView.ItemView?
            if (mForceShowIcon) {
                (newConvertView as ListMenuItemView).setForceShowIcon(true)
            }
            itemView!!.initialize(getItem(position), 0)
            return newConvertView!!
        }

        @SuppressLint("RestrictedApi")
        internal fun findExpandedIndex() {
            val expandedItem = mMenu.expandedItem
            if (expandedItem != null) {
                val items = mMenu.nonActionItems
                val count = items.size
                for (i in 0 until count) {
                    val item = items[i]
                    if (item == expandedItem) {
                        mExpandedIndex = i
                        return
                    }
                }
            }
            mExpandedIndex = -1
        }

        override fun notifyDataSetChanged() {
            findExpandedIndex()
            super.notifyDataSetChanged()
        }
    }

    companion object {
        private val TAG = "MenuPopupHelper"
        internal val ITEM_LAYOUT = R.layout.abc_popup_menu_item_layout
    }
}