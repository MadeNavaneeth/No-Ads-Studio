package com.example.lightapp.studio.persistence

/**
 * A saved `blockpuzzle(8)` run — decision D7, the same boring strings as every game.
 *
 * The board travels as 64 characters of `0`/`1`. The tray travels as up to three
 * `family:variant:density` triples — never geometry, because the catalog *is* the
 * geometry and a shape's identity is its three short fields. If a future catalog
 * change renames or reorders a family, restore rejects the session (null = start
 * fresh) rather than resurrecting a board a new catalog cannot express.
 *
 * `score` replaces the puzzles' mistakes-slot in the key order: this game's record
 * is a score, and the resume card reads it. `List<Int>` rather than `IntArray`
 * deliberately — an array in a data class breaks structural equality.
 *
 * Standalone build: no `GameSession` interface — the shell's `GameSessionSnapshot`
 * carries the resume card, and [BlockSessionStore] builds it from these fields.
 */
data class BlockSession(
    /** 64 cells of 0/1. 1 is a landed block. */
    val cells: List<Int>,
    /** Up to three `family:variant:density` triples. */
    val tray: List<String>,
    /** The run's score at save time — the resume card reads this. */
    val score: Int,
    val difficulty: String,
    val elapsedMs: Long,
    val mistakes: Int,
    /** Filled ground over all cells at save time. */
    val progress: Float,
) {
    init {
        require(cells.size == CELLS) { "cells must hold $CELLS cells" }
        require(cells.all { it == 0 || it == 1 }) { "cells hold 0/1 only" }
        require(tray.size <= 3) { "the tray holds three pieces" }
        require(progress in 0f..1f) { "progress reads 0..1" }
        // Structural validity only. Whether the tray triples name real catalog shapes
        // is the game's restore step's job (see games.blockpuzzle.BlockRestore).
    }

    companion object {
        const val CELLS = 64
    }
}

/**
 * Encoding for [BlockSession] — decision D7. Pure functions with no Android
 * dependency, unit-testable on the JVM. Every decode returns `null` on malformed
 * input rather than throwing: a corrupt save should mean "no saved run", never a
 * crash on launch.
 */
object BlockCodec {

    /** 64 characters, `0`–`1`. */
    fun encodeCells(cells: List<Int>): String =
        cells.joinToString("") { cell -> cell.coerceIn(0, 1).toString() }

    fun decodeCells(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != BlockSession.CELLS) return null
        val out = ArrayList<Int>(BlockSession.CELLS)
        for (ch in encoded) {
            if (ch != '0' && ch != '1') return null
            out += ch - '0'
        }
        return out
    }

    /** `family:variant:density` triples, joined with commas. */
    fun encodeTray(tray: List<String>): String = tray.joinToString(",")

    fun decodeTray(encoded: String?): List<String>? {
        if (encoded.isNullOrEmpty()) return emptyList()
        val out = ArrayList<String>(3)
        for (triple in encoded.split(',')) {
            val parts = triple.split(':')
            if (parts.size != 3) return null
            if (parts[1].toIntOrNull() == null || parts[2].toIntOrNull() == null) return null
            out += triple
        }
        return out
    }
}
