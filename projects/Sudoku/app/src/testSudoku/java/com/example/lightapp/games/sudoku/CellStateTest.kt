package com.example.lightapp.games.sudoku

import com.example.lightapp.designsystem.components.CellState
import com.example.lightapp.studio.persistence.SudokuCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a cell decides what it looks like, and which conflict gets the red.
 *
 * Two rules are load-bearing here and both were broken:
 *
 * 1. A given must resolve to [CellState.Given], because that is the state `GridCell` uses
 *    to refuse taps. Any state that outranks it makes a given selectable.
 * 2. At most one cell may be red (Requirement 14 criterion 2, decision D2), and it must be
 *    a cell the player can actually fix.
 */
class CellStateTest {

    private fun state(
        givens: Map<Int, Int> = emptyMap(),
        entries: Map<Int, Int> = emptyMap(),
        lastEntered: Int? = null,
    ) = SudokuState(
        givens = grid(givens),
        entries = grid(entries),
        notes = SudokuCodec.emptyGrid(),
        difficulty = "Moderate",
        lastEntered = lastEntered,
    )

    private fun grid(values: Map<Int, Int>): List<Int> =
        List(Conflicts.CELLS) { i -> values[i] ?: 0 }

    /** The mapping as the grid actually applies it: filtered conflicts, chosen accent. */
    private fun render(state: SudokuState): List<CellState> {
        val conflicts = playerConflicts(state)
        val accented = accentedConflict(state, conflicts)
        return List(Conflicts.CELLS) { i -> cellStateFor(state, i, conflicts, accented) }
    }

    // ─── givens are untouchable ───────────────────────────────────────────────

    @Test
    fun `a given conflicting with a player entry stays Given`() {
        // Cell 0 is a given 5; the player puts a 5 at cell 1, in the same row and box.
        // Both are "in conflict", but only cell 1 is the mistake.
        val s = state(givens = mapOf(0 to 5), entries = mapOf(1 to 5), lastEntered = 1)

        val rendered = render(s)

        assertEquals(CellState.Given, rendered[0])
        assertEquals(CellState.ConflictAccented, rendered[1])
    }

    @Test
    fun `a given is never reported as a conflict`() {
        val s = state(givens = mapOf(0 to 5), entries = mapOf(1 to 5))

        assertTrue(
            "raw conflict detection still sees both ends",
            0 in s.conflicts && 1 in s.conflicts,
        )
        assertEquals(setOf(1), playerConflicts(s))
    }

    @Test
    fun `the accent never lands on a given even when it is lowest in reading order`() {
        // This is the ordering bug: cell 0 sorts first, so minOrNull used to pick the
        // given and point the single red accent at the wrong cell.
        val s = state(givens = mapOf(0 to 5), entries = mapOf(1 to 5), lastEntered = null)

        assertEquals(1, accentedConflict(s, playerConflicts(s)))
    }

    // ─── at most one red ──────────────────────────────────────────────────────

    @Test
    fun `at most one cell is accented however many conflicts exist`() {
        // Four player 7s in the top row: six conflicting pairs, one accent.
        val s = state(
            entries = mapOf(0 to 7, 1 to 7, 2 to 7, 3 to 7),
            lastEntered = 3,
        )

        val accentedCount = render(s).count { it == CellState.ConflictAccented }

        assertEquals(1, accentedCount)
    }

    @Test
    fun `the most recently entered conflicting cell takes the accent`() {
        val s = state(entries = mapOf(0 to 7, 8 to 7), lastEntered = 8)

        val rendered = render(s)

        assertEquals(CellState.ConflictAccented, rendered[8])
        assertEquals(CellState.ConflictMuted, rendered[0])
    }

    @Test
    fun `when the last entry is not in conflict the first conflict in reading order wins`() {
        // The player's most recent move was fine; two older entries clash.
        val s = state(entries = mapOf(0 to 7, 8 to 7, 40 to 3), lastEntered = 40)

        val rendered = render(s)

        assertEquals(CellState.ConflictAccented, rendered[0])
        assertEquals(CellState.ConflictMuted, rendered[8])
    }

    @Test
    fun `a clean board has no accent and no red`() {
        // Decision D2: a screen in normal play has no red on it.
        val s = state(givens = mapOf(0 to 5), entries = mapOf(1 to 6))

        assertNull(accentedConflict(s, playerConflicts(s)))
        assertEquals(0, render(s).count { it == CellState.ConflictAccented })
    }

    // ─── the ordinary states ──────────────────────────────────────────────────

    @Test
    fun `an untouched cell is Empty and a player value is Entered`() {
        val s = state(givens = mapOf(0 to 5), entries = mapOf(1 to 6))

        val rendered = render(s)

        assertEquals(CellState.Given, rendered[0])
        assertEquals(CellState.Entered, rendered[1])
        assertEquals(CellState.Empty, rendered[2])
    }

    @Test
    fun `cellStateFor keeps a given uneditable even if handed unfiltered conflicts`() {
        // Defends the branch ordering itself, independently of playerConflicts.
        val s = state(givens = mapOf(0 to 5), entries = mapOf(1 to 5))

        val unfiltered = s.conflicts // includes the given at 0

        assertEquals(CellState.Given, cellStateFor(s, 0, unfiltered, accented = 0))
    }
}

/**
 * Selection highlighting — the two layers, composed (sudoku-market-study.md §5A, amending
 * D30).
 *
 * Every selection lights its **cross** (the row, column and box of the tapped cell); a
 * selection holding a digit additionally lights its **siblings** (the other cells holding
 * that digit). The cases that matter are the boundaries: nothing selected, an empty
 * selection, and a filled selection with no partners.
 */
class RelatedCellsTest {

    private fun state(
        givens: Map<Int, Int> = emptyMap(),
        entries: Map<Int, Int> = emptyMap(),
        selected: Int? = null,
    ) = SudokuState(
        givens = List(Conflicts.CELLS) { i -> givens[i] ?: 0 },
        entries = List(Conflicts.CELLS) { i -> entries[i] ?: 0 },
        notes = SudokuCodec.emptyGrid(),
        difficulty = "Moderate",
        selected = selected,
    )

    @Test
    fun `nothing selected highlights nothing`() {
        assertTrue(crossOf(state()).isEmpty())
        assertTrue(siblingsOf(state()).isEmpty())
    }

    @Test
    fun `selecting a filled cell lights its cross and every other cell holding that digit`() {
        // 7s at 0, 40 and 80; a 3 at 5 must not join them.
        val s = state(entries = mapOf(0 to 7, 40 to 7, 80 to 7, 5 to 3), selected = 0)

        assertEquals(setOf(40, 80), siblingsOf(s))
        assertEquals(Conflicts.peersOf(0).toSet(), crossOf(s))
    }

    @Test
    fun `a sibling inside the cross is lit by both layers`() {
        // Cell 10 shares box 0 with the selection at 0, so a 7 there is both a peer
        // (cross) and a same-digit cell (sibling) at once.
        val s = state(entries = mapOf(0 to 7, 10 to 7), selected = 0)

        assertTrue(10 in crossOf(s))
        assertTrue(10 in siblingsOf(s))
    }

    @Test
    fun `the selected cell is never in its own highlight sets`() {
        val s = state(entries = mapOf(0 to 7, 40 to 7), selected = 0)

        assertFalse(0 in crossOf(s))
        assertFalse(0 in siblingsOf(s))
    }

    @Test
    fun `a digit matches whether it is a given or a player entry`() {
        // The player is looking for fives, not for who placed them.
        val s = state(givens = mapOf(40 to 5), entries = mapOf(0 to 5), selected = 0)

        assertEquals(setOf(40), siblingsOf(s))
    }

    @Test
    fun `selecting the only instance of a digit leaves the sibling layer empty`() {
        // The cross still lights — a lone digit still sits inside a constraint.
        val s = state(entries = mapOf(0 to 7), selected = 0)

        assertTrue(siblingsOf(s).isEmpty())
        assertEquals(Conflicts.peersOf(0).toSet(), crossOf(s))
    }

    @Test
    fun `selecting an empty cell lights its twenty peers and no siblings`() {
        val s = state(selected = 0)

        val cross = crossOf(s)

        assertEquals(20, cross.size)
        assertEquals(Conflicts.peersOf(0).toSet(), cross)
        assertFalse("its own cell is not a peer", 0 in cross)
        assertTrue(siblingsOf(s).isEmpty())
    }

    @Test
    fun `an empty cell lights its cross regardless of what is on the board`() {
        // Cell 1 is empty; the 7s elsewhere are irrelevant to what can go in it.
        val s = state(entries = mapOf(40 to 7, 80 to 7), selected = 1)

        assertEquals(Conflicts.peersOf(1).toSet(), crossOf(s))
        assertTrue(siblingsOf(s).isEmpty())
    }
}
