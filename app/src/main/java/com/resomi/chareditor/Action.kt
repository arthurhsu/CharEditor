package com.resomi.chareditor

enum class Action {
    Add,
    Delete,
    Replace;

    companion object {
        val reverse = HashMap<Action, Action>(
            mapOf(
                Add to Delete,
                Delete to Add,
                Replace to Replace,
            )
        )
    }
}