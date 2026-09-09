package com.example.lightapp.games.minesweeper

import kotlin.random.Random

/**
 * Mine layouts for `minesweeper(10)`.
 *
 * **First-tap safety is structural, not retried.** Mines are never drawn at
 * generation time — they are placed only when the player's first reveal lands,
 * excluding that cell and (when possible) its neighbours. This is strictly better
 * than the shuffle-until-far-away loop: it terminates in one pass, and it removes
 * the classic frustration of an opening number with no zero region to grow from.
 *
 * Deterministic per seed: the same first tap against the same seed places the same
 * layout, which is what makes the layout safe to persist — a restored session
 * re-derives the identical board rather than trusting a re-shuffle.
 */
object MinesweeperGenerator {

    const val DEFAULT_DIFFICULTY = "Moderate"
    val DIFFICULTIES = listOf("Simple", "Moderate", "Hard")

    /**
     * Mine count per level. The board is 9×9 with 10 mines (roadmap §4); Simple
     * loosens it for a first game, Hard packs it toward classic-expert density.
     */
    private val MINE_COUNTS = mapOf(
        "Simple" to 8,
        "Moderate" to 10,
        "Hard" to 13,
    )

    fun mineCountFor(difficulty: String): Int =
        MINE_COUNTS[difficulty] ?: MINE_COUNTS.getValue(DEFAULT_DIFFICULTY)

    /**
     * Places [mineCount] mines uniformly at random, excluding [firstTap] and, when
     * the board is large enough to afford it, its neighbours — so the opening
     * reveal always shows a number at worst and a zero region at best.
     *
     * Returns null when the exclusions plus the mine count cannot fit the board,
     * which for this project's numbers (≤ 13 mines on 81 cells) cannot happen.
     */
    fun layoutFor(firstTap: Int, difficulty: String, seed: Long? = null): List<Boolean>? {
        val mineCount = mineCountFor(difficulty)
        if (mineCount < 0 || mineCount > MinesweeperRules.CELLS - 1) return null

        val random = if (seed == null) Random.Default else Random(seed)
        val excluded = mutableSetOf(firstTap)
        if (MinesweeperRules.CELLS - mineCount >= MinesweeperRules.neighbours(firstTap).size + 1) {
            excluded += MinesweeperRules.neighbours(firstTap)
        }
        if (mineCount > MinesweeperRules.CELLS - excluded.size) return null

        val pool = (0 until MinesweeperRules.CELLS).filter { it !in excluded }
        val mines = pool.shuffled(random).take(mineCount)
        return List(MinesweeperRules.CELLS) { it in mines }
    }
}
