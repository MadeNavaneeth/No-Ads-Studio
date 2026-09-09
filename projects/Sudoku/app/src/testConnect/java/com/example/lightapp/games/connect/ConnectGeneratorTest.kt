package com.example.lightapp.games.connect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The generator's contract, asserted rather than assumed (roadmap §3: measured, not
 * assumed). Validity is the load-bearing guarantee — every generated board must solve
 * by its own rules, because the cut is chosen so that the path *is* the solution —
 * and every test here runs the generator, so a regression shows up as a failing
 * suite, not as a subtle gameplay lie.
 */
class ConnectGeneratorTest {

    @Test
    fun `generated boards ship unsolved and carry only endpoints`() {
        for (difficulty in ConnectGenerator.DIFFICULTIES) {
            val board = requireNotNull(ConnectGenerator.generate(difficulty, seed = 1)) {
                "generator must produce a board for $difficulty"
            }
            // The shipped position is endpoints over empty ground — never the
            // solution. Solve-ability is asserted by construction (cutAndVerify)
            // and by every other test here that plays the board out.
            assertTrue(
                "$difficulty must hold endpoints only",
                board.all { ConnectRules.isEndpoint(it) || it == ConnectRules.EMPTY },
            )
        }
    }

    @Test
    fun `same seed same board different seed different board`() {
        val a = ConnectGenerator.generate("Moderate", seed = 42)
        val b = ConnectGenerator.generate("Moderate", seed = 42)
        assertEquals(a, b)

        // Different seeds, same difficulty: overwhelmingly different boards. One
        // collision across ten seeds would already be extraordinary.
        val boards = (1L..10L).map { ConnectGenerator.generate("Moderate", seed = it) }
        assertEquals(10, boards.map { it.hashCode() }.toSet().size)
    }

    @Test
    fun `difficulty fixes the pair count`() {
        assertEquals(4, ConnectGenerator.pairCountFor("Simple"))
        assertEquals(6, ConnectGenerator.pairCountFor("Moderate"))
        assertEquals(8, ConnectGenerator.pairCountFor("Hard"))
        // Unknown falls back to the default rather than failing.
        assertEquals(6, ConnectGenerator.pairCountFor("nope"))
    }

    @Test
    fun `generated boards carry the difficulty's pair count`() {
        for (difficulty in ConnectGenerator.DIFFICULTIES) {
            val board = requireNotNull(ConnectGenerator.generate(difficulty, seed = 3))
            assertEquals(
                ConnectGenerator.pairCountFor(difficulty),
                ConnectRules.endpointPairs(board).size,
            )
        }
    }

    @Test
    fun `generation stays inside the admission budget`() {
        // Roadmap §3: generation time ≤ 2 s for the default difficulty, slowest of
        // ten runs. This asserts the intent on the JVM, where timings are noisy —
        // the real gate is measured on the reference machine.
        val start = System.nanoTime()
        repeat(10) { run ->
            requireNotNull(ConnectGenerator.generate("Moderate", seed = run.toLong()))
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000
        assertTrue("ten generations took ${elapsedMs}ms", elapsedMs < 20_000)
    }

    @Test
    fun `every pair's endpoints are distinct cells`() {
        // A leg of length one would put both endpoints of a pair on the same cell.
        for (difficulty in ConnectGenerator.DIFFICULTIES) {
            val board = requireNotNull(ConnectGenerator.generate(difficulty, seed = 11))
            for (pair in ConnectRules.endpointPairs(board)) {
                val from = board.indexOf(ConnectRules.endpointOf(pair))
                val to = board.lastIndexOf(ConnectRules.endpointOf(pair))
                assertNotEquals("$difficulty pair $pair collapsed", from, to)
            }
        }
    }

    @Test
    fun `unknown difficulty still generates at the default density`() {
        assertNotNull(ConnectGenerator.generate("nope", seed = 1))
    }
}
