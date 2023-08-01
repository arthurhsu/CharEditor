package com.resomi.chareditor

class SVGMLCircle(id: String, private var cx: Int, private var cy: Int, color: Int, width: Int) :
    SVGMLElement(id, color, width) {

    override fun toString(): String {
        val commonAttr = getCommonAttributes()
        return "<circle $commonAttr cx=\"${cx}\" cy=\"${cy}\" fill=\"none\"></circle>"
    }
}