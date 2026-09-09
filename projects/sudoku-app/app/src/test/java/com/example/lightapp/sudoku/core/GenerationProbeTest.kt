package com.example.lightapp.sudoku.core

import org.junit.Test

/**
 * Scratch probe — not a real test. Times the exact phases a worker runs when
 * generating at a requested difficulty (generate → grade → rejection loop), to tell
 * whether a slow phone run is an algorithm regression or pure environment.
 */
class GenerationProbeTest {
    @Test
    fun probe() {
        repeat(6) { r ->
            val seed = 20260903 + r
            val qq = QQWing(GameType.Default9x9, GameDifficulty.Moderate)
            qq.setRecordHistory(true)
            qq.setRandom(seed)
            var attempts = 0
            val t0 = System.nanoTime()
            var matched = false
            while (System.nanoTime() - t0 < 20_000_000_000L) {
                attempts++
                var t = System.nanoTime()
                qq.generatePuzzleSymmetry(Symmetry.NONE)
                val genMs = (System.nanoTime() - t) / 1_000_000
                t = System.nanoTime()
                qq.countSolutionsLimited()
                val countMs = (System.nanoTime() - t) / 1_000_000
                t = System.nanoTime()
                qq.solve()
                val solveMs = (System.nanoTime() - t) / 1_000_000
                val diff = qq.getDifficulty()
                val elapsed = (System.nanoTime() - t0) / 1_000_000
                println("seed=$seed attempt=$attempts gen=${genMs}ms count=${countMs}ms solve=${solveMs}ms diff=$diff elapsed=${elapsed}ms")
                if (diff == GameDifficulty.Moderate) { matched = true; break }
            }
            println("seed=$seed matchedModerate=$matched")
        }
    }
}
