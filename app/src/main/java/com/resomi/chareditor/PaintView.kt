package com.resomi.chareditor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.resomi.chareditor.MainActivity.Companion.paintBrush
import com.resomi.chareditor.MainActivity.Companion.path

class PaintView : View {
    companion object {
        var pathList = ArrayList<Path>()
        var currentBrush = Color.BLUE
    }

    var params : ViewGroup.LayoutParams? = null

    constructor(ctx: Context) : super(ctx, null) {
        init()
    }

    constructor(ctx: Context, attrs: AttributeSet?): super(ctx, attrs, 0) {
        init()
    }

    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): super(ctx, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        paintBrush.isAntiAlias = true
        paintBrush.color = currentBrush
        paintBrush.style = Paint.Style.STROKE
        paintBrush.strokeJoin = Paint.Join.ROUND
        paintBrush.strokeWidth = 8f

        params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return false

        val x = ev.x
        val y = ev.y
        when(ev.action) {
            MotionEvent.ACTION_DOWN -> {
                path.moveTo(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                path.lineTo(x, y)
                pathList.add(path)
                return true
            }
            else -> return false
        }
        postInvalidate()
        return false
    }

    override fun onDraw(canvas: Canvas?) {
        if (canvas == null) return

        for (stroke in pathList) {
            paintBrush.setColor(Color.BLUE)
            canvas.drawPath(stroke, paintBrush)
        }
        invalidate()
    }
}