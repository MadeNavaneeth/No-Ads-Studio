package com.example.lightapp.studio.persistence

/**
 * What the shell needs to draw a resume card — MainActivity maps sessions to
 * `ResumeState` through this alone, so the shell never learns what any game's
 * session looks like and adding a game needs no change there.
 */
interface GameSession {
    val difficulty: String
    val progress: Float
    val elapsedMs: Long
    val mistakes: Int
}

/**
 * A saved `nonogram(10)` session — decision D7, the same boring strings as sudoku.
 *
 * The solution travels with the save: 100 digits that re-derive nothing and cost
 * nothing, so restore never depends on the generator still producing the same
 * board from a seed after an update. `List<Int>` rather than `IntArray`
 * deliberately — an array in a data class breaks structural equality.
 */
data class NonogramSession(
    /** 100 cells of 0/1. The puzzle. Never mutated, never shown. */
    val solution: List<Int>,
    /** 100 tri-state cells. 0 unknown, 1 filled, 2 marked. */
    val cells: List<Int>,
    override val elapsedMs: Long,
    override val difficulty: String,
    override val mistakes: Int,
    /** Correct fills over solution fills at save time — the resume card reads this. */
    override val progress: Float,
) : GameSession {
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
