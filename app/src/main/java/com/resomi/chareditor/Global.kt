package com.resomi.chareditor

class Global private constructor() {
    companion object {
        @Volatile
        private var instance: Global? = null

        fun get() = instance ?: synchronized(this) {
            instance ?: Global().also {
                instance = it
            }
        }
    }

    val state = State.Draw
    val c = Character.nada()
}