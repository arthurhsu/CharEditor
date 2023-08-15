package com.resomi.chareditor

import android.graphics.Color

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
            }
        }
        return "stroke=\"$colorString\" stroke-width=\"$width\""
    }
}