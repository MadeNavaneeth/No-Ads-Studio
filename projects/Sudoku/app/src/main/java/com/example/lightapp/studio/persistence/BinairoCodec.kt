package com.example.lightapp.studio.persistence

/**
 * A saved `binairo(10)` run — decision D7, the same boring strings as every game.
 *
 * The board travels as 100 characters, one per cell: `0` undecided, `1` one,
 * `2` zero — the merged grid as played. Beside it, [givens] carries the puzzle's
 * given mask as 100 characters of `0`/`1`/`2` — the layout's authority, without
 * which restore could not tell a given from a player entry (the two look alike on
 * the board). Nonogram set the precedent of carrying derived structure so restore
 * can verify rather than guess.
 *
 * Structural validity only here; semantic checks (a clue set that never generated,
 * a board legal play could not produce) are the game's restore step's job, as in
 * every game.
 */
data class BinairoSession(
    /** 100 characters of `0`–`2`, the merged board as played. */
    val cells: String,
    /** 100 characters of `0`–`2` — the givens, `0` where the cell is the player's. */
    val givens: String,
    override val difficulty: String,
    override val elapsedMs: Long,
    override val mistakes: Int,
    /** Player-decided cells over the cells the player was asked for, at save time. */
    override val progress: Float,
) : GameSession {
    init {
        require(cells.length == CELLS) { "cells must hold $CELLS cells" }
        require(cells.all { it in '0'..'2' }) { "cells hold the 0–2 vocabulary only" }
        require(givens.length == CELLS) { "givens must hold $CELLS cells" }
        require(givens.all { it in '0'..'2' }) { "givens hold the 0–2 vocabulary only" }
        require(progress in 0f..1f) { "progress reads 0..1" }
    }

    companion object {
        const val CELLS = 100
    }
}

/**
 * Encoding for [BinairoSession] — decision D7. Pure functions with no Android
 * dependency, unit-testable on the JVM. Every decode returns `null` on malformed
 * input rather than throwing: a corrupt save should mean "no saved run", never a
 * crash on launch.
 */
object BinairoCodec {

    /** 100 characters, `0`–`2`. */
    fun encodeCells(cells: List<Int>): String =
        cells.joinToString("") { cell -> cell.coerceIn(0, 2).toString() }

    fun decodeCells(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != BinairoSession.CELLS) return null
        val out = ArrayList<Int>(BinairoSession.CELLS)
        for (ch in encoded) {
            if (ch !in '0'..'2') return null
            out += ch - '0'
        }
        return out
    }

    /** The given mask of [givens]: the given value where fixed, `0` where the player's. */
    fun encodeGivens(givens: List<Int>): String =
        encodeCells(givens)

    fun decodeGivens(encoded: String?): List<Int>? = decodeCells(encoded)
}