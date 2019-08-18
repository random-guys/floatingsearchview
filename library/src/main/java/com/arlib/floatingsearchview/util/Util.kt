package com.arlib.floatingsearchview.util

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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Display
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView

object Util {

    private val TAG = "Util"

    fun showSoftKeyboard(context: Context, editText: EditText) {

        Handler().postDelayed({
            val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    fun closeSoftKeyboard(activity: Activity) {

        val currentFocusView = activity.currentFocus
        if (currentFocusView != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
        }
    }

    fun dpToPx(dp: Int): Int {
        val metrics = Resources.getSystem().displayMetrics
        return (dp * metrics.density).toInt()
    }

    fun pxToDp(px: Int): Int {
        val metrics = Resources.getSystem().displayMetrics
        return (px / metrics.density).toInt()
    }

    fun spToPx(sp: Int): Int {
        val metrics = Resources.getSystem().displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp.toFloat(), metrics).toInt()
    }

    fun pxToSp(px: Int): Int {
        val metrics = Resources.getSystem().displayMetrics
        return px / metrics.scaledDensity.toInt()
    }

    fun getScreenWidth(activity: Activity): Int {

        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        return outMetrics.widthPixels
    }

    fun getScreenHeight(activity: Activity): Int {

        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        return outMetrics.heightPixels
    }

    fun setIconColor(iconHolder: ImageView, color: Int) {
        val wrappedDrawable = DrawableCompat.wrap(iconHolder.drawable)
        DrawableCompat.setTint(wrappedDrawable, color)
        iconHolder.setImageDrawable(wrappedDrawable)
        iconHolder.invalidate()
    }

    /**
     * Gets a reference to a given drawable and prepares it for use with tinting through.
     *
     * @param resId the resource id for the given drawable
     * @return a wrapped drawable ready fo use
     * with [androidx.core.graphics.drawable.DrawableCompat]'s tinting methods
     * @throws Resources.NotFoundException
     */
    @Throws(Resources.NotFoundException::class)
    fun getWrappedDrawable(context: Context, @DrawableRes resId: Int): Drawable {
        return DrawableCompat.wrap(ResourcesCompat.getDrawable(context.resources,
                resId, null)!!)
    }

    @Throws(Resources.NotFoundException::class)
    fun getColor(context: Context, @ColorRes resId: Int): Int {
        return ContextCompat.getColor(context, resId)
    }

    fun removeGlobalLayoutObserver(view: View, layoutListener: ViewTreeObserver.OnGlobalLayoutListener) {
        if (Build.VERSION.SDK_INT < 16) {
            view.viewTreeObserver.removeGlobalOnLayoutListener(layoutListener)
        } else {
            view.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        }
    }

    fun getHostActivity(context: Context): Activity? {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }
}
