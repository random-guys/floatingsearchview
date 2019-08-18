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

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ColorWrapper private constructor(`in`: Parcel) : Parcelable {

    /**
     *
     * @return
     * The hex
     */
    /**
     *
     * @param hex
     * The hex
     */
    @SerializedName("hex")
    @Expose
    var hex: String? = null
    /**
     *
     * @return
     * The name
     */
    /**
     *
     * @param name
     * The name
     */
    @SerializedName("name")
    @Expose
    var name: String? = null
    /**
     *
     * @return
     * The rgb
     */
    /**
     *
     * @param rgb
     * The rgb
     */
    @SerializedName("rgb")
    @Expose
    var rgb: String? = null

    init {
        hex = `in`.readString()
        name = `in`.readString()
        rgb = `in`.readString()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(hex)
        dest.writeString(name)
        dest.writeString(rgb)
    }

    override fun describeContents(): Int {
        return 0
    }

    init {
        hex = `in`.readString()
        name = `in`.readString()
        rgb = `in`.readString()
    }

    companion object CREATOR : Parcelable.Creator<ColorWrapper> {
        override fun createFromParcel(parcel: Parcel): ColorWrapper {
            return ColorWrapper(parcel)
        }

        override fun newArray(size: Int): Array<ColorWrapper?> {
            return arrayOfNulls(size)
        }
    }
}