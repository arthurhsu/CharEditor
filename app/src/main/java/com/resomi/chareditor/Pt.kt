package com.resomi.chareditor

data class Pt (val x: Int, val y: Int) {
    override fun toString(): String {
        return "($x,$y)"
    }
}