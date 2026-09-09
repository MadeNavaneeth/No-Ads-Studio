package com.example.lightapp.games.nonogram

import kotlin.random.Random

/**
 * Seeded solution bitmaps with derived clues (parity N1). No bundled data, no
 * network: a bitmap is drawn from [Random], its clues are derived, and the
 * puzzle ships only when the overlap solver accepts it — every generated board
 * is solvable by deduction alone, which is the admission gate (roadmap §3).
 *
 * Deterministic per seed: the same seed draws the same stream and accepts the
 * same board, so a saved seed re-derives its exact solution after process
 * death. difficulties and [DEFAULT_DIFFICULTY] feed the shell's generic picker.
 */
object NonogramGenerator {

    const val DEFAULT_DIFFICULTY = "Moderate"
    val DIFFICULTIES = listOf("Simple", "Moderate", "Hard")

    /**
     * Solution density per level. Sparse enough to read, dense enough to constrain.
     * Acceptance is measured, not assumed — see NonogramGeneratorTest.
     */
    private val DENSITIES = mapOf(
        "Simple" to 0.44,
        "Moderate" to 0.37,
        "Hard" to 0.30,
    )

    /** Solutions outside this fill range never ship: empty is an instant win, full is noise. */
    const val MIN_FILLS = 20
    const val MAX_FILLS = 80

    private const val MAX_ATTEMPTS = 300

    /**
     * A deduction-solvable solution bitmap, or null when none landed in budget.
     *
     * The left half is drawn and mirrored: symmetric boards are what picture
     * nonograms look like, and — measured, not assumed — correlation across the
     * mirror accepts an order of magnitude more often than coin flips (6–34%
     * versus 0–2% per attempt across the three densities).
     */
    fun generate(difficulty: String, seed: Long? = null): List<Int>? {
        val density = DENSITIES[difficulty] ?: DENSITIES.getValue(DEFAULT_DIFFICULTY)
        val random = if (seed == null) Random.Default else Random(seed)
        val half = NonogramRules.SIZE / 2
        repeat(MAX_ATTEMPTS) {
            val left = List(NonogramRules.CELLS / 2) {
                if (random.nextDouble() < density) 1 else 0
            }
            val candidate = List(NonogramRules.CELLS) { i ->
                val c = i % NonogramRules.SIZE
                left[(i / NonogramRules.SIZE) * half + minOf(c, NonogramRules.SIZE - 1 - c)]
            }
            val fills = candidate.count { it == 1 }
            if (fills in MIN_FILLS..MAX_FILLS && NonogramRules.isDeducible(candidate)) {
                return candidate
            }
        }
        return null
    }
}
