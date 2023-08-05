package com.resomi.chareditor

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var paintView: PaintView
    private lateinit var previewSmall: Preview
    private lateinit var previewLarge: Preview
    private lateinit var charInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Instantiate global object
        Global.get()

        // Initialize views
        paintView = findViewById(R.id.image_view)
        paintView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        previewSmall = findViewById(R.id.preview_small)
        previewSmall.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewSmall)

        previewLarge = findViewById(R.id.preview_large)
        previewLarge.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewLarge)

        paintView.refresh()

        charInfo = findViewById(R.id.char_info)

        // Initialize buttons
        val loadButton = findViewById<ImageButton>(R.id.load)
        loadButton.setOnClickListener {
            val builder = AlertDialog.Builder(it.context)
            val inflater = layoutInflater
            builder.setTitle(R.string.char_picker)
            val dialogLayout = inflater.inflate(R.layout.char_picker, null)
            val editText  = dialogLayout.findViewById<EditText>(R.id.edit_char)
            builder.setView(dialogLayout)
            builder.setPositiveButton("OK") { _, _ ->
                Global.load(editText.text.toString())
                val c = Global.get().c
                val badge = "${c.text} ${c.code}"
                charInfo.text = badge
                // TODO: render tags
                paintView.onCharChange()
            }
            builder.show()
        }

        val drawButton = findViewById<ImageButton>(R.id.draw)
        drawButton.setOnClickListener {
            Global.get().state = State.Draw
        }
    }

    override fun onDestroy() {
        // TODO: save global state
        super.onDestroy()
    }
}