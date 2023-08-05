package com.resomi.chareditor

import android.content.Context
import android.util.AttributeSet
import com.caverock.androidsvg.SVGImageView

class Preview : SVGImageView {
    constructor(ctx: Context) : super(ctx, null)
    constructor(ctx: Context, attrs: AttributeSet?): super(ctx, attrs, 0)
    constructor(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int): super(ctx, attrs, defStyleAttr)
}