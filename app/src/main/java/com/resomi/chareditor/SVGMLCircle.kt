package com.resomi.chareditor

class SVGMLCircle(id: String, private var cx: Float, private var cy: Float, color: Int, width: Int) :
    SVGMLElement(id, color, width) {

    override fun toString(): String {
        val commonAttr = getCommonAttributes()
        return "<circle $commonAttr r=\"8\" cx=\"${cx}\" cy=\"${cy}\" fill=\"none\"></circle>"
    }
}