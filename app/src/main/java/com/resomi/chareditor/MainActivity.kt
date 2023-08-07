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
    private lateinit var listTagsAdapter: ArrayAdapter<String>

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
                merge(viewModel.charState, viewModel.scopeState).collectLatest {
                    if (it is Character) {
                        Log.i(TAG, "char change ${it.text}")
                        onCharChange()
                    }
                    if (it is Scope) {
                        Log.i(TAG, "scope change $it")
                        onScopeChange()
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

        val loadButton = findViewById<Button>(R.id.load)
        loadButton.setOnClickListener { onLoadClick(it) }

        val saveButton = findViewById<Button>(R.id.save)
        saveButton.setOnClickListener { onSaveClick(it) }

        val addButton = findViewById<Button>(R.id.add)
        addButton.setOnClickListener {
            when (viewModel.scopeState.value) {
                Scope.Char -> onAddGlyph(it)
                Scope.Stroke -> onAddControlPoint(it)
                Scope.Tag -> onAddTag(it)
                else -> invalidClick(it)
            }
        }

        val moveButton = findViewById<Button>(R.id.move)
        moveButton.setOnClickListener {
            when (viewModel.scopeState.value) {
                Scope.Glyph -> onMoveStrokes(it)
                Scope.Stroke -> onMoveControlPoint(it)
                else -> invalidClick(it)
            }
        }

        val deleteButton = findViewById<Button>(R.id.delete)
        deleteButton.setOnClickListener {
            when (viewModel.scopeState.value) {
                Scope.Char -> onDeleteGlyph(it)
                Scope.Glyph -> onDeleteStrokes(it)
                Scope.Stroke -> onDeleteControlPoint(it)
                Scope.Tag -> onDeleteTag(it)
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

        val importButton = findViewById<Button>(R.id.import_from)
        importButton.setOnClickListener {
            if (viewModel.scopeState.value != Scope.Glyph) {
                invalidClick(it)
            } else {
                onImportGlyph(it)
            }
        }

        listTags = findViewById<ListView>(R.id.tag_box)
        listTags.setOnItemClickListener { _, _, pos, _ ->
            viewModel.charState.value.currentGlyph.currentTag = listTagsAdapter.getItem(pos) ?: ""
        }

        val spinnerGlyph = findViewById<Spinner>(R.id.glyph_spinner)
        spinnerGlyph.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // TODO: implement
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // TODO: implement
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.email?.let { updateLoginInfo(it) }
            viewModel.storage = Firebase.storage
        }
        listTagsAdapter = ArrayAdapter(this, R.layout.list_item, R.id.textview)
        listTags.adapter = listTagsAdapter
    }

    private fun onLoadClick(v: View) {
        val builder = AlertDialog.Builder(v.context)
        val inflater = layoutInflater
        builder.setTitle(R.string.char_picker)
        val dialogLayout = inflater.inflate(R.layout.char_picker, null)
        val editText = dialogLayout.findViewById<EditText>(R.id.edit_char)
        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { _, _ ->
            if (editText.text.length == 1) {
                viewModel.load(editText.text.toString())
            }
        }
        builder.show()
    }

    private fun onScopeGroupChecked(
            scopeGroup: MaterialButtonToggleGroup, checkedId: Int, isChecked: Boolean) {
        if (isChecked) {
            val c = viewModel.charState.value
            if (c.isNada() && checkedId != R.id.btn_char) {
                scopeGroup.check(R.id.btn_char)
            } else {
                when (checkedId) {
                    R.id.btn_char -> viewModel.setScope(Scope.Char)
                    R.id.btn_glyph -> viewModel.setScope(Scope.Glyph)
                    R.id.btn_stroke -> viewModel.setScope(Scope.Stroke)
                    else -> viewModel.setScope(Scope.Tag)
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
        // TODO: implement
    }

    private fun onAddGlyph(v: View) {
        // TODO: implement
    }

    private fun onAddControlPoint(v: View) {
        // TODO: implement
    }

    private fun onAddTag(v: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.add_tag_title)
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton(R.string.ok) { _, _ ->
            val newTag = input.text.toString()
            val g = viewModel.charState.value.currentGlyph
            if (g.tags.add(newTag)) {
                listTagsAdapter.add(newTag)
                g.currentTag = newTag
            }
        }
        builder.setNegativeButton(R.string.cancel) { _, _ -> }
        builder.show()
    }

    private fun onMoveStrokes(v: View) {
        // TODO: implement
    }

    private fun onMoveControlPoint(v: View) {
        // TODO: implement
    }

    private fun onDeleteGlyph(v: View) {
        // TODO: implement
    }

    private fun onDeleteControlPoint(v: View) {
        // TODO: implement
    }

    private fun onDeleteStrokes(v: View) {
        // TODO: implement
    }

    private fun onDeleteTag(v: View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delete_tag_title)
        val g = viewModel.charState.value.currentGlyph
        val format = getString(R.string.delete_tag_prompt)
        builder.setMessage(String.format(format, g.currentTag))
        builder.setPositiveButton(R.string.yes) { _, _ ->
            val glyph = viewModel.charState.value.currentGlyph
            if (glyph.tags.remove(glyph.currentTag)) {
                listTagsAdapter.clear()
                listTagsAdapter.addAll(glyph.tags)
                glyph.currentTag = listTagsAdapter.getItem(0) ?: ""
            }
        }
        builder.setNegativeButton(R.string.no) { _, _ -> }
        builder.show()
    }

    private fun onRotateStrokes(v: View) {
        // TODO: implement
    }

    private fun onZoomStrokes(v: View) {
        // TODO: implement
    }

    private fun onUndoClick(v: View) {
        // TODO: implement
    }

    private fun onImportGlyph(v: View) {
        // TODO: implement
    }

    private fun onCharChange() {
        val c = viewModel.charState.value
        if (c.isNada()) return

        val badge = "${c.text} ${c.code}"
        val charInfo: TextView = findViewById(R.id.char_info)
        charInfo.text = badge
        listTagsAdapter.clear()
        listTagsAdapter.addAll(c.currentGlyph.tags)
        paintView.onCharChange()
    }

    private fun onScopeChange() {
        val c = viewModel.charState.value
        if (c.isNada() || c.currentGlyph.isEmpty()) return

        val s = viewModel.scopeState.value
        val stroke = viewModel.charState.value.currentGlyph.currentStroke
        when (s) {
            Scope.Char -> stroke.selected = false
            Scope.Glyph -> stroke.selected = false
            Scope.Stroke -> stroke.selected = true
            Scope.Tag -> stroke.selected = false
        }
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
            }
        } else {
            val resp = r.idpResponse
            if (resp != null) {
                Log.d(TAG, "Log in failed: ${resp.error}")
            }
        }
    }
}
