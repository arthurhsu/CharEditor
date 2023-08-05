package com.resomi.chareditor

class SVGMLBox(x1: Int, y1: Int, x2: Int, y2: Int, c: Int, dash: Boolean = true) :
        SVGMLElement(c, 1) {
    private var stroke = "fill=\"none\""
    init {
        if (dash) {
            stroke += " stroke-dasharray=\"5.5\""
        }
    }
    private var d: String = ""

    init {
        this.d = "M$x1 $y1 L$x2 $y1 $x2 $y2 $x1 $y2 $x1 $y1"
    }

    override fun toString(): String {
        val commonAttrs = getCommonAttributes()
        return "<path $commonAttrs ${stroke} d=\"$d\"></path>"
    }
}
