package com.arlib.floatingsearchview.util.view

/**
 * Copyright (C) 2015 Ari C.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuItemImpl
import com.arlib.floatingsearchview.R
import com.arlib.floatingsearchview.util.MenuPopupHelper
import com.arlib.floatingsearchview.util.Util
import com.bartoszlipinski.viewpropertyobjectanimator.ViewPropertyObjectAnimator
import java.util.*

/**
 * A view that shows menu items as actions or
 * as items in a overflow popup.
 */
class MenuView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private var mMenu = -1
    private var mMenuBuilder: MenuBuilder? = null
    private var mMenuInflater: SupportMenuInflater? = null
    private var mMenuPopupHelper: MenuPopupHelper? = null

    private var mMenuCallback: MenuBuilder.Callback? = null

    private var mActionIconColor: Int = 0
    private var mOverflowIconColor: Int = 0

    //all menu items
    private var mMenuItems: MutableList<MenuItemImpl>? = null

    //items that are currently presented as actions
    private var mActionItems: MutableList<MenuItemImpl> = ArrayList()

    private var mActionShowAlwaysItems: MutableList<MenuItemImpl> = ArrayList()

    private var mHasOverflow = false

    private var mOnVisibleWidthChangedListener: OnVisibleWidthChangedListener? = null
    var visibleWidth: Int = 0
        private set

    private var anims: MutableList<ObjectAnimator> = ArrayList()

    val currentMenuItems: List<MenuItemImpl>?
        get() = mMenuItems

    private val overflowActionView: ImageView
        get() = LayoutInflater.from(context).inflate(R.layout.overflow_action_item_layout, this, false) as ImageView

    private val menuInflater: MenuInflater
        @SuppressLint("RestrictedApi")
        get() {
            return when (mMenuInflater) {
                null -> SupportMenuInflater(context)
                else -> mMenuInflater!!
            }
        }

    interface OnVisibleWidthChangedListener {
        fun onItemsMenuVisibleWidthChanged(newVisibleWidth: Int)
    }

    init {
        ACTION_DIMENSION_PX = context.resources.getDimension(R.dimen.square_button_size)
        init()
    }

    @SuppressLint("RestrictedApi")
    private fun init() {
        mMenuBuilder = MenuBuilder(context)
        mMenuPopupHelper = MenuPopupHelper(context, mMenuBuilder!!, this)
        mActionIconColor = Util.getColor(context, R.color.gray_active_icon)
        mOverflowIconColor = Util.getColor(context, R.color.gray_active_icon)
    }

    fun setActionIconColor(actionColor: Int) {
        this.mActionIconColor = actionColor
        refreshColors()
    }

    fun setOverflowColor(overflowColor: Int) {
        this.mOverflowIconColor = overflowColor
        refreshColors()
    }

    private fun refreshColors() {
        for (i in 0 until childCount) {
            Util.setIconColor(getChildAt(i) as ImageView, mActionIconColor)
            if (mHasOverflow && i == childCount - 1) {
                Util.setIconColor(getChildAt(i) as ImageView, mOverflowIconColor)
            }
        }
    }

    /**
     * Set the callback that will be called when menu
     * items a selected.
     *
     * @param menuCallback
     */
    fun setMenuCallback(menuCallback: MenuBuilder.Callback) {
        this.mMenuCallback = menuCallback
    }

    /**
     * Resets the the view to fit into a new
     * available width.
     *
     *
     *
     * This clears and then re-inflates the menu items
     * , removes all of its associated action views, and re-creates
     * the menu and action items to fit in the new width.
     *
     * @param availWidth the width available for the menu to use. If
     * there is room, menu items that are flagged with
     * android:showAsAction="ifRoom" or android:showAsAction="always"
     * will show as actions.
     */
    @SuppressLint("RestrictedApi")
    fun reset(menu: Int, availWidth: Int) {
        mMenu = menu
        if (mMenu == -1) {
            return
        }

        mActionShowAlwaysItems = ArrayList()
        mActionItems = ArrayList()
        mMenuItems = ArrayList()
        mMenuBuilder = MenuBuilder(context)
        mMenuPopupHelper = MenuPopupHelper(context, mMenuBuilder!!, this)

        //clean view and re-inflate
        removeAllViews()
        menuInflater.inflate(mMenu, mMenuBuilder)

        mMenuItems = mMenuBuilder!!.actionItems
        mMenuItems!!.addAll(mMenuBuilder!!.nonActionItems)

        mMenuItems!!.sortWith(Comparator { lhs, rhs -> lhs.order.compareTo(rhs.order) })

        val localActionItems = filter(mMenuItems!!, object : MenuItemImplPredicate {
            @SuppressLint("RestrictedApi")
            override fun apply(menuItem: MenuItemImpl): Boolean {
                return menuItem.icon != null && (menuItem.requiresActionButton() || menuItem.requestsActionButton())
            }
        })


        var availItemRoom = availWidth / ACTION_DIMENSION_PX.toInt()

        //determine if to show overflow menu
        var addOverflowAtTheEnd = false
        if (localActionItems.size < mMenuItems!!.size || availItemRoom < localActionItems.size) {
            addOverflowAtTheEnd = true
            availItemRoom--
        }

        val actionItemsIds: ArrayList<Int> = ArrayList()
        if (availItemRoom > 0) {
            for (i in localActionItems.indices) {

                val menuItem = localActionItems[i]
                if (menuItem.icon != null) {

                    val action = createActionView()
                    action.contentDescription = menuItem.title
                    action.setImageDrawable(menuItem.icon)
                    Util.setIconColor(action, mActionIconColor)
                    addView(action)
                    mActionItems.add(menuItem)
                    actionItemsIds.add(menuItem.itemId)

                    action.setOnClickListener {
                        if (mMenuCallback != null) {
                            mMenuCallback!!.onMenuItemSelected(mMenuBuilder, menuItem)
                        }
                    }

                    availItemRoom--
                    if (availItemRoom == 0) {
                        break
                    }
                }
            }
        }

        mHasOverflow = addOverflowAtTheEnd
        if (addOverflowAtTheEnd) {

            val overflowAction = overflowActionView
            overflowAction.setImageResource(R.drawable.ic_more_vert_black_24dp)
            Util.setIconColor(overflowAction, mOverflowIconColor)
            addView(overflowAction)

            overflowAction.setOnClickListener { mMenuPopupHelper!!.show() }

            mMenuBuilder!!.setCallback(mMenuCallback)
        }

        //remove all menu items that will be shown as icons (the action items) from the overflow menu
        for (id in actionItemsIds) {
            mMenuBuilder!!.removeItem(id)
        }

        if (mOnVisibleWidthChangedListener != null) {
            visibleWidth = ACTION_DIMENSION_PX.toInt() * childCount - if (mHasOverflow) Util.dpToPx(8) else 0
            mOnVisibleWidthChangedListener!!.onItemsMenuVisibleWidthChanged(visibleWidth)
        }
    }

    private fun createActionView(): ImageView {
        return LayoutInflater.from(context).inflate(R.layout.action_item_layout, this, false) as ImageView
    }

    /**
     * Hides all the menu items flagged with "ifRoom"
     *
     * @param withAnim
     */
    @SuppressLint("RestrictedApi")
    fun hideIfRoomItems(withAnim: Boolean) {

        if (mMenu == -1) {
            return
        }

        mActionShowAlwaysItems.clear()
        cancelChildAnimListAndClear()

        val showAlwaysActionItems = filter(mMenuItems!!, object : MenuItemImplPredicate {
            @SuppressLint("RestrictedApi")
            override fun apply(menuItem: MenuItemImpl): Boolean {
                return menuItem.icon != null && menuItem.requiresActionButton()
            }
        })

        var actionItemIndex = 0
        while (actionItemIndex < mActionItems.size && actionItemIndex < showAlwaysActionItems.size) {

            val showAlwaysActionItem = showAlwaysActionItems[actionItemIndex]

            //reset action item image if needed
            if (mActionItems[actionItemIndex].itemId != showAlwaysActionItem.itemId) {

                val action = getChildAt(actionItemIndex) as ImageView
                action.setImageDrawable(showAlwaysActionItem.icon)
                Util.setIconColor(action, mOverflowIconColor)
                action.setOnClickListener {
                    if (mMenuCallback != null) {
                        mMenuCallback!!.onMenuItemSelected(mMenuBuilder, showAlwaysActionItem)
                    }
                }
            }
            mActionShowAlwaysItems.add(showAlwaysActionItem)
            actionItemIndex++
        }

        val diff = mActionItems.size - actionItemIndex + if (mHasOverflow) 1 else 0

        anims = ArrayList()

        //add anims for moving showAlwaysItem views to the right
        for (i in 0 until actionItemIndex) {

            val currentChild = getChildAt(i)
            val destTransX = ACTION_DIMENSION_PX * diff - if (mHasOverflow) Util.dpToPx(8) else 0
            anims.add(ViewPropertyObjectAnimator.animate(currentChild)
                    .setDuration((if (withAnim) HIDE_IF_ROOM_ITEMS_ANIM_DURATION else 0).toLong())
                    .setInterpolator(AccelerateInterpolator())
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentChild.translationX = destTransX
                        }
                    })
                    .translationXBy(destTransX).get())
        }

        //add anims for moving to right and/or zooming out previously shown items
        for (i in actionItemIndex until diff + actionItemIndex) {

            val currentView = getChildAt(i)
            currentView.isClickable = false

            //move to right
            if (i != childCount - 1) {
                anims.add(ViewPropertyObjectAnimator.animate(currentView).setDuration((if (withAnim) HIDE_IF_ROOM_ITEMS_ANIM_DURATION else 0).toLong())
                        .addListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {

                                currentView.translationX = ACTION_DIMENSION_PX
                            }
                        }).translationXBy(ACTION_DIMENSION_PX).get())
            }

            //scale and zoom out
            anims.add(ViewPropertyObjectAnimator.animate(currentView)
                    .setDuration((if (withAnim) HIDE_IF_ROOM_ITEMS_ANIM_DURATION else 0).toLong())
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentView.scaleX = 0.5f
                        }
                    }).scaleX(.5f).get())
            anims.add(ViewPropertyObjectAnimator.animate(currentView)
                    .setDuration((if (withAnim) HIDE_IF_ROOM_ITEMS_ANIM_DURATION else 0).toLong())
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentView.scaleY = 0.5f
                        }
                    }).scaleY(.5f).get())
            anims.add(ViewPropertyObjectAnimator.animate(currentView)
                    .setDuration((if (withAnim) HIDE_IF_ROOM_ITEMS_ANIM_DURATION else 0).toLong())
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentView.alpha = 0.0f
                        }
                    }).alpha(0.0f).get())
        }

        //finally, run animation
        if (anims.isNotEmpty()) {

            val animSet = AnimatorSet()
            if (!withAnim) {
                //temporary, from laziness
                animSet.duration = 0
            }
            animSet.playTogether(*anims.toTypedArray())
            animSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {

                    if (mOnVisibleWidthChangedListener != null) {
                        visibleWidth = ACTION_DIMENSION_PX.toInt() * actionItemIndex
                        mOnVisibleWidthChangedListener!!.onItemsMenuVisibleWidthChanged(visibleWidth)
                    }
                }
            })
            animSet.start()
        }
    }

    /**
     * Shows all the menu items that were hidden by hideIfRoomItems(boolean withAnim)
     *
     * @param withAnim
     */
    @SuppressLint("RestrictedApi")
    fun showIfRoomItems(withAnim: Boolean) {

        if (mMenu == -1) {
            return
        }

        cancelChildAnimListAndClear()

        if (mMenuItems!!.isEmpty()) {
            return
        }

        anims = ArrayList()

        for (i in 0 until childCount) {

            val currentView = getChildAt(i)

            //reset all the action item views
            if (i < mActionItems.size) {
                val action = currentView as ImageView
                val actionItem = mActionItems[i]
                action.setImageDrawable(actionItem.icon)
                Util.setIconColor(action, mActionIconColor)
                action.setOnClickListener {
                    if (mMenuCallback != null) {
                        mMenuCallback!!.onMenuItemSelected(mMenuBuilder, actionItem)
                    }
                }
            }

            var interpolator: Interpolator = DecelerateInterpolator()
            if (i > mActionShowAlwaysItems.size - 1) {
                interpolator = LinearInterpolator()
            }

            currentView.isClickable = true

            //simply animate all properties of all action item views back to their default/visible state
            anims.add(ViewPropertyObjectAnimator.animate(currentView)
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentView.translationX = 0f
                        }
                    })
                    .setInterpolator(interpolator)
                    .translationX(0f).get())
            anims.add(ViewPropertyObjectAnimator.animate(currentView)
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentView.scaleX = 1.0f
                        }
                    })
                    .setInterpolator(interpolator)
                    .scaleX(1.0f).get())
            anims.add(ViewPropertyObjectAnimator.animate(currentView)
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentView.scaleY = 1.0f
                        }
                    })
                    .setInterpolator(interpolator)
                    .scaleY(1.0f).get())
            anims.add(ViewPropertyObjectAnimator.animate(currentView)
                    .addListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {

                            currentView.alpha = 1.0f
                        }
                    })
                    .setInterpolator(interpolator)
                    .alpha(1.0f).get())
        }

        if (anims.isEmpty()) {
            return
        }

        val animSet = AnimatorSet()
        if (!withAnim) {
            //temporary, from laziness
            animSet.duration = 0
        }
        animSet.playTogether(*anims.toTypedArray())
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {

                if (mOnVisibleWidthChangedListener != null) {
                    visibleWidth = childCount * ACTION_DIMENSION_PX.toInt() - if (mHasOverflow) Util.dpToPx(8) else 0
                    mOnVisibleWidthChangedListener!!.onItemsMenuVisibleWidthChanged(visibleWidth)
                }
            }
        })
        animSet.start()
    }

    private interface MenuItemImplPredicate {

        fun apply(menuItem: MenuItemImpl): Boolean
    }

    private fun filter(target: List<MenuItemImpl>, predicate: MenuItemImplPredicate): List<MenuItemImpl> {
        val result = ArrayList<MenuItemImpl>()
        for (element in target) {
            if (predicate.apply(element)) {
                result.add(element)
            }
        }
        return result
    }

    fun setOnVisibleWidthChanged(listener: OnVisibleWidthChangedListener) {
        this.mOnVisibleWidthChangedListener = listener
    }

    private fun cancelChildAnimListAndClear() {
        for (animator in anims) {
            animator.cancel()
        }
        anims.clear()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        //clear anims if any to avoid leak
        cancelChildAnimListAndClear()
    }

    companion object {
        private const val HIDE_IF_ROOM_ITEMS_ANIM_DURATION = 400
        private const val SHOW_IF_ROOM_ITEMS_ANIM_DURATION = 450
        private var ACTION_DIMENSION_PX: Float = 0f
    }
}
