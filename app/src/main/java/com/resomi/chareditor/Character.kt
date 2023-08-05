package com.resomi.chareditor

import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.InvalidObjectException
import java.lang.Exception
import java.net.URL

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

        fun load(s: String): Character {
            var ret = Character(s)
            val file = "${ret.code.substring(0, 1)}/${ret.code}.json"
            val url = "https://raw.githubusercontent.com/arthurhsu/calligraphy/master/data/$file"
            val thread = Thread {
                try {
                    val data = URL(url).readText()
                    val json = JSONObject(data)
                    ret = fromJSON(json)
                } catch (e: Exception) {
                    Log.e("CharEditor", e.toString())
                }
            }
            thread.start()
            thread.join()
            return ret
        }
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