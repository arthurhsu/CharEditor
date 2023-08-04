package com.resomi.chareditor

import android.graphics.Color
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat

class Stroke {
    companion object {
        fun fromJSON(json: JSONObject): Stroke {
            val ret = Stroke()
            val dots = json.getJSONArray("dots")
            for (i in 0 until dots.length() step 2) {
                ret.vertices.add(Pt(dots.getInt(i), dots.getInt(i+1)))
            }
            val ds = json.getJSONArray("splines")
            for (i in 0 until ds.length()) {
                ret.splines.add(SVGMLPath(Color.BLUE, ds.getString(i)))
            }
            return ret
        }
    }

    fun toJSON() : JSONObject {
        val ret = JSONObject()
        val dots = ArrayList<Int>()
        for (v in vertices) {
            dots.add(v.x)
            dots.add(v.y)
        }
        ret.put("dots", JSONArray(dots))
        val m = ArrayList<String>()
        for (s in splines) {
            m.add(s.d)
        }
        ret.put("splines", JSONArray(m))
        return ret
    }

    private val vertices = ArrayList<Pt>()
    private var splines = ArrayList<SVGMLPath>()
    private val df = DecimalFormat("#.##")
    var activated = true

    // Compute control points
    private fun computeControlPoints(v: ArrayList<Int>): Pair<FloatArray, FloatArray> {
        Log.i("", "cp $v")
        val n = v.size - 1
        val p1 = FloatArray(n)
        val p2 = FloatArray(n)
        val a = FloatArray(n)
        val b = FloatArray(n)
        val c = FloatArray(n)
        val r = FloatArray(n)

        // left most segment
        a[0] = 0F
        b[0] = 2F
        c[0] = 1F
        r[0] = v[0] + 2F * v[1]

        // internal segments
        for (i in 1 until n-1)	{
            a[i] = 1F
            b[i] = 4F
            c[i] = 1F
            r[i] = 4F * v[i] + 2F * v[i + 1]
        }

        // right segment
        a[n - 1] = 2F
        b[n - 1] = 7F
        c[n - 1] = 0F
        r[n - 1] = 8F * v[n - 1] + v[n]

        // solves Ax=b with the Thomas algorithm (from Wikipedia)
        for (i in 1 until n) {
            val m = a[i] / b[i - 1]
            b[i] = b[i] - m * c[i - 1]
            r[i] = r[i] - m * r[i - 1]
        }

        p1[n - 1] = r[n - 1] / b[n - 1]
        for (i in n - 2 downTo 0) {
            p1[i] = (r[i] - c[i] * p1[i + 1]) / b[i]
        }

        // we have p1, now compute p2
        for (i in 0 until n - 1) {
            p2[i] = 2 * v[i + 1] - p1[i + 1]
        }
        p2[n - 1] = 0.5F * (v[n] + p1[n - 1])
        return Pair(p1, p2)
    }

    private fun r(x: Float): String {
        return df.format(x)
    }

    // Get D string for SVG cubic path element
    private fun getD(
            x1: Int, y1: Int, px1: Float, py1: Float,
            px2: Float, py2: Float, x2: Int, y2: Int): String {
        return "M $x1 $y1 C ${r(px1)} ${r(py1)} ${r(px2)} ${r(py2)} $x2 $y2"
    }

    private fun updateSplines() {
        if (vertices.size < 2) return

        val x = ArrayList<Int>()
        val y = ArrayList<Int>()
        for (v in vertices) {
            x.add(v.x)
            y.add(v.y)
        }

        val px = computeControlPoints(x)
        val py = computeControlPoints(y)
        splines.clear()
        var color: Int = Color.BLUE
        if (activated) color = Color.GREEN
        for (i in 0 until vertices.size - 1) {
            val d = getD(x[i], y[i], px.first[i], py.first[i], px.second[i], py.second[i], x[i+1], y[i+1])
            splines.add(SVGMLPath(color, d))
        }
    }

    fun isEmpty(): Boolean {
        return vertices.isEmpty()
    }

    fun addV(v: Pt) {
        vertices.add(v)
    }

    fun render(canvas: SVGML) {
        if (isEmpty()) return

        updateSplines()
        canvas.elements.addAll(splines)
        if (!activated) return

        for (v in vertices) {
            canvas.elements.add(SVGMLCircle(v.x, v.y, Color.RED, 3))
        }
    }
}