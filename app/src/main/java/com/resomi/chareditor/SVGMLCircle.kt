package com.resomi.chareditor

class SVGMLCircle(private var cx: Int, private var cy: Int, color: Int, width: Int) :
    SVGMLElement(color, width) {

    override fun toString(): String {
        val commonAttr = getCommonAttributes()
        return "<circle $commonAttr r=\"8\" cx=\"$cx\" cy=\"$cy\" fill=\"none\"></circle>"
    }
}