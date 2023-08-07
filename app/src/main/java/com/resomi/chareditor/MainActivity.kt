package com.resomi.chareditor

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
        val loadButton = findViewById<Button>(R.id.load)
        loadButton.setOnClickListener { onLoadClick(it) }

        val scopeGroup = findViewById<MaterialButtonToggleGroup>(R.id.scope)
        scopeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            onScopeGroupChecked(scopeGroup, checkedId, isChecked)
        }

        val loginButton = findViewById<Button>(R.id.login)
        loginButton.setOnClickListener { onLoginClick() }
    }

    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.email?.let { updateLoginInfo(it) }
            viewModel.storage = Firebase.storage
        }
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
                    else -> viewModel.setScope(Scope.Stroke)
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

    private fun onCharChange() {
        val c = viewModel.charState.value
        if (c.isNada()) return

        val badge = "${c.text} ${c.code}"
        val charInfo: TextView = findViewById(R.id.char_info)
        charInfo.text = badge
        // TODO: render tags
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
