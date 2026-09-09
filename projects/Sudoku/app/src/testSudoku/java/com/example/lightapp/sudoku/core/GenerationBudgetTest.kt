package com.example.lightapp.sudoku.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Roadmap §3 admission: the default difficulty (Moderate) generates in ≤ 2 s,
 * slowest of 10 runs, on the reference machine.
 *
 * Drives [QQWing] directly, not [QQWingController]: the controller's workers
 * call `android.os.Process`, which throws on a JVM, so the threaded path is
 * verified on-device instead — it is the app's normal generation, exercised on
 * every launch and every difficulty change. The sequence here (generate, count,
 * solve, grade, repeat until the grade matches) is exactly one worker's loop,
 * which the controller parallelises across up to four threads. Seeded, so the
 * work is identical every run; what varies is the machine.
 */
class GenerationBudgetTest {
    @Test
    fun moderateSlowestOfTenWithinTwoSeconds() {
        val elapsed = (0 until 10).map { run ->
            val qq = QQWing(GameType.Default9x9, GameDifficulty.Moderate)
            qq.setRecordHistory(true)
            qq.setRandom(20260903 + run)
            val t0 = System.nanoTime()
            var attempts = 0
            do {
                attempts++
                qq.generatePuzzleSymmetry(Symmetry.NONE)
                qq.countSolutionsLimited()
                qq.solve()
            } while (qq.getDifficulty() != GameDifficulty.Moderate && attempts < MAX_ATTEMPTS)
            val elapsedMs = (System.nanoTime() - t0) / 1_000_000
            println("run=$run attempts=$attempts elapsed=${elapsedMs}ms")
            assertEquals("run $run never graded Moderate", GameDifficulty.Moderate, qq.getDifficulty())
            elapsedMs
        }
        val slowest = elapsed.max()
        println("slowestOf10=${slowest}ms")
        assertTrue("slowest Moderate generation ${slowest}ms exceeds the 2000ms budget", slowest < 2_000)
    }

    companion object {
        private const val MAX_ATTEMPTS = 200
    }
}
