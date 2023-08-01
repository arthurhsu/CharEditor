package com.resomi.chareditor

class SVGML {
    companion object {
        const val xmlns = "http://www.w3.org/2000/svg"
        const val xlink = "http://www.w3.org/1999/xlink"
    }

    private var id = "pad"
    private val viewbox = "0 0 512 512"
    private val style = "width:512px; height:512px; border:1px solid red"
    var elements = ArrayList<SVGMLElement>()

    override fun toString(): String {
        val sb = StringBuilder("<svg ")
        sb.append("xmlns=\"$xmlns\" ")
        sb.append("xmlns:xlink=\"$xlink\" ")
        sb.append("id=\"$id\" ")
        sb.append("viewbox=\"$viewbox\" ")
        sb.append("style=\"$style\">")
        for (e in elements) {
            sb.append(e.toString())
        }
        sb.append("</svg>")
        return sb.toString()
    }
}