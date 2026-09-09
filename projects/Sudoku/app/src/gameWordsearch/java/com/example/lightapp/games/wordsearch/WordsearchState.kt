package com.example.lightapp.games.wordsearch

import androidx.compose.runtime.Immutable
import com.example.lightapp.studio.persistence.WordsearchSession

/**
 * A playable position for `wordsearch(12)`.
 *
 * `@Immutable` so Compose can skip recomposition when the instance is unchanged. That
 * annotation is a promise: every mutation must return a new instance, never edit in place.
 *
 * [letters] is the visible grid — all of it, from the first tap. What the player has
 * *found* lives in [foundCells]: found lines stay lettered and go inert, everything
 * else stays huntable. The word list on screen shows the placed words; found ones
 * stay listed (struck through by the screen), because a shrinking list would reveal
 * progress the readout already owns.
 */
@Immutable
data class WordsearchState(
    val letters: List<Char>,
    val placements: List<Placement>,
    val difficulty: String,
    val foundCells: List<Int> = List(WordsearchRules.CELLS) { WordsearchRules.HIDDEN },
    val elapsedMs: Long = 0L,

    /**
     * The UTC day this run belongs to, when it is the day's puzzle (decision D32).
     * Rides on the state — never persisted inside the session — so persist/finish
     * route to the daily slot exactly when a daily is being played.
     */
    val dailyDay: Long? = null,
) {
    /** Words found so far, for the WORDS readout. Derived, never stored. */
    val foundWords: Set<String>
        get() = placements.filter { p -> p.cells.all { foundCells[it] == WordsearchRules.FOUND } }
            .map { it.word }
            .toSet()

    val isComplete: Boolean get() = WordsearchRules.isWin(foundCells, placements)

    /** The one quantity (method §2): words found over words placed. */
    val completion: Float get() = WordsearchRules.completion(placements, foundWords)

    fun toSession() = WordsearchSession(
        letters = letters.map { it.code },
        found = foundCells,
        // `WORD:start:end` triples — the codec's contract. Endpoints re-derive
        // direction and length; the game's restore step re-spells the grid.
        placements = placements.map { "${it.word}:${it.cells.first()}:${it.cells.last()}" },
        difficulty = difficulty,
        elapsedMs = elapsedMs,
        mistakes = 0,
        progress = completion,
    )

    companion object {
        fun fresh(board: Board, difficulty: String, dailyDay: Long? = null) = WordsearchState(
            letters = board.letters,
            placements = board.placements,
            difficulty = difficulty,
            dailyDay = dailyDay,
        )
    }
}

/**
 * One reversible change — decision D17, in memory only. A found word's cells are the
 * previous value of every cell it lit, so undo can darken it again. The whole cell
 * layer travels rather than a delta: a drag can find two words at once, and one
 * 144-int snapshot per move is cheaper than the bookkeeping to invert overlaps.
 */
data class WordsearchMove(
    val previousCells: List<Int>,
) {
    companion object {
        const val MAX_HISTORY = 50
    }
}
