package com.example.lightapp.games.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Rules for `connect(7)`. Pure JVM tests, milliseconds each — the cheapest place to
 * find a bug in this repo.
 */
class ConnectRulesTest {

    /** Builds a board from `index to value` placements; everything else is empty. */
    private fun cells(vararg placements: Pair<Int, Int>): List<Int> {
        val board = MutableList(ConnectRules.CELLS) { ConnectRules.EMPTY }
        for ((index, value) in placements) board[index] = value
        return board
    }

    private val e1 = ConnectRules.endpointOf(1)
    private val e2 = ConnectRules.endpointOf(2)
    private val p1 = ConnectRules.pathOf(1)
    private val p2 = ConnectRules.pathOf(2)

    @Test
    fun `start begins a walk on an endpoint and nowhere else`() {
        val board = cells(0 to e1, 6 to e1)
        assertEquals(listOf(0), ConnectRules.start(board, 0))
        assertNull(ConnectRules.start(board, 3))
    }

    @Test
    fun `start is refused on a finished board`() {
        val solved = solvedBoard()
        assertNull(ConnectRules.start(solved, 0))
    }

    @Test
    fun `step lays a segment and stepping back retracts it`() {
        val board = cells(0 to e1, 6 to e1)
        val path = requireNotNull(ConnectRules.start(board, 0))

        val laid = requireNotNull(ConnectRules.step(board, path, 1))
        assertEquals(p1, laid.cells[1])
        assertEquals(listOf(0, 1), laid.path)
        assertTrue(laid.changed)

        val retracted = requireNotNull(ConnectRules.step(board, laid.path, 0))
        assertEquals(ConnectRules.EMPTY, retracted.cells[1])
        assertEquals(listOf(0), retracted.path)
        assertTrue(retracted.changed)
    }

    @Test
    fun `stepping onto the previous cell trims the tail back`() {
        var board = cells(0 to e1, 6 to e1)
        var path = listOf(0)
        for (i in 1..4) {
            val step = requireNotNull(ConnectRules.step(board, path, i))
            board = step.cells
            path = step.path
        }
        assertEquals(p1, board[4])

        // The head is 4; stepping onto 3 (adjacent, earlier in the walk) trims 4.
        val trimmed = requireNotNull(ConnectRules.step(board, path, 3))
        assertEquals(ConnectRules.EMPTY, trimmed.cells[4])
        assertEquals(p1, trimmed.cells[3])
        assertEquals(listOf(0, 1, 2, 3), trimmed.path)
    }

    @Test
    fun `arrival at the partner completes the walk without rewriting the endpoint`() {
        val board = cells(0 to e1, 2 to e1)
        val path = requireNotNull(ConnectRules.start(board, 0))
        val laid = requireNotNull(ConnectRules.step(board, path, 1))
        val arrived = requireNotNull(ConnectRules.step(laid.cells, laid.path, 2))

        assertEquals(listOf(0, 1, 2), arrived.path)
        assertEquals(e1, arrived.cells[2]) // the endpoint value stays an endpoint
        assertFalse(arrived.changed)
    }

    @Test
    fun `a laid segment of another pair is refused`() {
        val board = cells(0 to e1, 2 to e1, 7 to e2, 9 to e2)
        // Pair one lays across cell 1 (the segment of the walk 0 → 1).
        val laid = requireNotNull(ConnectRules.step(board, listOf(0), 1))
        assertEquals(p1, laid.cells[1])

        // Pair two's walk may not adopt pair one's segment.
        val path2 = requireNotNull(ConnectRules.start(board, 7))
        assertNull(ConnectRules.step(laid.cells, path2, 1))
    }

    @Test
    fun `a non-adjacent target is refused`() {
        val board = cells(0 to e1, 6 to e1)
        val path = requireNotNull(ConnectRules.start(board, 0))
        assertNull(ConnectRules.step(board, path, 15))
    }

    @Test
    fun `an own distant segment is adopted rather than refused`() {
        // A restored pair may have segments its new walk does not start from: laying
        // 1-2, ending the walk, then starting again from 0 and stepping to 1 adopts.
        val board = cells(0 to e1, 1 to p1, 2 to e1)
        val path = requireNotNull(ConnectRules.start(board, 0))
        val adopted = requireNotNull(ConnectRules.step(board, path, 1))
        assertEquals(listOf(0, 1), adopted.path)
        assertFalse(adopted.changed) // nothing was laid; it was already there
    }

    @Test
    fun `routeFor demands one connected chain over every segment`() {
        val good = cells(0 to e1, 1 to p1, 2 to e1)
        assertNotNull(ConnectRules.routeFor(good, 1))

        // An orphaned segment away from the route.
        val orphan = cells(0 to e1, 2 to e1, 40 to p1)
        assertNull(ConnectRules.routeFor(orphan, 1))

        // No segments at all: the pair has not been started.
        assertNull(ConnectRules.routeFor(cells(0 to e1, 2 to e1), 1))
    }

    @Test
    fun `win demands full cover and real routes`() {
        val solved = solvedBoard()
        assertTrue(ConnectRules.isWin(solved))
        assertEquals(0, solved.count { it == ConnectRules.EMPTY })

        // Corrupt one cell onto the wrong pair's route: full cover, broken route.
        val corrupted = solved.toMutableList()
        corrupted[20] = p1 // was pair two's snake
        assertFalse(ConnectRules.isWin(corrupted))

        // One hole: full routes impossible.
        val holed = solved.toMutableList()
        holed[24] = ConnectRules.EMPTY
        assertFalse(ConnectRules.isWin(holed))
    }

    @Test
    fun `completion counts used cells`() {
        assertEquals(0f, ConnectRules.completion(List(ConnectRules.CELLS) { ConnectRules.EMPTY }))
        assertEquals(2f / ConnectRules.CELLS, ConnectRules.completion(cells(0 to e1, 2 to e1)))
    }

    @Test
    fun `neighbours are orthogonal only`() {
        assertEquals(setOf(1, 7), ConnectRules.neighbours(0).toSet())
        assertEquals(setOf(2, 8, 10, 16), ConnectRules.neighbours(9).toSet())
        assertEquals(setOf(41, 47), ConnectRules.neighbours(48).toSet())
    }

    @Test
    fun `retraction clears to empty, never to a phantom value`() {
        // The value 8 (pathOf(0)) once leaked into cleared cells — an adjacency
        // ghost. Lay, retract, and confirm the cell is exactly EMPTY.
        val board = cells(0 to e1, 6 to e1)
        val laid = requireNotNull(ConnectRules.step(board, listOf(0), 1))
        val retracted = requireNotNull(ConnectRules.step(laid.cells, laid.path, 0))
        assertEquals(ConnectRules.EMPTY, retracted.cells[1])
    }
}

/**
 * A fully solved 7×7: pair one is column 0, pair two is the snake over columns 1–6.
 * Every cell used, both routes real — the test-suite's shared "known good" board.
 */
internal fun solvedBoard(): List<Int> {
    val board = MutableList(ConnectRules.CELLS) { ConnectRules.EMPTY }
    for (r in 0 until ConnectRules.SIZE) {
        board[r * ConnectRules.SIZE] = if (r == 0 || r == ConnectRules.SIZE - 1) {
            ConnectRules.endpointOf(1)
        } else {
            ConnectRules.pathOf(1)
        }
    }
    val snake = ArrayList<Int>()
    for (r in 0 until ConnectRules.SIZE) {
        val cols = if (r % 2 == 0) (1 until ConnectRules.SIZE) else (ConnectRules.SIZE - 1 downTo 1)
        for (c in cols) snake += r * ConnectRules.SIZE + c
    }
    for (cell in snake) {
        board[cell] = if (cell == snake.first() || cell == snake.last()) {
            ConnectRules.endpointOf(2)
        } else {
            ConnectRules.pathOf(2)
        }
    }
    return board
}
