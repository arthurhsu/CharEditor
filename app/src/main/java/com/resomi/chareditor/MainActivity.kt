package com.resomi.chareditor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var paintView: PaintView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        val drawButton = findViewById<ImageButton>(R.id.draw)

        drawButton.setOnClickListener {
            Toast.makeText(this, "clicked!", Toast.LENGTH_SHORT).show()
        }

        paintView = findViewById(R.id.image_view)
        paintView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.refresh()
    }
}