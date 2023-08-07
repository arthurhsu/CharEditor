package com.resomi.chareditor

/*
    There are four states in the editor, and the available actions will change.

    Display: no stroke can be selected, and one can add new glyph
    Draw: only one stroke can be selected, and one can move/delete points of that stroke
    Edit: multiple strokes can be selected, and one can move/zoom/delete selected strokes
    Tag: tagging operations only
 */
enum class Scope {
    Char,
    Stroke,
    Glyph,
    Tag
}