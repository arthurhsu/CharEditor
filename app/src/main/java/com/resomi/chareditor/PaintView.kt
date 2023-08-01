package com.resomi.chareditor

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.PictureDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.caverock.androidsvg.SVG

class PaintView : AppCompatImageView {
    private var canvas: SVGML = SVGML()

    constructor(ctx: Context) : super(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?): super(ctx, attrs, 0)
    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): super(ctx, attrs, defStyleAttr)

    fun refresh() {
        canvas.elements.clear()
        CanvasGuide.draw(canvas, 512)
        val svg = SVG.getFromString(canvas.toString())
        val drawable = PictureDrawable(svg.renderToPicture())
        this.setImageDrawable(drawable)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false

        val x = ev.x
        val y = ev.y

        val rc = Rect(0, 0, 0, 0)
        this.getGlobalVisibleRect(rc)

        when(ev.action) {
            MotionEvent.ACTION_DOWN -> {
                Log.i("D", "$x $y ($rc.left $rc.top $rc.right $rc.bottom)")
            }
            MotionEvent.ACTION_MOVE -> {
                Log.i("M", "$x $y")
            }
            MotionEvent.ACTION_UP -> {
                Log.i("U", "$x $y")
            }
            else -> return false
        }
        return true
    }
}