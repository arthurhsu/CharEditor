package com.resomi.chareditor

class ActionQueue<T>(private val parent: Op<T>) {
    private val actionQueue = ArrayDeque<Pair<Action, ArrayList<T>>>()
    private val undoQueue = ArrayDeque<Pair<Action, ArrayList<T>>>()

    fun register(action: Action, s: T) {
        actionQueue.add(Pair(action, arrayListOf(s)))
    }

    fun register(action: Action, s: ArrayList<T>) {
        actionQueue.add(Pair(action, s))
    }

    private fun doAction(action: Action?, strokes: ArrayList<T>) {
        when (action) {
            Action.Add -> parent.add(strokes)
            Action.Delete -> parent.remove(strokes)
            Action.Replace -> parent.replace(strokes)
            else -> {}
        }
    }

    fun undo() {
        val task = actionQueue.removeLast()
        undoQueue.addLast(task)
        doAction(Action.reverse[task.first], task.second)
    }

    fun redo() {
        val task = undoQueue.removeLast()
        actionQueue.addFirst(task)
        doAction(task.first, task.second)
    }
}