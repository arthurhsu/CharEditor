package com.resomi.chareditor

import org.json.JSONObject
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class CharEditorUnitTest {
    @Test
    fun JSON_serialization() {
        val sample =
            "{\"code\":\"4E8C\",\"text\":\"二\"," +
             "\"glyphs\":[{\"tags\":[\"楷\"]," +
             "\"strokes\":[{\"dots\":[167,187,228,195,289,179,332,162]," +
             "\"splines\":[\"M 167 187 C 186.93 191.78 206.87 196.56 228 195\"," +
             "\"M 228 195 C 249.13 193.44 271.47 185.56 289 179\"," +
             "\"M 289 179 C 306.53 172.44 319.27 167.22 332 162\"]}," +
             "{\"dots\":[66,358,129,345,191,333,256,321,319,308,390,305,443,321]," +
             "\"splines\":[\"M 66 358 C 87.20 353.59 108.39 349.19 129 345\"," +
             "\"M 129 345 C 149.61 340.81 169.63 336.85 191 333\"," +
             "\"M 191 333 C 212.37 329.15 235.08 325.41 256 321\"," +
             "\"M 256 321 C 276.92 316.59 296.06 311.50 319 308\"," +
             "\"M 319 308 C 341.94 304.50 368.70 302.57 390 305\"," +
             "\"M 390 305 C 411.30 307.43 427.15 314.21 443 321\"]}]}]}"
        val json = JSONObject(sample)
        val c = Character.fromJSON(json)
        assertEquals("二", c.text)
        assertEquals("4E8C", c.code)
        assertEquals(1, c.glyphs.size)
        assertEquals(1, c.glyphs[0].tags.size)

        val jsonString = c.toJSON().toString()
        val json2 = JSONObject(jsonString)
        val c2 = Character.fromJSON(json2)
        assertEquals(jsonString, c2.toJSON().toString())
    }
}