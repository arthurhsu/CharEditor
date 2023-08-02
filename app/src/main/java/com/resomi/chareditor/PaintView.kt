package com.resomi.chareditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import java.lang.Exception

class PaintView : SVGImageView {
    private var list = ArrayList<SVGMLElement>()
    private val W: Int = 512

    constructor(ctx: Context) : super(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?): super(ctx, attrs, 0)
    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): super(ctx, attrs, defStyleAttr)

    fun refresh() {
        val svgml = SVGML()
        CanvasGuide.draw(svgml, W)
        svgml.elements.addAll(list)
        try {
            val svg = SVG.getFromString(svgml.toString())
            this.setSVG(svg)
        } catch (e: Exception) {
            Log.d("SVGCrash", svgml.toString())
        }
    }

    private fun genCircle(x: Float, y: Float, rc: Rect): SVGMLCircle {
        Log.i("", "$x $y $W ${rc.width()}")
        val cx = x * W / rc.width()
        val cy = y * W / rc.height()
        Log.i("", "Adding circle $cx $cy")
        return SVGMLCircle("", cx, cy, Color.RED, 3)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        Log.i("", "Actual refresh")
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false

        val x = ev.x
        val y = ev.y

        val rc = Rect(0, 0, 0, 0)
        this.getGlobalVisibleRect(rc)

        when(ev.action) {
            MotionEvent.ACTION_DOWN -> {
                list.add(genCircle(x, y, rc))
            }
            MotionEvent.ACTION_MOVE -> {
                Log.i("M", "$x $y")
            }
            MotionEvent.ACTION_UP -> {
                Log.i("U", "$x $y")
            }
            else -> return false
        }
        refresh()
        return true
    }
}