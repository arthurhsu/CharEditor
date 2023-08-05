package com.resomi.chareditor

/*
    There are three states in the editor, and the available actions will change.

    Display: no stroke can be selected, and no editing actions
    Draw: only one stroke can be selected, and one can move/delete points of that stroke
    Edit: multiple strokes can be selected, and one can move/zoom/delete selected strokes
 */
enum class State {
    Display,
    Draw,
    Edit
}