package com.example.lightapp.games.wordsearch

import com.example.lightapp.studio.persistence.WordsearchSession

/**
 * Saved-session validation. A well-formed save can still describe a grid that never
 * shipped — an unknown difficulty, a negative clock, a placement triple that does
 * not spell its word on the letter grid, a grid already complete. Null means
 * "start fresh".
 *
 * Semantic checks live here, not in [WordsearchSession]'s `init`: the session class
 * is shared persistence vocabulary, the game owns the meaning of its own board.
 */
object WordsearchRestore {

    /**
     * The daily label (decision D32) — not a generator difficulty, but a session may
     * legitimately carry it: the day's puzzle is generated at a fixed engine setting
     * and stored under its own label so the stats row reads `DAILY`.
     */
    private val KNOWN_LABELS = WordsearchGenerator.DIFFICULTIES + WordsearchViewModel.DAILY_NAME

    fun stateOrNull(session: WordsearchSession): WordsearchState? {
        if (session.difficulty !in KNOWN_LABELS) return null
        if (session.elapsedMs < 0 || session.mistakes < 0) return null
        if (session.placements.isEmpty()) return null

        val letters = session.letters.map { it.toChar() }

        // Every placement triple must be well-formed and spell its word on this grid
        // — the one check that makes restore a validation rather than a formality.
        val placements = ArrayList<Placement>(session.placements.size)
        for (entry in session.placements) {
            val placement = decodePlacement(entry, letters) ?: return null
            placements += placement
        }
        // Words are distinct in the pool, so they must be distinct on the board.
        if (placements.map { it.word }.toSet().size != placements.size) return null

        val state = WordsearchState(
            letters = letters,
            placements = placements,
            difficulty = session.difficulty,
            foundCells = session.found,
            elapsedMs = session.elapsedMs,
        )
        // A finished grid is not an in-progress game.
        if (state.isComplete) return null
        return state
    }

    /** Parses `WORD:start:end` against [letters], or null when it does not spell. */
    private fun decodePlacement(entry: String, letters: List<Char>): Placement? {
        val parts = entry.split(":")
        if (parts.size != 3) return null
        val word = parts[0]
        val start = parts[1].toIntOrNull() ?: return null
        val end = parts[2].toIntOrNull() ?: return null
        if (word.isEmpty() || word.length < 2) return null
        if (start !in 0 until WordsearchRules.CELLS) return null
        if (end !in 0 until WordsearchRules.CELLS) return null

        // Straight line only, either direction of travel.
        val dr = WordsearchRules.rowOf(end) - WordsearchRules.rowOf(start)
        val dc = WordsearchRules.colOf(end) - WordsearchRules.colOf(start)
        if (dr != 0 && dc != 0 && kotlin.math.abs(dr) != kotlin.math.abs(dc)) return null
        val span = maxOf(kotlin.math.abs(dr), kotlin.math.abs(dc))
        if (span + 1 != word.length) return null

        val stepR = if (dr == 0) 0 else dr / kotlin.math.abs(dr)
        val stepC = if (dc == 0) 0 else dc / kotlin.math.abs(dc)
        val cells = WordsearchRules.line(start, stepR, stepC, word.length) ?: return null
        if (cells.last() != end) return null

        // The grid must spell the word along the cells — in walk order.
        if (WordsearchRules.spell(letters, cells) != word) {
            val backward = cells.asReversed()
            if (WordsearchRules.spell(letters, backward) != word) return null
            // The walk ran last-letter-first; normalise to first-letter-first cells.
            return Placement(word = word, cells = backward, reversed = true)
        }
        return Placement(word = word, cells = cells, reversed = false)
    }
}
