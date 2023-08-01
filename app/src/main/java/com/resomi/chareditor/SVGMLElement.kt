package com.resomi.chareditor

import android.graphics.Color

open class SVGMLElement(private val id: String, private val color: Int, private val width: Int) {

    fun getCommonAttributes(): String {
        var idString = ""
        if (id.isNotEmpty()) {
            idString = "id=\"${id}\""
        }
        var colorString = ""
        when (color) {
            Color.RED -> colorString = "red"
            Color.BLUE -> colorString = "blue"
            Color.GREEN -> colorString = "green"
        }
        return "$idString stroke=\"$colorString\" stroke-width=\"$width\""
    }
}