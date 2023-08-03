package com.resomi.chareditor

import android.graphics.Color

open class SVGMLElement(private val color: Int, private val width: Int) {

    fun getCommonAttributes(): String {
        var colorString = ""
        when (color) {
            Color.RED -> colorString = "red"
            Color.BLUE -> colorString = "blue"
            Color.GREEN -> colorString = "green"
        }
        return "stroke=\"$colorString\" stroke-width=\"$width\""
    }
}