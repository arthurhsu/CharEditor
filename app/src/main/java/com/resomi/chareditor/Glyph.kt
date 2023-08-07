package com.resomi.chareditor

import org.json.JSONArray
import org.json.JSONObject

class Glyph private constructor() {
    companion object {
        fun fromJSON(json: JSONObject): Glyph {
            val ret = Glyph()
            val tags = json.getJSONArray("tags")
            ret.tags.clear()
            for (i in 0 until tags.length()) {
                ret.tags.add(tags.getString(i))
            }
            val strokes = json.getJSONArray("strokes")
            for (i in 0 until strokes.length()) {
                ret.strokes.add(Stroke.fromJSON(strokes.getJSONObject(i)))
            }
            if (strokes.length() > 0) {
                ret.currentStroke = ret.strokes[strokes.length() - 1]
                ret.futureStroke = ret.currentStroke
            } else {
                ret.currentStroke = Stroke()
                ret.futureStroke = ret.currentStroke
            }
            return ret
        }

        fun getEmpty(): Glyph {
            val ret = Glyph()
            ret.currentStroke = Stroke()
            ret.futureStroke = ret.currentStroke
            return ret
        }
    }

    fun toJSON(): JSONObject {
        val ret = JSONObject()
        ret.put("tags", JSONArray(tags.toTypedArray()))
        val strokeJSON = ArrayList<JSONObject>()
        for (s in strokes) {
            strokeJSON.add(s.toJSON())
        }
        ret.put("strokes", JSONArray(strokeJSON))
        return ret
    }

    private val strokes = ArrayList<Stroke>()

    lateinit var currentStroke: Stroke
    lateinit var futureStroke: Stroke
    var tags = HashSet<String>(listOf("楷"))
    var currentTag: String = "楷"

    fun isEmpty(): Boolean {
        return strokes.isEmpty()
    }

    fun commitFutureStroke(): Stroke {
        strokes.add(futureStroke)
        futureStroke = Stroke()
        return futureStroke
    }

    fun render(canvas: SVGML, preview: Boolean, scope: Scope) {
        if (isEmpty()) return
        strokes.forEach { it.render(canvas, preview, scope) }
        futureStroke.render(canvas, preview, scope)
    }
}