package com.example.lightapp.games.blockpuzzle

import kotlin.random.Random

/**
 * The deal for `blockpuzzle(8)`.
 *
 * Block Puzzle is endless — there is no puzzle to generate and no solution to hold.
 * What the generator owns is the **deal**: three tray pieces, dealt under decision
 * D33's placeable-deal contract, the honest analogue of "puzzle validity" for a game
 * that has no solution:
 *
 * > Every dealt set admits at least one legal placement at deal time, *for the board
 * > it is dealt onto*. A run can therefore only end by accumulated placement — the
 * > player filling ground until nothing fits — never by an instantly-dead deal.
 *
 * The contract is checked against the **live board** at deal time, which is why
 * `deal` takes the cells: a tray refilled onto a crowded board is exactly the case
 * where a naive random deal could hand out three pieces that all refuse to land.
 *
 * Deterministic per seed, like every generator here: the same seed and the same
 * board deal the same tray, which is what the daily puzzle (decision D32) rests on.
 */
object BlockGenerator {

    /** Tray size: three pieces at a time, the genre's constant. */
    const val TRAY = 3

    val DIFFICULTIES = listOf("Classic")

    const val DEFAULT_DIFFICULTY = "Classic"

    /**
     * Deals [TRAY] pieces playable on [cells], or `null` if [attempts] tries cannot
     * produce one — in practice unreachable: when the board is so crowded that no
     * full catalog sweep finds a playable set, the game is over by any honest
     * reading, and the caller treats `null` as exactly that.
     *
     * The sweep is ordered: candidates are shuffled, then the dealer picks one piece
     * at a time, requiring only that the *set* admits one placement per piece in some
     * order — the D33 contract, which is strictly weaker (and more honest) than
     * requiring each piece to fit in isolation on the untouched board.
     */
    fun deal(cells: List<Int>, seed: Long? = null, attempts: Int = 32): List<BlockRules.Piece>? {
        val random = if (seed == null) Random.Default else Random(seed)
        repeat(attempts) {
            val deal = ArrayList<BlockRules.Piece>(TRAY)
            var work = cells
            var complete = true
            for (slot in 0 until TRAY) {
                val (family, variant) = BlockRules.ALL_SHAPES.random(random)
                val piece = BlockRules.Piece(
                    family = family,
                    variant = variant,
                    density = BlockRules.DENSITIES[slot % BlockRules.DENSITIES.size],
                )
                // Set-level check: this piece must fit on the board *after* the
                // earlier pieces of this deal have hypothetically landed. On an
                // empty board that reduces to "each piece fits", which is the
                // contract's well-known good case. A piece that fits nowhere
                // voids the whole attempt — a short deal would be a tray that
                // empties one piece early, an off-by-N the win/over logic must
                // never have to guess about.
                val anchor = BlockRules.placements(work, piece).firstOrNull()
                if (anchor == null) {
                    complete = false
                    break
                }
                work = BlockRules.placed(work, piece, anchor.first, anchor.second)
                deal += piece
            }
            if (complete) return deal
        }
        return null
    }

    /**
     * The daily deal (decision D32): the day's tray, seeded with the UTC epoch day.
     * The board at deal time is the day's opening board — always empty, since a daily
     * run starts fresh — so the contract holds trivially and every device deals the
     * same three pieces for the whole day.
     */
    fun dailyDeal(seed: Long): List<BlockRules.Piece>? =
        deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = seed)
}
