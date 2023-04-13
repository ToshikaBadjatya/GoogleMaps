package com.example.googlemaps

import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout

class ViewWrapper(val mapLayout: View) {
   fun setWeight(weight:Float)
   {
    val lp=mapLayout.layoutParams as LinearLayout.LayoutParams
    lp.weight=weight
   }
    fun getWeight():Float
    {
        val lp=mapLayout.layoutParams as LinearLayout.LayoutParams
        return lp.weight
    }

}
