package com.resomi.chareditor

class Glyph {
    private val strokes = ArrayList<Stroke>()
    private var futureStroke = Stroke()

    fun getFutureStroke(): Stroke {
        return futureStroke
    }

    fun commitFutureStroke(): Stroke {
        strokes.add(futureStroke)
        futureStroke = Stroke()
        return futureStroke
    }

    fun resetFutureStroke() {
        futureStroke = Stroke()
    }

    fun render(canvas: SVGML) {
        for (s in strokes) {
            s.render(canvas)
        }
        futureStroke.render(canvas)
    }
}