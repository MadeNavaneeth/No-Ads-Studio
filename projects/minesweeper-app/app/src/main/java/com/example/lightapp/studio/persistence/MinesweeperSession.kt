package com.example.lightapp.studio.persistence

/**
 * A saved `minesweeper(9)` session — decision D7, the same boring strings as sudoku
 * and nonogram.
 *
 * The mine layout travels with the save as 81 bits: it was placed at first tap and
 * is deterministic per seed, so carrying it costs nothing to derive and spares
 * restore from ever re-rolling the board underneath a half-finished game. `List<Int>`
 * rather than `IntArray` deliberately — an array in a data class breaks structural
 * equality.
 *
 * Standalone build: no `GameSession` interface — the shell's `GameSessionSnapshot`
 * carries the resume card, and [MinesweeperSessionStore] builds it from these fields.
 */
data class MinesweeperSession(
    /** 81 cells of 0/1. Where the mines are. Never shown until detonation. */
    val mines: List<Int>,
    /** 81 quad-state cells. 0 unknown, 1 revealed, 2 flagged, 3 detonated. */
    val cells: List<Int>,
    /** The difficulty — which also fixes the mine count through MinesweeperGenerator. */
    val difficulty: String,
    val elapsedMs: Long,
    val mistakes: Int,
    /** Revealed safe cells over total safe cells at save time — the resume card reads this. */
    val progress: Float,
) {
    init {
        require(mines.size == CELLS) { "mines must hold $CELLS cells" }
        require(cells.size == CELLS) { "cells must hold $CELLS cells" }
        require(mines.all { it == 0 || it == 1 }) { "mines hold 0/1 only" }
        require(cells.all { it in 0..3 }) { "cells hold 0-3 only" }
        require(progress in 0f..1f) { "progress reads 0..1" }
        // Structural validity only. Whether the mine count matches the difficulty —
        // and whether the board is still winnable — is the game's restore step's
        // job, which owns those invariants (see MinesweeperRestore).
    }

    companion object {
        const val CELLS = 81
    }
}

/**
 * Encoding for [MinesweeperSession] — decision D7.
 *
 * Pure functions with no Android dependency, so they are unit-testable on the JVM.
 * Every decode returns `null` on malformed input rather than throwing: a corrupt
 * save should mean "no saved game", never a crash on launch.
 */
object MinesweeperCodec {

    /** 81 characters, `0`–`1`. */
    fun encodeMines(mines: List<Int>): String =
        mines.joinToString("") { mine -> mine.coerceIn(0, 1).toString() }

    fun decodeMines(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != MinesweeperSession.CELLS) return null
        val out = ArrayList<Int>(MinesweeperSession.CELLS)
        for (ch in encoded) {
            if (ch != '0' && ch != '1') return null
            out += ch - '0'
        }
        return out
    }

    /** 81 characters, `0`–`3`. */
    fun encodeCells(cells: List<Int>): String =
        cells.joinToString("") { cell -> cell.coerceIn(0, 3).toString() }

    fun decodeCells(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != MinesweeperSession.CELLS) return null
        val out = ArrayList<Int>(MinesweeperSession.CELLS)
        for (ch in encoded) {
            if (ch !in '0'..'3') return null
            out += ch - '0'
        }
        return out
    }
}
