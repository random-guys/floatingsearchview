package com.arlib.floatingsearchviewdemo.data

import android.os.Parcel
import android.os.Parcelable

import com.arlib.floatingsearchview.suggestions.SearchSuggestion

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

class ColorSuggestion : SearchSuggestion {
    override val body: String
        get() = mColorName!!

    private var mColorName: String? = null
    var isHistory = false

    constructor(suggestion: String) {
        this.mColorName = suggestion.toLowerCase()
    }

    constructor(source: Parcel) {
        this.mColorName = source.readString()
        this.isHistory = source.readInt() != 0
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mColorName)
        dest.writeInt(if (isHistory) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<ColorSuggestion> {
        override fun createFromParcel(parcel: Parcel): ColorSuggestion {
            return ColorSuggestion(parcel)
        }

        override fun newArray(size: Int): Array<ColorSuggestion?> {
            return arrayOfNulls(size)
        }
    }
}