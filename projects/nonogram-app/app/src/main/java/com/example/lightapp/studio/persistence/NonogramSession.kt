package com.example.lightapp.studio.persistence

/**
 * A saved `nonogram(10)` session — decision D7, the same boring strings as sudoku.
 *
 * The solution travels with the save: 100 digits that re-derive nothing and cost
 * nothing, so restore never depends on the generator still producing the same
 * board from a seed after an update. `List<Int>` rather than `IntArray`
 * deliberately — an array in a data class breaks structural equality.
 *
 * Standalone build: no `GameSession` interface — the shell's `GameSessionSnapshot`
 * carries the resume card, and [NonogramSessionStore] builds it from these fields.
 */
data class NonogramSession(
    /** 100 cells of 0/1. The puzzle. Never mutated, never shown. */
    val solution: List<Int>,
    /** 100 tri-state cells. 0 unknown, 1 filled, 2 marked. */
    val cells: List<Int>,
    val elapsedMs: Long,
    val difficulty: String,
    val mistakes: Int,
    /** Correct fills over solution fills at save time — the resume card reads this. */
    val progress: Float,
) {
    init {
        require(solution.size == CELLS) { "solution must hold $CELLS cells" }
        require(cells.size == CELLS) { "cells must hold $CELLS cells" }
        require(solution.all { it == 0 || it == 1 }) { "solution holds 0/1 only" }
        require(cells.all { it in 0..2 }) { "cells hold 0/1/2 only" }
        require(progress in 0f..1f) { "progress reads 0..1" }
    }

    companion object {
        const val CELLS = 100
    }
}

/**
 * Encoding for [NonogramSession] — decision D7.
 *
 * Pure functions with no Android dependency, so they are unit-testable on the JVM.
 * Every decode returns `null` on malformed input rather than throwing: a corrupt
 * save should mean "no saved game", never a crash on launch.
 */
object NonogramCodec {

    /** 100 characters, `0`–`1`. */
    fun encodeBitmap(bits: List<Int>): String =
        bits.joinToString("") { bit -> bit.coerceIn(0, 1).toString() }

    fun decodeBitmap(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != NonogramSession.CELLS) return null
        val out = ArrayList<Int>(NonogramSession.CELLS)
        for (ch in encoded) {
            if (ch != '0' && ch != '1') return null
            out += ch - '0'
        }
        return out
    }

    /** 100 characters, `0`–`2`. */
    fun encodeCells(cells: List<Int>): String =
        cells.joinToString("") { cell -> cell.coerceIn(0, 2).toString() }

    fun decodeCells(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != NonogramSession.CELLS) return null
        val out = ArrayList<Int>(NonogramSession.CELLS)
        for (ch in encoded) {
            if (ch !in '0'..'2') return null
            out += ch - '0'
        }
        return out
    }
}
