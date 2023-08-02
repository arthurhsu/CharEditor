package com.resomi.chareditor

import android.graphics.Color

class CanvasGuide() {
    companion object {
        fun box(width: Int, pct: Int): SVGMLElement {
            val l = width * (100 - pct) / 200
            val r = width - l
            return SVGMLBox("rc$pct", l, l, r, r, Color.RED)
        }

        fun line(x1: Int, y1: Int, x2: Int, y2: Int): SVGMLElement {
            return SVGMLLine(x1, y1, x2, y2, Color.RED)
        }
        fun draw(root: SVGML, width: Int) {
            val e = root.elements
            e.add(SVGMLBox("canvasFrame", 0, 0, width - 1, width - 1, Color.RED))
            e.add(box(width, 90))
            e.add(box(width, 80))
            e.add(box(width, 75))
            e.add(box(width, 50))
            e.add(line(0, width/2, width, width/2))
            e.add(line(width/2, 0, width/2, width))
            e.add(line(0, 0, width, width))
            e.add(line(width, 0, 0, width))
        }
    }
}