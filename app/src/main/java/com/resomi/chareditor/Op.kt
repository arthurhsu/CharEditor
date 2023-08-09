package com.resomi.chareditor

abstract class Op<T> {
    abstract fun add(target: ArrayList<T>)
    abstract fun remove(target: ArrayList<T>)
    abstract fun replace(target:ArrayList<T>)
    abstract fun undo()
    abstract fun redo()
}