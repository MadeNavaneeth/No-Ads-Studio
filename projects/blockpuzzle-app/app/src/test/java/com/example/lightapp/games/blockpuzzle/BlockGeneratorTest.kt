package com.example.lightapp.games.blockpuzzle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator's contract, asserted rather than assumed. The load-bearing guarantee
 * is decision D33's placeable deal: every dealt set admits at least one legal
 * placement per piece **in some order on the board it was dealt onto** — so a run
 * can only end by accumulated placement, never by a dead deal.
 */
class BlockGeneratorTest {

    @Test
    fun `a deal holds three pieces with valid shapes`() {
        val deal = requireNotNull(BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = 1))
        assertEquals(BlockGenerator.TRAY, deal.size)
        for (piece in deal) {
            assertTrue(BlockRules.CATALOG.containsKey(piece.family))
            assertTrue(piece.variant in BlockRules.CATALOG.getValue(piece.family).indices)
            assertTrue(piece.density in BlockRules.DENSITIES.indices)
        }
    }

    @Test
    fun `every dealt piece fits on the empty board`() {
        // On an empty board the D33 set-contract reduces to per-piece fit, which is
        // the strongest and most checkable form of the guarantee.
        repeat(20) { seed ->
            val deal = requireNotNull(BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = seed.toLong()))
            for (piece in deal) {
                assertTrue(
                    "seed $seed piece ${piece.family}",
                    BlockRules.placements(List(BlockRules.CELLS) { BlockRules.EMPTY }, piece).isNotEmpty(),
                )
            }
        }
    }

    @Test
    fun `same seed same deal different seed different deal`() {
        val a = BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = 42)
        val b = BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = 42)
        val c = BlockGenerator.deal(List(BlockRules.CELLS) { BlockRules.EMPTY }, seed = 43)
        assertEquals(a, b)
        assertNotNull(c)
        assertTrue(
            "two seeds should rarely deal identically",
            a != c,
        )
    }

    @Test
    fun `a crowded board still deals a playable set or reports game over honestly`() {
        // Board with one empty cell: no piece beyond the 1×1 can land anywhere, so
        // the generator must return null — the game-over signal — rather than deal
        // an unplayable set.
        val cells = List(BlockRules.CELLS) { BlockRules.FILLED }.toMutableList()
        cells[BlockRules.CELLS - 1] = BlockRules.EMPTY
        val deal = BlockGenerator.deal(cells.toList())
        assertTrue("a one-cell board must refuse a deal (null = game over)", deal == null)
    }

    @Test
    fun `the daily deal is deterministic per day`() {
        val day = 20_660L
        assertEquals(
            BlockGenerator.dailyDeal(day),
            BlockGenerator.dailyDeal(day),
        )
        assertNotNull(BlockGenerator.dailyDeal(day))
    }
}
