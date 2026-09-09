package com.example.lightapp.studio.persistence

/**
 * A saved `wordsearch(12)` session — decision D7, the same boring strings as every
 * game. This is the game whose *content* is partly data (the words), so the session
 * carries the placed words and where they run, not a seed: re-deriving a layout from
 * a seed would break silently the day the generator changes, and a half-found grid
 * must survive updates exactly as it was left.
 *
 * Placements travel as `WORD:start:end` triples over the shell's `session.<id>.*`
 * field shape. Direction and length re-derive from the endpoints, and the restore
 * step (the game's own, which owns its invariants) checks the triple against the
 * letter grid before trusting it.
 *
 * Standalone build: no `GameSession` interface — the shell's `GameSessionSnapshot`
 * carries the resume card, and [WordsearchSessionStore] builds it from these fields.
 *
 * `List<Int>` rather than `IntArray` deliberately — an array in a data class breaks
 * structural equality.
 */
data class WordsearchSession(
    /** 144 cells of ASCII A–Z (65–90). The whole grid is visible from the start. */
    val letters: List<Int>,
    /** 144 cells of 0/1 — a found line's cells are lit and stay lit. */
    val found: List<Int>,
    /** Each placed word as `WORD:start:end` (board indexes, first letter first). */
    val placements: List<String>,
    val difficulty: String,
    val elapsedMs: Long,
    val mistakes: Int,
    /** Words found over words placed at save time — the resume card reads this. */
    val progress: Float,
) {
    init {
        require(letters.size == CELLS) { "letters must hold $CELLS cells" }
        require(found.size == CELLS) { "found must hold $CELLS cells" }
        require(letters.all { it in 'A'.code..'Z'.code }) { "letters hold A–Z only" }
        require(found.all { it == 0 || it == 1 }) { "found holds 0/1 only" }
        require(progress in 0f..1f) { "progress reads 0..1" }
        // Structural validity only. Whether the placement triples actually spell
        // their words on this grid is the game's restore step's job.
    }

    companion object {
        const val CELLS = 144
    }
}

/**
 * Encoding for [WordsearchSession] — decision D7.
 *
 * Pure functions with no Android dependency, so they are unit-testable on the JVM.
 * Every decode returns `null` on malformed input rather than throwing: a corrupt
 * save should mean "no saved game", never a crash on launch.
 */
object WordsearchCodec {

    /** 144 characters, `A`–`Z`. */
    fun encodeLetters(letters: List<Int>): String =
        letters.joinToString("") { it.toChar().toString() }

    fun decodeLetters(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != WordsearchSession.CELLS) return null
        if (encoded.any { it !in 'A'..'Z' }) return null
        return encoded.map { it.code }
    }

    /** 144 characters, `0`–`1`. */
    fun encodeFound(found: List<Int>): String =
        found.joinToString("") { it.coerceIn(0, 1).toString() }

    fun decodeFound(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != WordsearchSession.CELLS) return null
        val out = ArrayList<Int>(WordsearchSession.CELLS)
        for (ch in encoded) {
            if (ch != '0' && ch != '1') return null
            out += ch - '0'
        }
        return out
    }

    fun encodePlacements(placements: List<String>): String =
        placements.joinToString(";")

    fun decodePlacements(encoded: String?): List<String>? {
        if (encoded == null) return null
        if (encoded.isEmpty()) return emptyList()
        return encoded.split(";")
    }
}
