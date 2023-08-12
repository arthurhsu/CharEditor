package com.resomi.chareditor

import java.util.Stack

open class ActionQueue<T> {
    private val actionQueue = Stack<Pair<Action, Triple<Int, T, T>>>()
    private val undoQueue = Stack<Pair<Action, Triple<Int, T, T>>>()

    private fun doAction(action: Action?, task: Triple<Int, T, T>) {
        when (action) {
            Action.Add -> add(task.component1(), task.component2(), false)
            Action.Delete -> remove(task.component1(), task.component2(), false)
            Action.Replace -> replace(task.component1(), task.component2(), task.component3(), false)
            else -> {}
        }
    }

    private fun reverse(task: Triple<Int, T, T>): Triple<Int, T, T> {
        if (task.component3() != null) {
            return Triple(task.component1(), task.component3(), task.component2())
        }
        return task
    }

    fun undo() {
        if (actionQueue.isEmpty()) return

        val task = actionQueue.pop()
        undoQueue.push(task)
        doAction(Action.reverse[task.first], reverse(task.second))
    }

    fun redo() {
        if (undoQueue.isEmpty()) return

        val task = undoQueue.pop()
        actionQueue.push(task)
        doAction(task.first, task.second)
    }

    private fun record(action: Action, index: Int, target: T, original: T) {
        actionQueue.push(Pair(action, Triple(index, target, original)))
    }

    open fun add(index: Int, target: T, rec: Boolean) {
        if (rec) {
            record(Action.Add, index, target, target)
        }
    }

    open fun remove(index: Int, target: T, rec: Boolean) {
        if (rec) {
            record(Action.Delete, index, target, target)
        }
    }

    open fun replace(index: Int, target: T, original: T, rec: Boolean) {
        if (rec) {
            record(Action.Replace, index, target, original)
        }
    }
}