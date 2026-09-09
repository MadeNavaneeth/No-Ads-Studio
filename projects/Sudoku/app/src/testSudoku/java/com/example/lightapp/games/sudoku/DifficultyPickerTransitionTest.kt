package com.example.lightapp.games.sudoku

import com.example.lightapp.studio.persistence.SudokuCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The difficulty picker's state machine.
 *
 * These exist because of a reproducible dead end: from a finished puzzle, `NEW` then
 * `CANCEL` left the UI on "GENERATING" with no generator running, no button, and no
 * recovery — the view model outlives the screen, so leaving and returning did not clear
 * it either. The cause was the picker recording only a `Playing` state as its origin, so
 * every other origin became `null` and dismissal fell through to `Generating`.
 *
 * The invariant asserted throughout: **dismissal is always defined.** It either names a
 * real destination or asks for a fresh puzzle. It never lands on `Generating`.
 */
class DifficultyPickerTransitionTest {

    private fun playing(difficulty: String = "Hard") = SudokuUi.Playing(state(difficulty))

    private fun complete(difficulty: String = "Easy") = SudokuUi.Complete(state(difficulty))

    private fun state(difficulty: String) = SudokuState(
        givens = SudokuCodec.emptyGrid(),
        entries = SudokuCodec.emptyGrid(),
        notes = SudokuCodec.emptyGrid(),
        difficulty = difficulty,
    )

    // ─── the regression ───────────────────────────────────────────────────────

    @Test
    fun `cancelling from a finished puzzle returns to it, not to Generating`() {
        // The exact two-tap sequence that used to strand the UI.
        val finished = complete()

        val picker = DifficultyPickerTransition.open(finished)
        val destination = DifficultyPickerTransition.dismiss(picker)

        assertSame(finished, destination)
        assertNotEquals(SudokuUi.Generating, destination)
    }

    @Test
    fun `no origin ever dismisses to Generating`() {
        val origins = listOf(
            playing(),
            complete(),
            SudokuUi.GenerationFailed,
            SudokuUi.Generating,
        )

        for (origin in origins) {
            val destination = DifficultyPickerTransition.dismiss(
                DifficultyPickerTransition.open(origin)
            )
            assertNotEquals(
                "dismissing a picker opened from $origin",
                SudokuUi.Generating,
                destination,
            )
        }
    }

    // ─── destinations ─────────────────────────────────────────────────────────

    @Test
    fun `cancelling from a game in progress returns to that exact game`() {
        val inProgress = playing()

        val destination = DifficultyPickerTransition.dismiss(
            DifficultyPickerTransition.open(inProgress)
        )

        assertSame(inProgress, destination)
    }

    @Test
    fun `cancelling from a failed generation returns to the failure`() {
        // The retry button has to still be there.
        val destination = DifficultyPickerTransition.dismiss(
            DifficultyPickerTransition.open(SudokuUi.GenerationFailed)
        )

        assertEquals(SudokuUi.GenerationFailed, destination)
    }

    @Test
    fun `opening while generating records no destination, so dismissal generates`() {
        // Opening the picker cancels the in-flight generation, so there is nothing to go
        // back to. null is the caller's signal to generate rather than to wait.
        val picker = DifficultyPickerTransition.open(SudokuUi.Generating)

        assertNull(picker.returnTo)
        assertNull(DifficultyPickerTransition.dismiss(picker))
    }

    // ─── the displayed current difficulty ─────────────────────────────────────

    @Test
    fun `the picker shows the difficulty of the game it was opened over`() {
        assertEquals("Hard", DifficultyPickerTransition.open(playing("Hard")).currentDifficulty)
        assertEquals("Easy", DifficultyPickerTransition.open(complete("Easy")).currentDifficulty)
    }

    @Test
    fun `the picker shows no current difficulty when there is no game`() {
        assertNull(DifficultyPickerTransition.open(SudokuUi.Generating).currentDifficulty)
        assertNull(DifficultyPickerTransition.open(SudokuUi.GenerationFailed).currentDifficulty)
    }

    @Test
    fun `reopening over an existing picker does not nest or lose the difficulty`() {
        // Guards against a double tap on NEW burying the real destination behind a picker.
        val first = DifficultyPickerTransition.open(playing("Challenge"))
        val second = DifficultyPickerTransition.open(first)

        assertEquals("Challenge", second.currentDifficulty)
        assertNull("a picker is never a destination", second.returnTo)
    }
}
