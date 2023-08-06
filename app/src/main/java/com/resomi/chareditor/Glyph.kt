package com.resomi.chareditor

import org.json.JSONArray
import org.json.JSONObject

class Glyph {
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
            return ret
        }
    }

    fun toJSON(): JSONObject {
        val ret = JSONObject()
        ret.put("tags", JSONArray(tags))
        val strokeJSON = ArrayList<JSONObject>()
        for (s in strokes) {
            strokeJSON.add(s.toJSON())
        }
        ret.put("strokes", JSONArray(strokeJSON))
        return ret
    }

    private val strokes = ArrayList<Stroke>()
    private var futureStroke = Stroke()
    var tags = ArrayList<String>()

    init {
        tags.add("楷")
    }

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

    fun render(canvas: SVGML, preview: Boolean, scope: Scope) {
        strokes.forEach { it.render(canvas, preview, scope) }
        futureStroke.render(canvas, preview, scope)
    }
}