package com.resomi.chareditor

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity() {
    private lateinit var paintView: PaintView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        // Instantiate global object
        Global.get()

        // Initialize views
        paintView = findViewById(R.id.image_view)
        paintView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        val previewSmall: Preview = findViewById(R.id.preview_small)
        previewSmall.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewSmall)

        val previewLarge: Preview = findViewById(R.id.preview_large)
        previewLarge.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewLarge)

        paintView.refresh()

        val charInfo: TextView = findViewById(R.id.char_info)

        // Initialize buttons
        val loadButton = findViewById<Button>(R.id.load)
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

        val scopeGroup = findViewById<MaterialButtonToggleGroup>(R.id.scope)
        scopeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                if (Global.get().c.isNada() && isChecked && checkedId != R.id.btn_char) {
                    scopeGroup.check(R.id.btn_char)
                } else {
                    when (checkedId) {
                        R.id.btn_char -> Global.get().scope = Scope.Char
                        R.id.btn_glyph -> Global.get().scope = Scope.Glyph
                        else -> Global.get().scope = Scope.Stroke
                    }
                    Log.d("CharEditor", "scope ${Global.get().scope}")
                }
            }
        }
    }

    override fun onDestroy() {
        // TODO: save global state
        super.onDestroy()
    }
}
