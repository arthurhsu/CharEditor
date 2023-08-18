package com.resomi.chareditor

import org.json.JSONArray
import org.json.JSONObject
import java.io.InvalidObjectException

class Character private constructor(val text: String) : ActionQueue<Glyph>() {
    companion object {
        fun fromJSON(json: JSONObject): Character {
            val text = json.getString("text")
            val ret = Character(text)
            if (ret.code != json.getString("code")) {
                throw InvalidObjectException("Data corrupted")
            }
            val glyphArray = json.getJSONArray("glyphs")
            ret.glyphs.clear()
            for (i in 0 until glyphArray.length()) {
                ret.glyphs.add(Glyph.fromJSON(glyphArray.getJSONObject(i)))
            }
            ret.index = 0
            return ret
        }

        fun nada(): Character {
            return Character("0")
        }

        fun getNew(s: String): Character {
            val ret = Character(s)
            ret.glyphs.add(Glyph.getEmpty())
            ret.select(0)
            return ret
        }

        fun getCode(s: String): String {
            return s.codePointAt(0).toString(16).uppercase()
        }
    }

    private val glyphs = ArrayList<Glyph>()
    private var index = -1

    fun isNada(): Boolean {
        return text == "0"
    }

    fun toJSON(): JSONObject {
        val ret = JSONObject()
        ret.put("code", code)
        ret.put("text", text)
        val glyphArray = ArrayList<JSONObject>()
        for (g in glyphs) {
            glyphArray.add(g.toJSON())
        }
        ret.put("glyphs", JSONArray(glyphArray))
        return ret
    }

    val code = getCode(text)
    val currentGlyph get() = this.glyphs[this.index]
    val currentIndex get() = this.index
    val size get() = this.glyphs.size

    fun select(i: Int) {
        if (i == index || i < 0 || i >= glyphs.size) return
        index = i
    }

    override fun add(index: Int, target: Glyph, rec: Boolean) {
        super.add(index, target, rec)
        glyphs.add(index, target)
    }

    override fun remove(index: Int, target: Glyph, rec: Boolean) {
        super.remove(index, target, rec)
        glyphs.removeAt(index)
    }

    override fun replace(index: Int, target: Glyph, original: Glyph, rec: Boolean) {
        super.replace(index, target, original, rec)
        glyphs[index] = target
    }
}