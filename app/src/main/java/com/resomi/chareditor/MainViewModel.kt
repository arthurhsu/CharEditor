package com.resomi.chareditor

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread

val TAG = "CharEditor"

class MainViewModel : ViewModel() {
    private var scope = MutableStateFlow(Scope.Char)
    private var char = MutableStateFlow(Character.nada())
    private var msg = MutableStateFlow("")
    var scopeState: StateFlow<Scope> = scope.asStateFlow()
    var charState: StateFlow<Character> = char.asStateFlow()
    var msgState: StateFlow<String> = msg.asStateFlow()
    var drawMode: Boolean = false
    lateinit var storage: FirebaseStorage
    val serverChars = HashSet<String>()
    val stageChars = HashSet<String>()

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

    private fun saveChar() {
        if (char.value.isNada()) return

        msg.value = ""
        val code = char.value.code
        val file = "stage/$code.json"
        val ref = storage.reference.child(file)
        val json = char.value.toJSON().toString().encodeToByteArray()
        ref.putBytes(json).addOnSuccessListener {
            msg.value = R.string.save_success.toString()
            stageChars.add(code)
        }.addOnFailureListener {
            Log.e(TAG, it.toString())
            msg.value = R.string.save_failed.toString()
        }
    }

    private fun listDirOnServer(dir: String): CompletableFuture<ArrayList<String>> {
        val promise = CompletableFuture<ArrayList<String>>()
        thread(true) {
            val ref = storage.reference.child(dir)
            val ret = ArrayList<String>()
            ref.listAll()
                .addOnSuccessListener {
                    for (item in it.items) {
                        ret.add(item.name.substring(0, 4))
                    }
                    Log.i(TAG, "list $dir done")
                    promise.complete(ret)
                }
                .addOnFailureListener {
                    Log.e(TAG, "unable to list $dir, error: ${it.toString()}")
                    promise.complete(ret)
                }
        }
        return promise
    }

    fun load(s: String) {
        thread(true) {
            loadChar(s)
        }
    }

    fun save() {
        thread(true) {
            saveChar()
        }
    }

    fun list() {
        msg.value = ""

        val futures = ArrayList<CompletableFuture<ArrayList<String>>>()
        serverChars.clear()
        stageChars.clear()

        for (i in 4..9) {
            val p = listDirOnServer("data/$i")
            p.thenAccept {
                runBlocking {
                    serverChars.addAll(it)
                }
            }
            futures.add(p)
        }
        val q = listDirOnServer("/stage")
        futures.add(q)
        q.thenAccept {
            stageChars.clear()
            stageChars.addAll(it)
        }

        CompletableFuture.allOf(*futures.toTypedArray()).thenAccept {
            msg.value = R.string.list_success.toString()
        }
    }

    fun setScope(s: Scope) {
        thread(true) {
            scope.value = s
        }
    }
}

