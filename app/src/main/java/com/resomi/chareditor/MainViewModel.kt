package com.resomi.chareditor

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import kotlin.concurrent.thread

val TAG = "CharEditor"

class MainViewModel : ViewModel() {
    private var scope = MutableStateFlow(Scope.Char)
    private var char = MutableStateFlow(Character.nada())
    var scopeState: StateFlow<Scope> = scope.asStateFlow()
    var charState: StateFlow<Character> = char.asStateFlow()

    private fun loadChar(s: String) {
        val code = Character.getCode(s)
        val file = "${code.substring(0, 1)}/$code.json"
        val url = "https://raw.githubusercontent.com/arthurhsu/calligraphy/master/data/$file"
        try {
            val data = URL(url).readText()
            val json = JSONObject(data)
            val ret = Character.fromJSON(json)
            char.value = ret
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
            // TODO: do not blindly add a new char
            char.value = Character.getNew(s)
        }
    }

    fun load(s: String) {
        thread(true) {
            loadChar(s)
        }
    }

    fun setScope(s: Scope) {
        thread(true) {
            scope.value = s
        }
    }
}

