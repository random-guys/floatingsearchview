package com.arlib.floatingsearchbiew.rxbinding

import androidx.annotation.CheckResult

import com.arlib.floatingsearchview.FloatingSearchView
import com.jakewharton.rxbinding2.InitialValueObservable

object RxFloatingSearchView {

    @CheckResult
    @JvmOverloads
    fun queryChanges(
            view: FloatingSearchView, characterLimit: Int = 1): InitialValueObservable<CharSequence> {
        checkNotNull(view, "view == null")
        return QueryObservable(view, characterLimit)
    }

    fun checkNotNull(value: Any?, message: String) {
        if (value == null) {
            throw NullPointerException(message)
        }
    }
}
