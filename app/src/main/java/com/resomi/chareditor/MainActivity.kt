package com.resomi.chareditor

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private lateinit var paintView: PaintView
    private lateinit var previewSmall: Preview
    private lateinit var previewLarge: Preview

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

        previewSmall = findViewById(R.id.preview_small)
        previewSmall.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewSmall)

        previewLarge = findViewById(R.id.preview_large)
        previewLarge.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewLarge)

        paintView.refresh()

        // Instantiate global object
        Global.get()
    }

    override fun onDestroy() {
        // TODO: save global state
        super.onDestroy()
    }
}