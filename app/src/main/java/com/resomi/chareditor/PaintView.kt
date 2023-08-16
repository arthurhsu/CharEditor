package com.resomi.chareditor

import android.annotation.SuppressLint
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
    companion object {
        const val W: Int = 512
    }
    private var startPt = Pt(0, 0)
    private var lastPt = Pt(0, 0)
    private var previews = ArrayList<Preview>()
    private lateinit var viewModel: MainViewModel

    constructor(ctx: Context) : super(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?): super(ctx, attrs, 0)
    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): super(ctx, attrs, defStyleAttr)

    fun setViewModel(model: MainViewModel) {
        viewModel = model
    }

    fun associatePreview(preview: Preview) {
        previews.add(preview)
    }

    fun refresh() {
        val c = viewModel.charState.value
        if (c.isNada()) return

        val curGlyph = c.currentGlyph

        val svgml = SVGML()
        CanvasGuide.draw(svgml, W)
        curGlyph.render(svgml, false, viewModel.scopeState.value)

        val svgPreview = SVGML()
        svgPreview.elements.add(SVGMLBox(0, 0, W, W, Color.BLACK, false))
        curGlyph.render(svgPreview, true, viewModel.scopeState.value)
        try {
            var svg = SVG.getFromString(svgml.toString())
            this.setSVG(svg)
            svg = SVG.getFromString(svgPreview.toString())
            previews.forEach { it.setSVG(svg) }
        } catch (e: Exception) {
            Log.d(TAG, "SVG parser crash: ${e.toString()}")
            Log.d(TAG, svgml.toString())
        }
    }

    private fun toSVGCoordinates(x: Int, y: Int, rc: Rect): Pt {
        val cx: Int = x * W / rc.width()
        val cy: Int = y * W / rc.height()
        return Pt(cx, cy)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false

        val x = ev.x.toInt()
        val y = ev.y.toInt()

        val rc = Rect(0, 0, 0, 0)
        this.getGlobalVisibleRect(rc)

        return when(viewModel.scopeState.value) {
            Scope.Stroke -> strokeModeOnTouchEvent(ev, x, y, rc)
            Scope.Glyph -> glyphModeOnTouchEvent(ev, x, y, rc)
            else -> charModeOnTouchEvent(ev, x, y, rc)
        }
    }

    private fun strokeModeOnTouchEvent(ev: MotionEvent, x: Int, y: Int, rc: Rect): Boolean {
        if (viewModel.drawMode) {
            return drawModeOnTouchEvent(ev, x, y, rc)
        }

        val curStroke = viewModel.charState.value.currentGlyph.currentStroke
        if (curStroke.isEmpty()) {
            // Select stroke
            if (viewModel.charState.value.currentGlyph.select(toSVGCoordinates(x, y, rc))) {
                refresh()
                return true
            }
        }

        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                if (curStroke.toggleSelectControlPoint(toSVGCoordinates(x, y, rc))) {
                    lastPt = toSVGCoordinates(x, y, rc)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (curStroke.selectedControlPoint != -1) {
                    val curPt = toSVGCoordinates(x, y, rc)
                    if (max(abs(curPt.x - lastPt.x), abs(curPt.y - lastPt.y)) > 4) {
                        lastPt = curPt
                        curStroke.moveControlPointTo(lastPt, false)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (curStroke.selectedControlPoint != -1) {
                    val curPt = toSVGCoordinates(x, y, rc)
                    curStroke.moveControlPointTo(curPt, true)
                    curStroke.selectedControlPoint = -1
                }
            }
        }

        refresh()
        return true
    }

    private fun drawModeOnTouchEvent(ev: MotionEvent, x: Int, y: Int, rc: Rect): Boolean {
        val curGlyph = viewModel.charState.value.currentGlyph
        var curStroke = curGlyph.currentStroke
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                curStroke = curGlyph.deselectStrokes()
                lastPt = toSVGCoordinates(x, y, rc)
                curStroke.addV(lastPt)
                curStroke.selected = true
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
                curGlyph.commitStroke()
            }

            else -> return false
        }
        refresh()
        return true
    }

    private fun glyphModeOnTouchEvent(ev: MotionEvent, x: Int, y: Int, rc: Rect): Boolean {
        val g = viewModel.charState.value.currentGlyph
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                g.select(toSVGCoordinates(x, y, rc))
                if (g.hasSelectedStrokes() && viewModel.drawMode) {
                    g.snapshotStrokes()
                    lastPt = Pt(x, y)
                    startPt = Pt(x, y)
                }
                refresh()
            }

            MotionEvent.ACTION_MOVE -> {
                if (g.hasSelectedStrokes() && viewModel.drawMode) {
                    if (max(abs(x - lastPt.x), abs(y - lastPt.y)) > 4) {
                        val deltaX = x - lastPt.x
                        val deltaY = y - lastPt.y
                        g.moveStrokes(deltaX, deltaY)
                        lastPt = Pt(x, y)
                        refresh()
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (g.hasSelectedStrokes() && viewModel.drawMode) {
                    val deltaX = x - lastPt.x
                    val deltaY = y - lastPt.y
                    if (max(abs(x - startPt.x), abs(y - startPt.y)) > 4) {
                        g.moveStrokes(deltaX, deltaY)
                        g.recordMove()
                    }
                }
                startPt = Pt(0, 0)
                lastPt = Pt(0, 0)
                refresh()
            }
            else -> return false
        }
        return true
    }

    private fun charModeOnTouchEvent(ev: MotionEvent, x:Int, y: Int, rc: Rect): Boolean {
        return true
    }
}