package com.example.lightapp.studio.persistence

/**
 * A saved `akari(10)` run — decision D7, the same boring strings as every game.
 *
 * The board travels as 100 characters, one per cell: `0` ground, `1` blank wall,
 * `2` bulb, `3`–`7` numbered walls demanding 0–4 bulbs. Beside it, [walls] carries
 * the puzzle's wall layout as 100 characters of `0`/`1` — every wall kind, blank and
 * numbered alike. It is redundant with the board in honest play (walls never mutate),
 * which is the point: restore reads it as the layout's authority cell by cell, so a
 * save whose board has drifted from its mask is a puzzle that never shipped, not a
 * variant. Nonogram set the precedent of carrying derived structure so restore can
 * verify rather than guess.
 *
 * Standalone build: no `GameSession` interface — the shell's `GameSessionSnapshot`
 * carries the resume card, and [AkariSessionStore] builds it from these fields.
 */
data class AkariSession(
    /** 100 characters of `0`–`7`, the board as played. */
    val cells: String,
    /** 100 characters of `0`/`1` — every wall (blank and numbered) of the puzzle. */
    val walls: String,
    val difficulty: String,
    val elapsedMs: Long,
    val mistakes: Int,
    /** Lit white cells over all white cells at save time. */
    val progress: Float,
) {
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

    /** The wall mask of [cells]: 1 where the cell is any wall — blank or numbered
     *  (clues). Restore verifies board against mask cell by cell, so the mask must
     *  carry every wall kind the generator produces, not just the blank ones. */
    fun encodeWalls(cells: List<Int>): String =
        cells.joinToString("") { cell -> if (cell in 1..7 && cell != 2) "1" else "0" }

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