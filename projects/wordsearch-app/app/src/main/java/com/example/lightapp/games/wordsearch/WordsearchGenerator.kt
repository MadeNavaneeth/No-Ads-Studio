package com.example.lightapp.games.wordsearch

import kotlin.random.Random

/**
 * Grids for `wordsearch(12)`.
 *
 * ## Placement, not luck
 *
 * Words are placed one at a time with **scored trials**: for each word, try up to
 * N random positions/directions, score each legal candidate by the overlap it
 * earns (letters it shares with words already down), and take the best — falling
 * back to a non-overlapping spot when nothing overlaps. This is what makes crossing
 * grids the rule rather than the accident, without a repair pass.
 *
 * ## Fairness by construction
 *
 * - Words never run through another word's cells *unless* they agree letter-for-letter
 *   on every shared cell — the classic rule that keeps a placed word readable.
 * - Words never lie on top of each other with different spellings.
 * - The pool ([WordList]) is all-caps and distinct, so matching is case-exact.
 * - [WordsearchRules.evaluate] matches whole lines, so a player tracing a shared
 *   line can legitimately find two words with one drag.
 *
 * ## Why this is the studio's first data-consuming game
 *
 * Everything before it generated its content. This one bundles a word pool
 * ([WordList]), which spends the first kilobytes of the 512 KB embedded-data budget
 * (roadmap §3) and proves the pipeline: bundled data + generated layout.
 *
 * Deterministic per seed — the same seed lays the same words, fills the same filler
 * letters, and produces the same grid, which is what makes a saved board re-derivable.
 */
object WordsearchGenerator {

    const val DEFAULT_DIFFICULTY = "Moderate"
    val DIFFICULTIES = listOf("Simple", "Moderate", "Hard")

    /** Words per grid. The grid is 12×12; eight-to-twelve words keeps hunts dense but fair. */
    private val WORD_COUNTS = mapOf(
        "Simple" to 7,
        "Moderate" to 9,
        "Hard" to 12,
    )

    fun wordCountFor(difficulty: String): Int =
        WORD_COUNTS[difficulty] ?: WORD_COUNTS.getValue(DEFAULT_DIFFICULTY)

    /** Directions per level: Simple reads any way but stays 4-directional; the rest go diagonal too. */
    private fun directionsFor(difficulty: String, random: Random): List<Pair<Int, Int>> =
        if (difficulty == "Simple") {
            WordsearchRules.DIRECTIONS.take(4).shuffled(random)
        } else {
            WordsearchRules.DIRECTIONS.shuffled(random)
        }

    /** Trials per word before giving up on that word. High enough that it never fires. */
    private const val TRIALS_PER_WORD = 220

    /** Rejection budget for the whole grid; a reroll is cheaper than a bad grid. */
    private const val MAX_ATTEMPTS = 30

    /**
     * A playable grid, or null when none landed in budget.
     *
     * The letters are generated for the *start* of the game: the player has not
     * earned the word list's secret cells yet, but the letters are all on the table —
     * a word search hides nothing but the words' positions.
     */
    fun generate(difficulty: String, seed: Long? = null): Board? {
        val wordCount = wordCountFor(difficulty)
        if (wordCount !in 1..WordList.words.size) return null

        val random = if (seed == null) Random.Default else Random(seed)
        repeat(MAX_ATTEMPTS) {
            val board = attempt(wordCount, difficulty, random) ?: return@repeat
            return board
        }
        return null
    }

    private fun attempt(wordCount: Int, difficulty: String, random: Random): Board? {
        val directions = directionsFor(difficulty, random)
        val pool = WordList.words.shuffled(random).take(wordCount)
        val letters = MutableList(WordsearchRules.CELLS) { ' ' }
        val placements = ArrayList<Placement>(wordCount)

        for (word in pool) {
            val candidate = bestPlacement(word, directions, letters, placements, random)
                ?: return null // this word simply will not fit; reroll the grid
            // Lay the word, letter by letter.
            for ((i, cell) in candidate.cells.withIndex()) {
                letters[cell] = word[i]
            }
            placements += candidate
        }

        // Filler: uniform random letters over everything unplaced. Fillers never
        // accidentally complete a word *by the rules' own definition* — evaluate()
        // only matches placed lines, so no accidental word can ever lock.
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        for (i in 0 until WordsearchRules.CELLS) {
            if (letters[i] == ' ') letters[i] = alphabet[random.nextInt(alphabet.length)]
        }
        return Board(letters = letters.toList(), placements = placements)
    }

    /**
     * Scores every legal placement of [word] and returns the best-scoring one.
     *
     * Legality: every shared cell must already hold the same letter. Score: the
     * number of shared cells (overlap rewards crossing grids), with a small bonus
     * for length so long words claim the board's spine first. Ties broken randomly
     * so seeds stay lively.
     */
    private fun bestPlacement(
        word: String,
        directions: List<Pair<Int, Int>>,
        letters: List<Char>,
        placements: List<Placement>,
        random: Random,
    ): Placement? {
        var best: Placement? = null
        var bestScore = -1

        repeat(TRIALS_PER_WORD) {
            val dir = directions.random(random)
            val dr = dir.first
            val dc = dir.second
            val reversed = random.nextBoolean()
            val stepR = if (reversed) -dr else dr
            val stepC = if (reversed) -dc else dc

            // Random start such that the word stays on the board.
            val maxDr = (word.length - 1) * kotlin.math.abs(stepR)
            val maxDc = (word.length - 1) * kotlin.math.abs(stepC)
            if (maxDr >= WordsearchRules.SIZE || maxDc >= WordsearchRules.SIZE) return@repeat
            val startRow = random.nextInt(WordsearchRules.SIZE - maxDr)
            val startCol = random.nextInt(WordsearchRules.SIZE - maxDc)
            val start = startRow * WordsearchRules.SIZE + startCol

            val cells = WordsearchRules.line(start, stepR, stepC, word.length) ?: return@repeat

            // Legality + overlap scoring. The walk may run against the word's
            // reading order, so the letter to expect at walk step i is the
            // mirrored one — comparing `word[i]` here would let a reversed word
            // agree with a crossing word on the wrong letters and overwrite its
            // cell when laid.
            var overlap = 0
            var legal = true
            for ((i, cell) in cells.withIndex()) {
                val expected = if (reversed) word[word.length - 1 - i] else word[i]
                val existing = letters[cell]
                if (existing == ' ') continue
                if (existing == expected) {
                    overlap++
                } else {
                    legal = false
                    break
                }
            }
            if (!legal) return@repeat

            val score = overlap * 10 + word.length
            if (score > bestScore) {
                val placed = Placement(
                    word = word,
                    // First letter first, as the type promises: a reversed walk
                    // stores the walk's mirror, so every reader of `cells` can
                    // assume reading order and `reversed` stays descriptive.
                    cells = if (reversed) cells.asReversed() else cells,
                    reversed = reversed,
                )
                best = placed
                bestScore = score
            }
        }
        return best
    }
}

/**
 * A generated wordsearch: the letter grid and where the words run. Both travel to
 * the state together; the screen shows only [letters] until lines are found.
 */
data class Board(
    val letters: List<Char>,
    val placements: List<Placement>,
)
