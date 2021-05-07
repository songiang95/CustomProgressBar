package com.example.customprogressbar

import android.util.DisplayMetrics
import android.view.View

fun View.dp2px(dp: Float): Float {
    return dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}