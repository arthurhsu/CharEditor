package com.resomi.chareditor

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGImageView
import kotlin.math.max
import kotlin.math.abs

class PaintView : SVGImageView {
    private val W: Int = 512
    private var lastPt = Pt(0, 0)
    private var curGlyph = Glyph()
    private var curStroke = Stroke()
    private var lastStroke = curStroke
    private var previews = ArrayList<Preview>()

    constructor(ctx: Context) : super(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?): super(ctx, attrs, 0)
    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): super(ctx, attrs, defStyleAttr)

    fun associatePreview(preview: Preview) {
        previews.add(preview)
    }

    fun onCharChange() {
        curGlyph = Global.get().c.glyphs[0]
        curStroke = curGlyph.getFutureStroke()
        lastStroke = curStroke
        refresh()
    }

    fun refresh() {
        val svgml = SVGML()
        CanvasGuide.draw(svgml, W)
        curGlyph.render(svgml, false)

        val svgPreview = SVGML()
        svgPreview.elements.add(SVGMLBox(0, 0, W, W, Color.BLACK, false))
        curGlyph.render(svgPreview, true)
        try {
            var svg = SVG.getFromString(svgml.toString())
            this.setSVG(svg)
            svg = SVG.getFromString(svgPreview.toString())
            previews.forEach { it.setSVG(svg) }
        } catch (e: Exception) {
            Log.d("SVGCrash", svgml.toString())
        }
    }

    private fun toSVGCoordinates(x: Int, y: Int, rc: Rect): Pt {
        val cx: Int = x * W / rc.width()
        val cy: Int = y * W / rc.height()
        return Pt(cx, cy)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false

        val x = ev.x.toInt()
        val y = ev.y.toInt()

        val rc = Rect(0, 0, 0, 0)
        this.getGlobalVisibleRect(rc)

        when(Global.get().state) {
            State.Draw -> {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (lastStroke != curStroke) {
                            lastStroke.selected = false
                        }
                        lastPt = toSVGCoordinates(x, y, rc)
                        curStroke.addV(lastPt)
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val curPt = toSVGCoordinates(x, y, rc)
                        if (max(abs(curPt.x - lastPt.x), abs(curPt.y - lastPt.y)) > 30) {
                            lastPt = curPt
                            curStroke.addV(lastPt)
                        }
                    }

                    MotionEvent.ACTION_UP -> {
                        lastPt = toSVGCoordinates(x, y, rc)
                        curStroke.addV(lastPt)
                        lastStroke = curStroke
                        curStroke = curGlyph.commitFutureStroke()
                    }

                    else -> return false
                }
            }

            State.Edit -> {
                when (ev.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // TODO: select stroke
                    }

                    else -> return false
                }
            }

            else -> return false
        }
        refresh()
        return true
    }
}