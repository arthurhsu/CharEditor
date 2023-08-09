package com.resomi.chareditor

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class Glyph private constructor() : Op<Stroke>() {
    companion object {
        fun fromJSON(json: JSONObject): Glyph {
            val ret = Glyph()
            val tags = json.getJSONArray("tags")
            ret.tags.clear()
            for (i in 0 until tags.length()) {
                ret.tags.add(tags.getString(i))
            }
            if (tags.length() > 0) {
                ret.currentTag = ret.tags.first()
            } else {
                ret.currentTag = ""
            }
            val strokes = json.getJSONArray("strokes")
            for (i in 0 until strokes.length()) {
                ret.strokes.add(Stroke.fromJSON(strokes.getJSONObject(i)))
            }
            if (!ret.strokes.isEmpty()) {
                ret.currentStroke = ret.strokes.last()
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
    private val actionQueue = ActionQueue(this)

    lateinit var currentStroke: Stroke
    lateinit var futureStroke: Stroke
    var tags = HashSet<String>(listOf("楷"))
    var currentTag: String = "楷"

    fun clone(): Glyph {
        val ret = getEmpty()
        ret.strokes.addAll(strokes)
        ret.tags.addAll(tags)
        if (!ret.tags.isEmpty()) {
            ret.currentTag = tags.first()
        } else {
            ret.currentTag = ""
        }
        return ret
    }

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

    fun select(p: Pt): Boolean {
        for (s in strokes) {
            if (s.toggleSelect(p)) {
                Log.i(TAG, "select hit: ${p.x}, ${p.y}")
                return true
            }
        }
        return false
    }

    fun deselectToOne() {
        var count = 0
        for (i in strokes.indices.reversed()) {
            if (!strokes[i].selected) {
                continue
            }
            if (count > 0) {
                strokes[i].selected = false
            }
            count++
            currentStroke = strokes[i]
        }
        if (count == 0) {
            strokes.last().selected = true
            currentStroke = strokes.last()
        }
    }

    override fun add(target: ArrayList<Stroke>) {
        for (s in target) {
            add(s)
        }
    }

    fun add(s: Stroke) {
        strokes.add(s)
    }

    override fun remove(target: ArrayList<Stroke>) {
        for (s in target) {
            remove(s)
        }
    }

    fun remove(s: Stroke) {
        // TODO: implement, keep stroke ordering
    }

    override fun replace(target: ArrayList<Stroke>) {
        for (s in target) {
            replace(s)
        }
    }

    fun replace(s: Stroke) {
        // TODO: implement, keep stroke ordering
    }

    override fun undo() {
        actionQueue.undo()
    }

    override fun redo() {
        actionQueue.redo()
    }
}