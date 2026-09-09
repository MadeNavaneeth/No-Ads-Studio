package com.example.lightapp.studio.persistence

/**
 * A saved Sudoku session — decision D7.
 *
 * [givens] and [entries] are kept **separate** rather than merged into one grid. That is
 * the whole reason a given can never be edited: there is no code path by which a player
 * value overwrites a given, even across a process restart. Requirement 16 criteria 6
 * and 10.
 *
 * `List<Int>` rather than `IntArray` deliberately — an array in a data class breaks
 * structural equality, and 81 boxed ints cost nothing at persistence frequency.
 *
 * Implements [GameSession] so the shell's resume cards read progress without loading
 * a game — see `MainActivity`, which maps sessions without knowing this type.
 */
data class SudokuSession(
    /** 81 cells. The generated puzzle. 0 is empty. Never mutated. */
    val givens: List<Int>,
    /** 81 cells. Player values only. 0 is empty. */
    val entries: List<Int>,
    /** 81 cells. Pencil marks as a 9-bit mask per cell; bit 0 is the digit 1. */
    val notes: List<Int>,
    override val elapsedMs: Long,
    override val difficulty: String,
    override val mistakes: Int,
) : GameSession {
    init {
        require(givens.size == CELLS) { "givens must hold $CELLS cells, got ${givens.size}" }
        require(entries.size == CELLS) { "entries must hold $CELLS cells, got ${entries.size}" }
        require(notes.size == CELLS) { "notes must hold $CELLS cells, got ${notes.size}" }
    }

    /**
     * How far through the puzzle this save is, 0..1 — what the resume card shows.
     *
     * Counts only what the player supplied over what the player was asked to supply, so a
     * freshly generated save reads 0% rather than the ~40% a naive "filled cells" count would
     * give. Deliberately the same definition as `SudokuState.completion`, which computes it
     * from live state on the other side of the module boundary; this one exists so the shell
     * can show progress without loading a game.
     */
    override val progress: Float
        get() {
            val askedFor = givens.count { it == 0 }
            return if (askedFor == 0) 1f else entries.count { it != 0 }.toFloat() / askedFor
        }

    companion object {
        const val CELLS = 81
    }
}

/**
 * The stored daily puzzle: one session plus the UTC day it belongs to.
 *
 * The day travels with the session because a daily session is only meaningful on the
 * day it was generated — yesterday's session is not "in progress", it is leftovers.
 * Keeping the pair together means the caller decides by comparing one long, not by
 * guessing which session might be the daily one.
 */
data class DailyPuzzle(
    val day: Long,
    val session: SudokuSession,
)

/**
 * Encoding for [SudokuSession] — decision D7.
 *
 * Compact strings rather than a database. Room would have needed KSP annotation
 * processing, which taxes every build on the reference machine, and one game's state is
 * three strings and a long. This is deliberately boring.
 *
 * Pure functions with no Android dependency, so they are unit-testable on the JVM.
 * Every decode returns `null` on malformed input rather than throwing: a corrupt save
 * should mean "no saved game", never a crash on launch.
 */
object SudokuCodec {

    /** 81 characters, `0`–`9`. */
    fun encodeGrid(cells: List<Int>): String =
        cells.joinToString("") { digit -> digit.coerceIn(0, 9).toString() }

    fun decodeGrid(encoded: String?): List<Int>? {
        if (encoded == null || encoded.length != SudokuSession.CELLS) return null
        val out = ArrayList<Int>(SudokuSession.CELLS)
        for (ch in encoded) {
            if (ch !in '0'..'9') return null
            out += ch - '0'
        }
        return out
    }

    /** Comma-separated 9-bit masks, one per cell. */
    fun encodeNotes(masks: List<Int>): String =
        masks.joinToString(",") { mask -> (mask and NOTE_MASK).toString() }

    fun decodeNotes(encoded: String?): List<Int>? {
        if (encoded.isNullOrEmpty()) return null
        val parts = encoded.split(',')
        if (parts.size != SudokuSession.CELLS) return null
        val out = ArrayList<Int>(SudokuSession.CELLS)
        for (part in parts) {
            val value = part.toIntOrNull() ?: return null
            if (value < 0 || value > NOTE_MASK) return null
            out += value
        }
        return out
    }

    /** An empty grid, for a fresh session. */
    fun emptyGrid(): List<Int> = List(SudokuSession.CELLS) { 0 }

    // ─── note helpers, so callers never touch bit arithmetic ───

    /** Digits 1–9 currently marked in [mask]. */
    fun notesIn(mask: Int): List<Int> = (1..9).filter { digit -> hasNote(mask, digit) }

    fun hasNote(mask: Int, digit: Int): Boolean =
        digit in 1..9 && (mask and (1 shl (digit - 1))) != 0

    fun toggleNote(mask: Int, digit: Int): Int =
        if (digit !in 1..9) mask else mask xor (1 shl (digit - 1))

    /** All nine bits set. Guards against a mask wider than the grid allows. */
    private const val NOTE_MASK = 0b1_1111_1111
}
