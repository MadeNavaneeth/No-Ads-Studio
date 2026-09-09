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
 */
data class SudokuSession(
    /** 81 cells. The generated puzzle. 0 is empty. Never mutated. */
    val givens: List<Int>,
    /** 81 cells. Player values only. 0 is empty. */
    val entries: List<Int>,
    /** 81 cells. Pencil marks as a 9-bit mask per cell; bit 0 is the digit 1. */
    val notes: List<Int>,
    val elapsedMs: Long,
    val difficulty: String,
    val mistakes: Int,
) {
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
     * from live state on the other side of the module boundary; this one is encoded into the
     * saved fields so the shell can show progress without loading a game.
     */
    val progress: Float
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

    // ─── session / daily field encoding ──────────────────────────────────────
    // The shell's SessionStore persists opaque string fields per game and builds its
    // resume-card snapshot from four reserved names — `difficulty`, `progress`,
    // `elapsedMs`, `mistakes`. This codec owns the sudoku-specific names alongside
    // them (`givens`, `entries`, `notes`) and the string packing of the grid itself.

    /** Field names this game writes under `session.sudoku.` / `daily.sudoku.`. */
    private const val F_GIVENS = "givens"
    private const val F_ENTRIES = "entries"
    private const val F_NOTES = "notes"
    private const val F_DIFFICULTY = "difficulty"
    private const val F_PROGRESS = "progress"
    private const val F_ELAPSED = "elapsedMs"
    private const val F_MISTAKES = "mistakes"

    /** Packs a session into the opaque field map the shell stores. */
    fun encodeSession(session: SudokuSession): Map<String, String> = mapOf(
        F_GIVENS to encodeGrid(session.givens),
        F_ENTRIES to encodeGrid(session.entries),
        F_NOTES to encodeNotes(session.notes),
        F_DIFFICULTY to session.difficulty,
        F_PROGRESS to session.progress.toString(),
        F_ELAPSED to session.elapsedMs.toString(),
        F_MISTAKES to session.mistakes.toString(),
    )

    /**
     * Unpacks a stored field map back into a session, or `null` when anything is
     * malformed. A corrupt save means "no saved game", never a crash on launch.
     */
    fun decodeSession(fields: Map<String, String>): SudokuSession? {
        val givens = decodeGrid(fields[F_GIVENS]) ?: return null
        val entries = decodeGrid(fields[F_ENTRIES]) ?: return null
        val notes = decodeNotes(fields[F_NOTES]) ?: emptyGrid()
        return SudokuSession(
            givens = givens,
            entries = entries,
            notes = notes,
            elapsedMs = fields[F_ELAPSED]?.toLongOrNull() ?: 0L,
            difficulty = fields[F_DIFFICULTY] ?: "Moderate",
            mistakes = fields[F_MISTAKES]?.toIntOrNull() ?: 0,
        )
    }

    /** Packs a daily puzzle (day + session) into the shell's daily field map. */
    fun encodeDaily(puzzle: DailyPuzzle): Map<String, String> = encodeSession(puzzle.session)

    /**
     * Unpacks a stored daily field map. The shell has already verified the stored day
     * equals the requested day, so the caller's [day] is attached here unchanged.
     */
    fun decodeDaily(fields: Map<String, String>, day: Long): DailyPuzzle? =
        decodeSession(fields)?.let { DailyPuzzle(day = day, session = it) }

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
