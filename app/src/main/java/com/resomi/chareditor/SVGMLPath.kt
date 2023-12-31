package com.resomi.chareditor

class SVGMLPath(color: Int, val d: String) : SVGMLElement(color, 16) {
    companion object {
        const val stroke = "fill=\"none\" stroke-linecap=\"round\""
    }

    override fun toString(): String {
        val commonAttrs = getCommonAttributes()
        return "<path $commonAttrs $stroke d=\"$d\"></path>"
    }
}