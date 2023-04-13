package com.example.googlemaps.utils

import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout

class ViewAnimator {
    private lateinit var view: View
    constructor(v: View)
    {
        if(view.layoutParams is LinearLayout.LayoutParams)
        {
            view=v
        }
        else
        {
            throw Exception("Parent should be a linear layout")
        }
    }
    public fun setWeight(w:Float)
    {
     val lp=view.layoutParams as LinearLayout.LayoutParams
        lp.weight=w
    }
    public fun getWeight():Float
    {
        val lp=view.layoutParams as LinearLayout.LayoutParams
      return lp.weight
    }
}