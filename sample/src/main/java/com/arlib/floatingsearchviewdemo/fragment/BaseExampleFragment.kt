package com.arlib.floatingsearchviewdemo.fragment

import android.content.Context
import androidx.fragment.app.Fragment

import com.arlib.floatingsearchview.FloatingSearchView

/**
 * Created by ari on 8/16/16.
 */
abstract class BaseExampleFragment : Fragment() {


    private var mCallbacks: BaseExampleFragmentCallbacks? = null

    interface BaseExampleFragmentCallbacks {

        fun onAttachSearchViewToDrawer(searchView: FloatingSearchView)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is BaseExampleFragmentCallbacks) {
            mCallbacks = context
        } else {
            throw RuntimeException("$context must implement BaseExampleFragmentCallbacks")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
    }

    protected fun attachSearchViewActivityDrawer(searchView: FloatingSearchView) {
        if (mCallbacks != null) {
            mCallbacks!!.onAttachSearchViewToDrawer(searchView)
        }
    }

    abstract fun onActivityBackPress(): Boolean
}
