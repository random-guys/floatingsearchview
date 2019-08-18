package com.arlib.floatingsearchviewdemo.data

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
import android.widget.Filter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.util.*

object DataHelper {

    private const val COLORS_FILE_NAME = "colors.json"

    private var sColorWrappers: List<ColorWrapper>? = ArrayList()

    private val sColorSuggestions = ArrayList(listOf(ColorSuggestion("green"), ColorSuggestion("blue"), ColorSuggestion("pink"), ColorSuggestion("purple"), ColorSuggestion("brown"), ColorSuggestion("gray"), ColorSuggestion("Granny Smith Apple"), ColorSuggestion("Indigo"), ColorSuggestion("Periwinkle"), ColorSuggestion("Mahogany"), ColorSuggestion("Maize"), ColorSuggestion("Mahogany"), ColorSuggestion("Outer Space"), ColorSuggestion("Melon"), ColorSuggestion("Yellow"), ColorSuggestion("Orange"), ColorSuggestion("Red"), ColorSuggestion("Orchid")))

    interface OnFindColorsListener {
        fun onResults(results: List<ColorWrapper>)
    }

    interface OnFindSuggestionsListener {
        fun onResults(results: List<ColorSuggestion>)
    }

    fun getHistory(context: Context, count: Int): List<ColorSuggestion> {

        val suggestionList = ArrayList<ColorSuggestion>()
        var colorSuggestion: ColorSuggestion
        for (i in sColorSuggestions.indices) {
            colorSuggestion = sColorSuggestions[i]
            colorSuggestion.isHistory = true
            suggestionList.add(colorSuggestion)
            if (suggestionList.size == count) {
                break
            }
        }
        return suggestionList
    }

    fun resetSuggestionsHistory() {
        for (colorSuggestion in sColorSuggestions) {
            colorSuggestion.isHistory = false
        }
    }

    fun findSuggestions(context: Context, query: String, limit: Int, simulatedDelay: Long,
                        listener: OnFindSuggestionsListener?) {
        object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {

                try {
                    Thread.sleep(simulatedDelay)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                resetSuggestionsHistory()
                val suggestionList = ArrayList<ColorSuggestion>()
                if (!(constraint == null || constraint.isEmpty())) {

                    for (suggestion in sColorSuggestions) {
                        if (suggestion.body!!.toUpperCase()
                                        .startsWith(constraint.toString().toUpperCase())) {

                            suggestionList.add(suggestion)
                            if (limit != -1 && suggestionList.size == limit) {
                                break
                            }
                        }
                    }
                }

                val results = FilterResults()
                suggestionList.sortWith(Comparator { lhs, _ -> if (lhs.isHistory) -1 else 0 })
                results.values = suggestionList
                results.count = suggestionList.size

                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {

                listener?.onResults(results.values as List<ColorSuggestion>)
            }
        }.filter(query)

    }


    fun findColors(context: Context, query: String, listener: OnFindColorsListener?) {
        initColorWrapperList(context)

        object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {


                val suggestionList = ArrayList<ColorWrapper>()

                if (!(constraint == null || constraint.length == 0)) {

                    for (color in sColorWrappers!!) {
                        if (color.name!!.toUpperCase()
                                        .startsWith(constraint.toString().toUpperCase())) {

                            suggestionList.add(color)
                        }
                    }

                }

                val results = FilterResults()
                results.values = suggestionList
                results.count = suggestionList.size

                return results
            }

            override fun publishResults(constraint: CharSequence, results: FilterResults) {

                listener?.onResults(results.values as List<ColorWrapper>)
            }
        }.filter(query)

    }

    private fun initColorWrapperList(context: Context) {

        if (sColorWrappers!!.isEmpty()) {
            val jsonString = loadJson(context)
            sColorWrappers = deserializeColors(jsonString)
        }
    }

    private fun loadJson(context: Context): String? {

        val jsonString: String

        try {
            val `is` = context.assets.open(COLORS_FILE_NAME)
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            jsonString = String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }

        return jsonString
    }

    private fun deserializeColors(jsonString: String?): List<ColorWrapper>? {
        val gson = Gson()
        val collectionType = object : TypeToken<List<ColorWrapper>>() {}.type
        return gson.fromJson<List<ColorWrapper>>(jsonString, collectionType)
    }

}