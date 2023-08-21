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
    companion object {
        val ONE_MEGA_BYTE: Long = 1024 * 1024
    }

    private var scope = MutableStateFlow(Scope.Char)
    private var char = MutableStateFlow(Character.nada())
    private var msg = MutableStateFlow("")
    var scopeState: StateFlow<Scope> = scope.asStateFlow()
    var charState: StateFlow<Character> = char.asStateFlow()
    var msgState: StateFlow<String> = msg.asStateFlow()
    var drawMode: Boolean = false
    lateinit var storage: FirebaseStorage
    private val serverChars = HashSet<String>()
    private val stageChars = HashSet<String>()
    private val allChars = HashSet<String>()
    private var listed = false

    fun loadChar(s: String, fromStage: Boolean): CompletableFuture<Character> {
        val promise = CompletableFuture<Character>()
        if (storage == null) {
            promise.complete(Character.getNew(s))
            return promise
        }

        thread(true) {
            val code = Character.getCode(s)
            val file: String
            if (fromStage) {
                file = "stage/$code.json"
            } else {
                file = "data/${code.substring(0, 1)}/$code.json"
            }
            val ref = storage.reference.child(file)
            ref.getBytes(ONE_MEGA_BYTE).addOnSuccessListener {
                try {
                    val data = String(it)
                    val json = JSONObject(data)
                    val ret = Character.fromJSON(json)
                    promise.complete(ret)
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                }
            }.addOnFailureListener {
                Log.e(TAG, it.toString())
                if (it is StorageException) {
                    if (it.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                        Log.i(TAG, "$file not existed in cloud, creating new Character")
                        promise.complete(Character.getNew(s))
                    }
                }
            }
        }
        return promise
    }

    private fun saveChar(): CompletableFuture<String> {
        val promise = CompletableFuture<String>()
        if (char.value.isNada()) {
            promise.complete("")
            return promise
        }

        thread(true) {
            msg.value = ""
            val code = char.value.code
            val file = "stage/$code.json"
            val ref = storage.reference.child(file)
            val json = char.value.toJSON().toString().encodeToByteArray()
            ref.putBytes(json).addOnSuccessListener {
                promise.complete(code)
            }.addOnFailureListener {
                Log.e(TAG, it.toString())
                promise.complete("")
            }
        }
        return promise
    }

    private fun parseAllChars(): CompletableFuture<Boolean> {
        val promise = CompletableFuture<Boolean>()
        thread(true) {
            val ref = storage.reference.child("word.txt")
            ref.getBytes(ONE_MEGA_BYTE).addOnSuccessListener {
                try {
                    val data = String(it)
                    for (i in 0 until data.length) {
                        if (data.codePointAt(i) > 0x4000) {
                            allChars.add(data[i].toString())
                        }
                    }
                    Log.i(TAG, "all chars: ${allChars.size}")
                    promise.complete(true)
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    promise.complete(false)
                }
            }.addOnFailureListener {
                Log.e(TAG, it.toString())
                promise.complete(false)
            }
        }
        return promise
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

    fun hasStaged(s: String): Boolean {
        return stageChars.contains(Character.getCode(s))
    }

    fun load(s: String, fromStage: Boolean) {
        if (listed) {
            // Check if shortcutting is possible
            val code = Character.getCode(s)
            if (!serverChars.contains(code) && !stageChars.contains(code)) {
                // New character
                char.value = Character.getNew(s)
                Log.i(TAG, "short cut new char")
                return
            }
        }
        loadChar(s, fromStage).thenAccept {
            char.value = it
        }
    }

    fun save() {
        saveChar().thenAccept {
            if (it.isEmpty()) {
                msg.value = R.string.save_failed.toString()
            } else {
                msg.value = R.string.save_success.toString()
                stageChars.add(it)
            }
        }
    }

    fun list() {
        if (listed) return
        msg.value = ""

        val futures = ArrayList<CompletableFuture<*>>()
        serverChars.clear()
        stageChars.clear()
        allChars.clear()

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

        futures.add(parseAllChars())

        CompletableFuture.allOf(*futures.toTypedArray()).thenAccept {
            msg.value = R.string.list_success.toString()
            listed = true
        }
    }

    fun getStageChars(): Array<String> {
        return stageChars.toTypedArray()
    }

    fun setScope(s: Scope) {
        thread(true) {
            scope.value = s
        }
    }

    fun suggestChar(): String {
        if (!listed) return ""

        for (c in allChars) {
            if (!serverChars.contains(c) && !stageChars.contains(c)) {
                return c
            }
        }
        return ""
    }
}

