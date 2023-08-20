package com.resomi.chareditor

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var paintView: PaintView
    private lateinit var viewModel: MainViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var listTags: ListView
    private lateinit var spinnerGlyph: Spinner
    private lateinit var drawModeCheck: CheckBox
    private lateinit var listTagsAdapter: ArrayAdapter<String>
    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private lateinit var deleteMode: TextView

    private val loginLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) {
        res -> this.onLoginResult(res)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        auth = Firebase.auth

        // Instantiate view model
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                merge(viewModel.charState, viewModel.scopeState, viewModel.msgState).collectLatest {
                    if (it is Character) {
                        Log.i(TAG, "char change ${it.text}")
                        onCharChange()
                    }
                    if (it is Scope) {
                        Log.i(TAG, "scope change ${it.toString()}")
                        onScopeChange()
                    }
                    if (it is String) {
                        onRequestToast(it)
                    }
                }
            }
        }

        // Initialize views
        paintView = findViewById(R.id.image_view)
        paintView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.setViewModel(viewModel)

        val previewSmall: Preview = findViewById(R.id.preview_small)
        previewSmall.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewSmall)

        val previewLarge: Preview = findViewById(R.id.preview_large)
        previewLarge.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        paintView.associatePreview(previewLarge)

        paintView.refresh()

        // Initialize buttons
        val loginButton = findViewById<Button>(R.id.login)
        loginButton.setOnClickListener { onLoginClick() }

        val scopeGroup = findViewById<MaterialButtonToggleGroup>(R.id.scope)
        scopeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            onScopeGroupChecked(scopeGroup, checkedId, isChecked)
        }

        val tagButton = findViewById<Button>(R.id.btn_tag)
        tagButton.setOnClickListener {
            if (viewModel.scopeState.value == Scope.Glyph) {
                onTag(it)
            } else {
                invalidClick(it)
            }
        }

        val loadButton = findViewById<Button>(R.id.load)
        loadButton.setOnClickListener { onLoadClick(it) }

        val saveButton = findViewById<Button>(R.id.save)
        saveButton.setOnClickListener { onSaveClick(it) }

        drawModeCheck = findViewById<CheckBox>(R.id.draw_mode)
        drawModeCheck.setOnCheckedChangeListener { _, isChecked ->
            viewModel.drawMode = isChecked
            if (isChecked && viewModel.scopeState.value == Scope.Stroke) {
                viewModel.charState.value.currentGlyph.deselectStrokes()
                paintView.refresh()
            }
        }

        val addButton = findViewById<Button>(R.id.add)
        addButton.setOnClickListener {
            when (viewModel.scopeState.value) {
                Scope.Char -> onAddGlyph(it)
                Scope.Stroke -> onAddControlPoint(it)
                else -> invalidClick(it)
            }
        }

        val deleteButton = findViewById<Button>(R.id.delete)
        deleteButton.setOnClickListener {
            when (viewModel.scopeState.value) {
                Scope.Char -> onDeleteGlyph(it)
                Scope.Glyph -> onDeleteStrokes(it)
                Scope.Stroke -> onDeleteControlPoint(it)
            }
        }

        val rotateButton = findViewById<Button>(R.id.rotate)
        rotateButton.setOnClickListener {
            if (viewModel.scopeState.value != Scope.Glyph) {
                invalidClick(it)
            } else {
                onRotateStrokes(it)
            }
        }

        val zoomButton = findViewById<Button>(R.id.zoom)
        zoomButton.setOnClickListener {
            if (viewModel.scopeState.value != Scope.Glyph) {
                invalidClick(it)
            } else {
                onZoomStrokes(it)
            }
        }

        val undoButton = findViewById<Button>(R.id.undo)
        undoButton.setOnClickListener { onUndoClick(it) }

        val redoButton = findViewById<Button>(R.id.redo)
        redoButton.setOnClickListener { onRedoClick(it) }

        val importButton = findViewById<Button>(R.id.import_from)
        importButton.setOnClickListener {
            if (viewModel.scopeState.value != Scope.Glyph) {
                invalidClick(it)
            } else {
                onImportGlyph(it)
            }
        }

        listTags = findViewById(R.id.tag_box)
        listTags.setOnItemClickListener { _, _, _, _ -> }

        spinnerGlyph = findViewById<Spinner>(R.id.glyph_spinner)
        spinnerGlyph.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(p0: AdapterView<*>?) {
                viewModel.charState.value.select(0)
                paintView.refresh()
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.charState.value.select(position)
                paintView.refresh()
            }
        }

        deleteMode = findViewById(R.id.delete_mode)
    }

    private fun toggleDeleteMode(): Boolean {
        paintView.deleteMode = !paintView.deleteMode
        if (paintView.deleteMode) {
            deleteMode.text = getText(R.string.normal_mode)
        } else {
            deleteMode.text = getText(R.string.delete_mode)
        }
        return paintView.deleteMode
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.email?.let { updateLoginInfo(it) }
            viewModel.storage = Firebase.storage
            viewModel.list()
        }
        listTagsAdapter = ArrayAdapter(this, R.layout.list_item, R.id.textview)
        listTags.adapter = listTagsAdapter

        spinnerAdapter = ArrayAdapter(this, R.layout.list_item, R.id.textview)
        spinnerGlyph.adapter = spinnerAdapter
    }

    private fun onRequestToast(s: String) {
        if (s.isEmpty()) return

        val msg = getString(viewModel.msgState.value.toInt())
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun onStaged(v: View, s: String) {
        val builder = AlertDialog.Builder(v.context)
        builder.setTitle(R.string.load_which_title)
            .setMessage(R.string.load_which_message)
            .setPositiveButton(R.string.yes) { _, _ ->
                viewModel.load(s, true)
            }
            .setNegativeButton(R.string.no) { _, _ ->
                viewModel.load(s, false)
            }
            .show()
    }

    private fun onLoadClick(v: View) {
        val editText = EditText(this)
        val builder = AlertDialog.Builder(v.context)
        builder.setTitle(R.string.char_picker_title)
            .setMessage(R.string.char_picker_message)
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                if (editText.text.length == 1) {
                    val targetChar = editText.text.toString()
                    if (viewModel.hasStaged(targetChar)) {
                        onStaged(v, targetChar)
                    } else {
                        viewModel.load(targetChar, false)
                    }
                }
            }
            .show()
    }

    private fun onScopeGroupChecked(
            scopeGroup: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean) {
        if (isChecked) {
            val c = viewModel.charState.value
            if (c.isNada() && checkedId != R.id.btn_char) {
                scopeGroup.check(R.id.btn_char)
            } else {
                val checkBox = findViewById<CheckBox>(R.id.draw_mode)
                checkBox.isEnabled = true
                when (checkedId) {
                    R.id.btn_char -> {
                        viewModel.setScope(Scope.Char)
                        checkBox.text = getString(R.string.move)
                    }
                    R.id.btn_glyph -> {
                        viewModel.setScope(Scope.Glyph)
                        checkBox.text = getString(R.string.move)
                    }
                    R.id.btn_stroke -> {
                        viewModel.setScope(Scope.Stroke)
                        checkBox.text = getString(R.string.draw)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun onLoginClick() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        val loginIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        loginLauncher.launch(loginIntent)
    }

    private fun invalidClick(v: View) {
        Toast.makeText(v.context, R.string.invalid_click, Toast.LENGTH_SHORT).show()
    }

    private fun onSaveClick(v: View) {
        if (viewModel.hasStaged(viewModel.charState.value.text)) {
            // Validate for overwriting
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(R.string.overwrite_title)
                .setMessage(R.string.overwrite_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    viewModel.save()
                }
                .setNegativeButton(R.string.no) { _, _ -> }

            builder.show()
        } else {
            viewModel.save()
        }
    }

    private fun onAddGlyph(@Suppress("UNUSED_PARAMETER") v: View) {
        val c = viewModel.charState.value
        if (c.isNada()) return

        val index = c.size
        c.add(index, Glyph.getEmpty(), true)
        spinnerAdapter.add(index.toString())
        spinnerGlyph.setSelection(index)
    }

    private fun onAddControlPoint(@Suppress("UNUSED_PARAMETER") v: View) {
        viewModel.charState.value.currentGlyph.getSelectedStroke().addControlPoint()
        if (viewModel.drawMode) {
            drawModeCheck.isChecked = false
        }
        paintView.refresh()
    }

    private fun onTag(v: View) {
        val builder = AlertDialog.Builder(v.context)
        val tagsId = arrayOf(
            R.string.tag_kai,
            R.string.tag_xin,
            R.string.tag_cao,
            R.string.tag_yi,
            R.string.tag_zhi,
            R.string.tag_jian,
            R.string.tag_you,
            R.string.tag_gai
        )
        val g = viewModel.charState.value.currentGlyph
        val tags = tagsId.map { getString(it) }.toTypedArray()
        val checked = tags.map { g.tags.contains(it) }.toBooleanArray()
        builder.setMultiChoiceItems(tags, checked) { _, _, _ -> }
            .setTitle(R.string.dlg_tag_title)
            .setPositiveButton(R.string.ok) { _, _ ->
                g.tags.clear()
                listTagsAdapter.clear()
                for (i in 0 until tags.size) {
                    val t = tags[i]
                    if (checked[i]) {
                        g.tags.add(t)
                        listTagsAdapter.add(t)
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    private fun onDeleteGlyph(v: View) {
        val c = viewModel.charState.value
        if (c.isNada() || c.size <= 1) return

        val actualDelete = {
            val index = c.currentIndex
            c.remove(index, c.currentGlyph, true)
            resetSpinner()
            if (index == 0) {
                paintView.refresh()
            }
        }

        if (c.currentGlyph.isEmpty()) {
            actualDelete()
        } else {
            val builder = AlertDialog.Builder(v.context)
            builder.setTitle(R.string.delete_glyph_title)
                .setMessage(R.string.delete_glyph_message)
                .setPositiveButton(R.string.yes) { _, _ ->
                    actualDelete()
                }
                .setNegativeButton(R.string.no) { _, _ -> }
                .show()
        }
    }

    private fun onDeleteControlPoint(v: View) {
        val mode = toggleDeleteMode()
        val format = getString(R.string.set_delete_mode)
        if (mode) {
            deleteMode.text = getString(R.string.delete_mode)
        } else {
            deleteMode.text = getString(R.string.normal_mode)
        }
        val message = String.format(format, deleteMode.text)
        Toast.makeText(v.context, message, Toast.LENGTH_SHORT).show()
    }

    private fun onDeleteStrokes(@Suppress("UNUSED_PARAMETER") v: View) {
        val g = viewModel.charState.value.currentGlyph
        if (!g.hasSelectedStrokes()) return

        g.removeSelectedStrokes()
        paintView.refresh()
    }

    private fun onRotateStrokes(v: View) {
        val g = viewModel.charState.value.currentGlyph
        if (!g.hasSelectedStrokes()) return

        val builder = AlertDialog.Builder(v.context)
        val input = EditText(v.context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setTitle(R.string.rotate_title)
            .setMessage(R.string.rotate_message)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val degree = input.text.toString().toInt()
                if (degree < -360 || degree > 360) {
                    Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                g.rotateSelectedStrokes(degree)
                paintView.refresh()
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
        builder.show()
    }

    private fun onZoomStrokes(v: View) {
        val g = viewModel.charState.value.currentGlyph
        if (!g.hasSelectedStrokes()) return

        val dialogView = layoutInflater.inflate(R.layout.zoom_dialog, null)
        val input = dialogView.findViewById<EditText>(R.id.text_pct)
        val zoomX = dialogView.findViewById<CheckBox>(R.id.zoom_chk_x)
        val zoomY = dialogView.findViewById<CheckBox>(R.id.zoom_chk_y)
        val dlg = AlertDialog.Builder(v.context)
            .setView(dialogView)
            .show()
        dialogView.findViewById<Button>(R.id.zoom_ok).setOnClickListener {
            val pct = input.text.toString().toInt()
            if (pct <= 0 || pct > 30000) {
                Toast.makeText(this, R.string.invalid_input, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            g.zoomSelectedStrokes(pct, zoomX.isChecked, zoomY.isChecked)
            paintView.refresh()
            dlg.dismiss()
        }
        dialogView.findViewById<Button>(R.id.zoom_cancel).setOnClickListener {
            dlg.dismiss()
        }
    }

    private fun onUndoClick(@Suppress("UNUSED_PARAMETER") v: View) {
        val c = viewModel.charState.value
        when (viewModel.scopeState.value) {
            Scope.Char -> {
                c.undo()
                resetSpinner()
            }
            Scope.Glyph -> {
                c.currentGlyph.undo()
                paintView.refresh()
            }
            Scope.Stroke -> {
                if (viewModel.drawMode) {
                    c.currentGlyph.undo()
                } else {
                    c.currentGlyph.currentStroke.undo()
                }
                paintView.refresh()
            }
        }
    }

    private fun onRedoClick(@Suppress("UNUSED_PARAMETER") v: View) {
        val c = viewModel.charState.value
        when (viewModel.scopeState.value) {
            Scope.Char -> {
                c.redo()
                resetSpinner()
            }
            Scope.Glyph -> {
                c.currentGlyph.redo()
                paintView.refresh()
            }
            Scope.Stroke -> {
                c.currentGlyph.currentStroke.redo()
                paintView.refresh()
            }
        }
    }

    private fun onImportGlyph(v: View) {
        val c = viewModel.charState.value
        if (!c.currentGlyph.isEmpty()) {
            Toast.makeText(v.context, R.string.error_import_glyph, Toast.LENGTH_SHORT).show()
            return
        }

        val editText = EditText(this)
        val builder = AlertDialog.Builder(v.context)
        builder.setTitle(R.string.char_picker_title)
            .setMessage(R.string.char_picker_message)
            .setView(editText)
            .setPositiveButton("OK") { _, _ ->
                if (editText.text.length == 1) {
                    val targetChar = editText.text.toString()
                    viewModel.loadChar(targetChar, viewModel.hasStaged(targetChar)).thenAccept {
                        c.replace(c.currentIndex, it.currentGlyph.clone(), c.currentGlyph, true)
                        paintView.refresh()
                    }
                }
            }
            .show()
    }

    private fun resetSpinner() {
        val c = viewModel.charState.value
        spinnerAdapter.clear()
        for (i in 0 until c.size) {
            spinnerAdapter.add(i.toString())
        }
        spinnerGlyph.setSelection(0)
    }

    private fun onCharChange() {
        val c = viewModel.charState.value
        if (c.isNada()) return

        val badge = "${c.text} ${c.code}"
        val charInfo: TextView = findViewById(R.id.char_info)
        charInfo.text = badge

        listTagsAdapter.clear()
        listTagsAdapter.addAll(c.currentGlyph.tags)
        resetSpinner()
        paintView.refresh()
    }

    private fun onScopeChange() {
        val c = viewModel.charState.value
        if (c.isNada() || c.currentGlyph.isEmpty()) return

        viewModel.charState.value.currentGlyph.deselectStrokes()
        paintView.refresh()
    }

    private fun updateLoginInfo(s: String) {
        findViewById<TextView>(R.id.login_info).text = s
    }

    private fun onLoginResult(r: FirebaseAuthUIAuthenticationResult) {
        if (r.resultCode == RESULT_OK) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                currentUser.email?.let { updateLoginInfo(it) }
                viewModel.storage = Firebase.storage
                viewModel.list()
            }
        } else {
            val resp = r.idpResponse
            if (resp != null) {
                Log.d(TAG, "Log in failed: ${resp.error}")
            }
        }
    }
}
