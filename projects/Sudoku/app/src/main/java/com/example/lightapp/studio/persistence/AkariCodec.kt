package com.example.lightapp.studio.persistence

/**
 * A saved `akari(10)` run — decision D7, the same boring strings as every game.
 *
 * The board travels as 100 characters, one per cell: `0` ground, `1` blank wall,
 * `2` bulb, `3`–`7` numbered walls demanding 0–4 bulbs. Beside it, [walls] carries
 * the puzzle's blank-wall layout as 100 characters of `0`/`1` — redundant with the
 * board for every position a bulb could occupy, but *not* derivable from it: clues
 * are the only walls the board names explicitly, and a board whose blank walls have
 * moved is a puzzle that never shipped. Nonogram set the precedent of carrying
 * derived structure so restore can verify rather than guess.
 *
 * Structural validity only here; semantic checks (a layout that never generated, a
 * board legal play could not produce) are the game's restore step's job, as in
 * every game.
 */
data class AkariSession(
    /** 100 characters of `0`–`7`, the board as played. */
    val cells: String,
    /** 100 characters of `0`/`1` — the blank walls under the clues. */
    val walls: String,
    override val difficulty: String,
    override val elapsedMs: Long,
    override val mistakes: Int,
    /** Lit white cells over all white cells at save time. */
    override val progress: Float,
) : GameSession {
    init {
        require(cells.length == CELLS) { "cells must hold $CELLS cells" }
        require(cells.all { it in '0'..'7' }) { "cells hold the 0–7 vocabulary only" }
        require(walls.length == CELLS) { "walls must hold $CELLS cells" }
        require(walls.all { it == '0' || it == '1' }) { "walls hold 0/1 only" }
        require(progress in 0f..1f) { "progress reads 0..1" }
    }

    companion object {
        const val CELLS = 100
    }
}

/**
 * Encoding for [AkariSession] — decision D7. Pure functions with no Android
 * dependency, unit-testable on the JVM. Every decode returns `null` on malformed
 * input rather than throwing: a corrupt save should mean "no saved run", never a
 * crash on launch.
 */
object AkariCodec {

    /** 100 characters, `0`–`7`. */
    fun encodeCells(cells: List<Int>): String =
        cells.joinToString("") { cell -> cell.coerceIn(0, 7).toString() }

    fun decodeCells(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != AkariSession.CELLS) return null
        val out = ArrayList<Int>(AkariSession.CELLS)
        for (ch in encoded) {
            if (ch !in '0'..'7') return null
            out += ch - '0'
        }
        return out
    }

    /** The blank-wall mask of [cells]: 1 where the cell is a blank wall. */
    fun encodeWalls(cells: List<Int>): String =
        cells.joinToString("") { cell -> if (cell == 1) "1" else "0" }

    fun decodeWalls(encoded: String?): List<Boolean>? {
        if (encoded == null || encoded.length != AkariSession.CELLS) return null
        val out = ArrayList<Boolean>(AkariSession.CELLS)
        for (ch in encoded) {
            if (ch != '0' && ch != '1') return null
            out += ch == '1'
        }
        return out
    }
}
