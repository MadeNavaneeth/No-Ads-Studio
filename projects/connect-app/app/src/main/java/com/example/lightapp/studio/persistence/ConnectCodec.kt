package com.example.lightapp.studio.persistence

/**
 * Encoding ceiling for a connect(7) board value. Duplicated here, not imported:
 * the game's tests pin the two in sync.
 */
const val CONNECT_PATH_BASE = 8
const val CONNECT_PAIRS = 8

/**
 * A saved `connect(7)` session — decision D7, the same boring strings as every game.
 *
 * The full merged board travels as 49 characters, one per cell: `0` empty, `1`–`8`
 * an endpoint of that pair, `9`–`16` a laid segment (pair id + [CONNECT_PATH_BASE]).
 * The segment's *pair id is carried* — adjacent routes from different pairs can sit
 * orthogonally next to each other, so one connected blob of segments can hold two
 * routes, and re-deriving ids from topology alone would be a guess.
 *
 * Standalone build: no `GameSession` interface — the shell's `GameSessionSnapshot`
 * carries the resume card, and [ConnectSessionStore] builds it from these fields.
 * `List<Int>` rather than `IntArray` deliberately — an array in a data class breaks
 * structural equality.
 */
data class ConnectSession(
    /** 49 cells: 0 empty, 1–8 endpoint, 9–16 laid segment of pair (value − 8). */
    val board: List<Int>,
    val difficulty: String,
    val elapsedMs: Long,
    val mistakes: Int,
    /** Used cells over total cells at save time — the resume card reads this. */
    val progress: Float,
) {
    init {
        require(board.size == CELLS) { "board must hold $CELLS cells" }
        require(board.all { it in 0..CONNECT_PATH_BASE + CONNECT_PAIRS }) {
            "board values read 0..${CONNECT_PATH_BASE + CONNECT_PAIRS}"
        }
        require(progress in 0f..1f) { "progress reads 0..1" }
    }

    companion object {
        const val CELLS = 49
    }
}

/**
 * Encoding for [ConnectSession] — decision D7.
 *
 * Pure functions with no Android dependency, so they are unit-testable on the JVM.
 * Every decode returns `null` on malformed input rather than throwing.
 */
object ConnectCodec {

    /** `0`–`9` for values 0–9, `a`–`g` for values 10–16. */
    fun encodeBoard(board: List<Int>): String =
        board.joinToString("") { encodeValue(it) }

    fun decodeBoard(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != ConnectSession.CELLS) return null
        val out = ArrayList<Int>(ConnectSession.CELLS)
        for (ch in encoded) {
            val value = decodeValue(ch) ?: return null
            out += value
        }
        return out
    }

    private fun encodeValue(value: Int): String {
        val v = value.coerceIn(0, CONNECT_PATH_BASE + CONNECT_PAIRS)
        return if (v <= 9) v.toString() else ('a' + v - 10).toString()
    }

    private fun decodeValue(ch: Char): Int? = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'a'..'g' -> ch - 'a' + 10
        else -> null
    }
}
