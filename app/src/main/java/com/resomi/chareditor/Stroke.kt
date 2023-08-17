package com.resomi.chareditor

import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class Stroke : ActionQueue<Pt>() {
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
            ret.selected = false
            return ret
        }

        const val THRESHOLD = 4
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
    var selected = true
    var selectedControlPoint = -1
    var curPt = Pt(0, 0)

    fun clone(): Stroke {
        val ret = Stroke()
        ret.vertices.addAll(vertices)
        return ret
    }

    // Compute control points
    private fun computeControlPoints(v: ArrayList<Int>): Pair<FloatArray, FloatArray> {
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

    private fun updateSplines(preview: Boolean, scope: Scope) {
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
        if (selected && !preview) {
            when (scope) {
                Scope.Stroke -> {
                    color = Color.GREEN
                }
                Scope.Glyph -> {
                    color = Color.argb(0, 150, 75, 0)
                }
                else -> {
                    // Do nothing, keep it blue
                }
            }
        }
        for (i in 0 until vertices.size - 1) {
            val d = getD(x[i], y[i], px.first[i], py.first[i],
                         px.second[i], py.second[i], x[i+1], y[i+1])
            splines.add(SVGMLPath(color, d))
        }
    }

    fun isEmpty(): Boolean {
        return vertices.isEmpty()
    }

    fun addV(v: Pt) {
        vertices.add(v)
    }

    fun render(canvas: SVGML, preview: Boolean, scope: Scope) {
        if (isEmpty()) return

        updateSplines(preview, scope)
        canvas.elements.addAll(splines)
        if (!selected || preview || scope != Scope.Stroke) return

        for (i in 0 until vertices.size) {
            val v = vertices[i]
            var color = Color.RED
            if (i == selectedControlPoint) {
                color = Color.BLUE
            }
            canvas.elements.add(SVGMLCircle(v.x, v.y, color, 3))
        }
    }

    private fun isClosedTo(p1: Pt, p2: Pt, p: Pt): Boolean {
        val x0 = p.x
        val y0 = p.y
        val x1 = p1.x
        val y1 = p1.y
        val x2 = p2.x
        val y2 = p2.y
        val rc = Rect(x1.coerceAtMost(x2), y1.coerceAtMost(y2),
                      x1.coerceAtLeast(x2), y1.coerceAtLeast(y2))
        if (rc.contains(Rect(x0, y0, x0, y0))) {
            val d = abs((x2-x1)*(y1-y0)-(x1-x0)*(y2-y1)) /
                    sqrt((x2 - x1).toDouble().pow(2.0) + (y2 - y1).toDouble().pow(2.0))
            if (d < THRESHOLD) {
                return true
            }
        }
        return false
    }

    fun toggleSelect(p: Pt): Boolean {
        for (i in 0 until vertices.size - 1) {
            if (isClosedTo(vertices[i], vertices[i+1], p)) {
                Log.d(TAG, "toggle select: stroke $i")
                selected = !selected
                if (selected) {
                    toggleSelectControlPoint(p)
                }
                return true
            }
        }
        return false
    }

    fun toggleSelectControlPoint(p: Pt): Boolean {
        for (i in 0 until vertices.size) {
            val v = vertices[i]
            val rc = Rect(v.x - THRESHOLD, v.y - THRESHOLD,
                v.x + THRESHOLD, v.y + THRESHOLD)
            if (rc.contains(p.x, p.y)) {
                selectedControlPoint = if (selectedControlPoint == i) {
                    -1
                } else {
                    i
                }
                if (selectedControlPoint != -1) {
                    curPt = vertices[selectedControlPoint]
                }
                return true
            }
        }
        return false
    }

    fun moveControlPointTo(p: Pt, end: Boolean) {
        if (selectedControlPoint == -1) return
        vertices[selectedControlPoint] = p
        if (end) {
            replace(selectedControlPoint, p, curPt, true)
            curPt = p
        }
    }

    fun move(deltaX: Int, deltaY: Int) {
        for (i in 0 until vertices.size) {
            vertices[i] = Pt(vertices[i].x + deltaX, vertices[i].y + deltaY)
        }
    }

    override fun toString() : String {
        if (vertices.isEmpty()) {
            return "NaN-NaN"
        }
        return "${vertices[0].toString()}-${vertices[vertices.size-1].toString()}"
    }

    fun addControlPoint() {
        if (!selected || vertices.size < 2) return

        val index = vertices.size - 1
        val pt1 = vertices[index]
        val pt = Pt(pt1.x + 30, pt1.y + 30)
        add(index + 1, pt, true)
    }

    override fun add(index: Int, target: Pt, rec: Boolean) {
        super.add(index, target, rec)
        vertices.add(index, target)
    }

    override fun replace(index: Int, target: Pt, original: Pt, rec: Boolean) {
        super.replace(index, target, original, rec)
        vertices[index] = target
    }

    override fun remove(index: Int, target: Pt, rec: Boolean) {
        super.remove(index, target, rec)
        vertices.removeAt(index)
    }

    private fun transform(v: Int, pct: Int): Int {
        return (256F + (v.toFloat() - 256F) * (pct.toFloat() / 100F)).toInt()
    }

    fun zoom(pct: Int, zoomX: Boolean, zoomY: Boolean) {
        for (i in 0 until vertices.size) {
            var x = vertices[i].x
            var y = vertices[i].y
            if (zoomX) x = transform(x, pct)
            if (zoomY) y = transform(y, pct)
            vertices[i] = Pt(x, y)
        }
    }

    fun rotate(deg: Int) {
        for (i in 0 until vertices.size) {
            val v = vertices[i]
            val cosT = cos(deg.toDouble() * Math.PI / 100.0)
            val sinT = sin(deg.toDouble() * Math.PI / 100.0)
            var x = v.x
            var y = v.y
            x = ((x.toDouble() - 256.0) * cosT - (y.toDouble() - 256.0) * sinT + 256.0).toInt()
            y = ((x.toDouble() - 256.0) * sinT + (y.toDouble() - 256.0) * cosT + 256.0).toInt()
            vertices[i] = Pt(x, y)
        }
    }
}