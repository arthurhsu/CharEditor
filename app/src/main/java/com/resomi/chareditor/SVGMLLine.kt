package com.resomi.chareditor

class SVGMLLine(
    private var x1: Int,
    private var y1: Int,
    private var x2: Int,
    private var y2: Int,
    color: Int
) : SVGMLElement("", color, 1) {
    companion object {
        const val dash = "stroke-dasharray=\"5,5\""
    }

    override fun toString(): String {
        val commonAttr = getCommonAttributes()
        return "<line x1=\"$x1\" y1=\"$y1\" x2=\"$x2\" y2=\"$y2\" $commonAttr $dash></line>"
    }
}