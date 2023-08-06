package com.resomi.chareditor

import org.json.JSONArray
import org.json.JSONObject
import java.io.InvalidObjectException

class Character(val text: String) {
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
            return ret
        }

        fun nada(): Character {
            return Character("0")
        }

        fun getCode(s: String): String {
            return s.codePointAt(0).toString(16).uppercase()
        }
    }

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
    val glyphs = ArrayList<Glyph>()
}