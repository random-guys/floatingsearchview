package com.arlib.floatingsearchview.suggestions

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

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.arlib.floatingsearchview.R

import com.arlib.floatingsearchview.util.Util

import java.util.ArrayList
import java.util.Collections

class SearchSuggestionsAdapter(private val mContext: Context, private val mBodyTextSizePx: Int, private val mListener: Listener?) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var dataSet: List<SearchSuggestion>? = ArrayList()
        private set

    private val mRightIconDrawable: Drawable
    private var mShowRightMoveUpBtn = false
    private var mTextColor = -1
    private var mRightIconColor = -1

    private var mOnBindSuggestionCallback: OnBindSuggestionCallback? = null

    interface OnBindSuggestionCallback {

        fun onBindSuggestion(suggestionView: View, leftIcon: ImageView, textView: TextView,
                             item: SearchSuggestion, itemPosition: Int)
    }

    interface Listener {

        fun onItemSelected(item: SearchSuggestion)

        fun onMoveItemToSearchClicked(item: SearchSuggestion)
    }

    class SearchSuggestionViewHolder(v: View, private val mListener: Listener?) : RecyclerView.ViewHolder(v) {

        var body: TextView
        var leftIcon: ImageView
        var rightIcon: ImageView

        interface Listener {

            fun onItemClicked(adapterPosition: Int)

            fun onMoveItemToSearchClicked(adapterPosition: Int)
        }

        init {
            body = v.findViewById<View>(R.id.body) as TextView
            leftIcon = v.findViewById<View>(R.id.left_icon) as ImageView
            rightIcon = v.findViewById<View>(R.id.right_icon) as ImageView

            rightIcon.setOnClickListener {
                val adapterPosition = adapterPosition
                if (mListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    mListener.onMoveItemToSearchClicked(getAdapterPosition())
                }
            }

            itemView.setOnClickListener {
                val adapterPosition = adapterPosition
                if (mListener != null && adapterPosition != RecyclerView.NO_POSITION) {
                    mListener.onItemClicked(adapterPosition)
                }
            }
        }
    }

    init {

        mRightIconDrawable = Util.getWrappedDrawable(mContext, R.drawable.ic_arrow_back_black_24dp)
        DrawableCompat.setTint(mRightIconDrawable, Util.getColor(mContext, R.color.gray_active_icon))
    }

    fun swapData(searchSuggestions: List<SearchSuggestion>) {
        dataSet = searchSuggestions
        notifyDataSetChanged()
    }

    fun setOnBindSuggestionCallback(callback: OnBindSuggestionCallback) {
        this.mOnBindSuggestionCallback = callback
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerView.ViewHolder {

        val view = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.search_suggestion_item, viewGroup, false)
        val viewHolder = SearchSuggestionViewHolder(view,
                object : SearchSuggestionViewHolder.Listener {

                    override fun onItemClicked(adapterPosition: Int) {

                        mListener?.onItemSelected(dataSet!![adapterPosition])
                    }

                    override fun onMoveItemToSearchClicked(adapterPosition: Int) {

                        mListener?.onMoveItemToSearchClicked(dataSet!![adapterPosition])
                    }

                })

        viewHolder.rightIcon.setImageDrawable(mRightIconDrawable)
        viewHolder.body.setTextSize(TypedValue.COMPLEX_UNIT_PX, mBodyTextSizePx.toFloat())

        return viewHolder
    }

    override fun onBindViewHolder(vh: RecyclerView.ViewHolder, position: Int) {

        val viewHolder = vh as SearchSuggestionViewHolder

        if (!mShowRightMoveUpBtn) {
            viewHolder.rightIcon.isEnabled = false
            viewHolder.rightIcon.visibility = View.INVISIBLE
        } else {
            viewHolder.rightIcon.isEnabled = true
            viewHolder.rightIcon.visibility = View.VISIBLE
        }

        val suggestionItem = dataSet!![position]
        viewHolder.body.text = suggestionItem.body

        if (mTextColor != -1) {
            viewHolder.body.setTextColor(mTextColor)
        }

        if (mRightIconColor != -1) {
            Util.setIconColor(viewHolder.rightIcon, mRightIconColor)
        }

        if (mOnBindSuggestionCallback != null) {
            mOnBindSuggestionCallback!!.onBindSuggestion(viewHolder.itemView, viewHolder.leftIcon, viewHolder.body,
                    suggestionItem, position)
        }
    }

    override fun getItemCount(): Int {
        return if (dataSet != null) dataSet!!.size else 0
    }

    fun setTextColor(color: Int) {

        var notify = false
        if (this.mTextColor != color) {
            notify = true
        }
        this.mTextColor = color
        if (notify) {
            notifyDataSetChanged()
        }
    }

    fun setRightIconColor(color: Int) {

        var notify = false
        if (this.mRightIconColor != color) {
            notify = true
        }
        this.mRightIconColor = color
        if (notify) {
            notifyDataSetChanged()
        }
    }

    fun setShowMoveUpIcon(show: Boolean) {

        var notify = false
        if (this.mShowRightMoveUpBtn != show) {
            notify = true
        }
        this.mShowRightMoveUpBtn = show
        if (notify) {
            notifyDataSetChanged()
        }
    }

    fun reverseList() {
        Collections.reverse(dataSet!!)
        notifyDataSetChanged()
    }

    companion object {

        private val TAG = "SearchSuggestionsAdapter"
    }
}
