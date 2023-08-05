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

        fun load(s: String) {
            get().c = Character.load(s)
        }
    }

    var state = State.Display
    var c = Character.nada()
}