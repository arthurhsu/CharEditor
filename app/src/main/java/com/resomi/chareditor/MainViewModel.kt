package com.resomi.chareditor

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.lang.Exception
import kotlin.concurrent.thread

val TAG = "CharEditor"

class MainViewModel : ViewModel() {
    private var scope = MutableStateFlow(Scope.Char)
    private var char = MutableStateFlow(Character.nada())
    var scopeState: StateFlow<Scope> = scope.asStateFlow()
    var charState: StateFlow<Character> = char.asStateFlow()
    var drawMode: Boolean = false
    lateinit var storage: FirebaseStorage

    private fun loadChar(s: String) {
        if (storage == null) return

        val code = Character.getCode(s)
        val file = "data/${code.substring(0, 1)}/$code.json"
        val ref = storage.reference.child(file)
        val ONE_MEGA_BYTE: Long = 1024 * 1024
        ref.getBytes(ONE_MEGA_BYTE).addOnSuccessListener {
            try {
                val data = String(it)
                val json = JSONObject(data)
                val ret = Character.fromJSON(json)
                char.value = ret
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }.addOnFailureListener {
            Log.e(TAG, it.toString())
            if (it is StorageException) {
                if (it.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                    Log.i(TAG,"$file not existed in cloud, creating new Character")
                    char.value = Character.getNew(s)
                }
            }
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

