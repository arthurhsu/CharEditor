package com.resomi.chareditor

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.InvalidAlgorithmParameterException

class Glyph private constructor() : ActionQueue<Stroke>() {
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
    private val snapshot = HashMap<Int, Stroke>()

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
        add(strokes.size, currentStroke, true)
        currentStroke = Stroke()
        futureStroke = currentStroke
        return futureStroke
    }

    fun getSelectedStroke(): Stroke {
        if (currentStroke != futureStroke) {
            return currentStroke
        } else {
            val ret = strokes[strokes.size - 1]
            ret.selected = true
            currentStroke = ret
            return ret
        }
    }

    fun render(canvas: SVGML, preview: Boolean, scope: Scope) {
        if (isEmpty()) return
        strokes.forEach { it.render(canvas, preview, scope) }
        futureStroke.render(canvas, preview, scope)
    }

    fun select(p: Pt): Boolean {
        for (s in strokes) {
            if (s.toggleSelect(p)) {
                Log.i(TAG, "select hit: ${p.x}, ${p.y} ${s.selected}")
                if (s.selected) {
                    currentStroke = s
                }
                return true
            }
        }
        return false
    }

    fun hasSelectedStrokes() : Boolean {
        return strokes.any { it.selected }
    }

    fun deselectStrokes() : Stroke {
        for (s in strokes) {
            s.selected = false
        }
        currentStroke = Stroke()
        futureStroke = currentStroke
        futureStroke.selected = true
        return futureStroke
    }

    fun snapshotStrokes() {
        snapshot.clear()
        for (i in 0 until strokes.size) {
            if (strokes[i].selected) {
                snapshot.set(i, strokes[i].clone())
            }
        }
    }

    fun moveStrokes(deltaX: Int, deltaY: Int) {
        for (s in strokes) {
            if (s.selected) {
                s.move(deltaX, deltaY)
            }
        }
    }

    fun recordMove() {
        for (entry in snapshot) {
            super.replace(entry.key, strokes[entry.key], entry.value, true)
        }
        snapshot.clear()
    }

    override fun toString(): String {
        return "strokes: ${strokes.size} tags: ${tags.toString()}"
    }

    override fun add(index: Int, target: Stroke, rec: Boolean) {
        if (index == strokes.size) {
            strokes.add(target)
        } else {
            strokes.add(index, target)
        }
        super.add(index, target, rec)
    }

    override fun remove(index: Int, target: Stroke, rec: Boolean) {
        if (index < 0 || index >= strokes.size) {
            throw InvalidAlgorithmParameterException("remove stroke $index out of range")
        }
        strokes.removeAt(index)
        super.remove(index, target, rec)
    }

    override fun replace(index: Int, target: Stroke, original: Stroke, rec: Boolean) {
        strokes[index] = target
        super.replace(index, target, original, rec)
    }

    fun rotateSelectedStrokes(deg: Int) {
        snapshotStrokes()
        for (i in 0 until strokes.size) {
            val s = strokes[i]
            if (s.selected) {
                s.rotate(deg)
                snapshot[i]?.let { super.replace(i, s, it, true) }
            }
        }
    }

    fun zoomSelectedStrokes(pct: Int, zoomX: Boolean, zoomY: Boolean) {
        snapshotStrokes()
        for (i in 0 until strokes.size) {
            val s = strokes[i]
            if (s.selected) {
                s.zoom(pct, zoomX, zoomY)
                snapshot[i]?.let { super.replace(i, s, it, true) }
            }
        }
    }

    fun removeSelectedStrokes() {
        for (i in strokes.size - 1 downTo 0) {
            if (strokes[i].selected) {
                remove(i, strokes[i], true)
            }
        }
    }
}