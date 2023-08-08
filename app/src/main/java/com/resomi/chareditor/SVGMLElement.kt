package com.resomi.chareditor

import android.graphics.Color
import android.util.Log

open class SVGMLElement(private val color: Int, private val width: Int) {

    fun getCommonAttributes(): String {
        val colorString: String
        when (color) {
            Color.BLACK -> colorString = "black"
            Color.RED -> colorString = "red"
            Color.BLUE -> colorString = "blue"
            Color.GREEN -> colorString = "green"
            else -> {
                colorString = "#${String.format("%08x", color).substring(2)}"
                Log.i(TAG, "custom color: $colorString")
            }
        }
        return "stroke=\"$colorString\" stroke-width=\"$width\""
    }
}