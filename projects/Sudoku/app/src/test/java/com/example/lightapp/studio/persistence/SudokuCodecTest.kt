package com.example.lightapp.studio.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The D7 encoding. Pure JVM, no emulator.
 *
 * The load-bearing test is [a given survives a restore intact] — that is the property
 * Requirement 16 criteria 6 and 10 exist to guarantee, and the reason givens and entries
 * are stored as separate fields.
 */
class SudokuCodecTest {

    private val sample = List(81) { i -> i % 10 }

    @Test
    fun `grid round-trips`() {
        val encoded = SudokuCodec.encodeGrid(sample)
        assertEquals(81, encoded.length)
        assertEquals(sample, SudokuCodec.decodeGrid(encoded))
    }

    @Test
    fun `a given survives a restore intact`() {
        val givens = List(81) { i -> if (i % 3 == 0) (i % 9) + 1 else 0 }
        val entries = List(81) { i -> if (i % 3 == 1) (i % 9) + 1 else 0 }

        val session = SudokuSession(
            givens = givens,
            entries = entries,
            notes = SudokuCodec.emptyGrid(),
            elapsedMs = 123_456L,
            difficulty = "Moderate",
            mistakes = 2,
        )

        val restored = SudokuSession(
            givens = SudokuCodec.decodeGrid(SudokuCodec.encodeGrid(session.givens))!!,
            entries = SudokuCodec.decodeGrid(SudokuCodec.encodeGrid(session.entries))!!,
            notes = SudokuCodec.decodeNotes(SudokuCodec.encodeNotes(session.notes))!!,
            elapsedMs = session.elapsedMs,
            difficulty = session.difficulty,
            mistakes = session.mistakes,
        )

        assertEquals(session, restored)

        // The property that matters: every given holds its value, and no player entry
        // occupies a cell that a given occupies.
        givens.forEachIndexed { i, given ->
            assertEquals(given, restored.givens[i])
            if (given != 0) assertEquals(0, restored.entries[i])
        }
    }

    @Test
    fun `notes round-trip through bit masks`() {
        var mask = 0
        mask = SudokuCodec.toggleNote(mask, 1)
        mask = SudokuCodec.toggleNote(mask, 5)
        mask = SudokuCodec.toggleNote(mask, 9)

        val notes = List(81) { i -> if (i == 40) mask else 0 }
        val restored = SudokuCodec.decodeNotes(SudokuCodec.encodeNotes(notes))

        assertEquals(notes, restored)
        assertEquals(listOf(1, 5, 9), SudokuCodec.notesIn(restored!![40]))
    }

    @Test
    fun `toggling a note twice clears it`() {
        val once = SudokuCodec.toggleNote(0, 7)
        assertTrue(SudokuCodec.hasNote(once, 7))
        assertEquals(0, SudokuCodec.toggleNote(once, 7))
    }

    // ─── malformed input yields null, never an exception ──────────────────────

    @Test
    fun `a short grid is rejected`() = assertNull(SudokuCodec.decodeGrid("123"))

    @Test
    fun `a non-numeric grid is rejected`() =
        assertNull(SudokuCodec.decodeGrid("x".repeat(81)))

    @Test
    fun `a null grid is rejected`() = assertNull(SudokuCodec.decodeGrid(null))

    @Test
    fun `notes with the wrong cell count are rejected`() =
        assertNull(SudokuCodec.decodeNotes("1,2,3"))

    @Test
    fun `notes with an out-of-range mask are rejected`() =
        assertNull(SudokuCodec.decodeNotes(List(81) { "9999" }.joinToString(",")))

    @Test
    fun `an empty notes string is rejected rather than treated as empty notes`() =
        assertNull(SudokuCodec.decodeNotes(""))

    @Test
    fun `a session must hold exactly 81 cells`() {
        val tooFew = List(80) { 0 }
        val threw = try {
            SudokuSession(tooFew, tooFew, tooFew, 0L, "Moderate", 0); false
        } catch (e: IllegalArgumentException) {
            true
        }
        assertTrue("A malformed session must fail loudly at construction", threw)
    }
}
