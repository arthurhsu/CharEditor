package com.resomi.chareditor

class SVGMLPath(id: String, color: Int, private var d: String) : SVGMLElement(id, color, 16) {
    companion object {
        const val stroke = "fill=\"none\" stroke-linecap=\"round\""
    }

    override fun toString(): String {
        val commonAttrs = getCommonAttributes()
        return "<path $commonAttrs $SVGMLPath.stroke d=\"$d\"></path>"
    }
}