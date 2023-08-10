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

            ret.currentStroke = Stroke()
            ret.futureStroke = ret.currentStroke
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
    private lateinit var futureStroke: Stroke
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

    fun commitStroke(): Stroke {
        strokes.add(currentStroke)
        currentStroke = Stroke()
        futureStroke = currentStroke
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
                currentStroke = s
                return true
            }
        }
        return false
    }

    fun deselect() {
        for (s in strokes) {
            s.selected = false
        }
        currentStroke = Stroke()
        futureStroke = currentStroke
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