package com.example.lightapp.studio.persistence

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NonogramCodecTest {

    private fun session() = NonogramSession(
        solution = List(NonogramSession.CELLS) { if (it % 3 == 0) 1 else 0 },
        cells = List(NonogramSession.CELLS) { it % 3 },
        elapsedMs = 61_000L,
        difficulty = "Moderate",
        mistakes = 1,
        progress = 0.25f,
    )

    @Test
    fun bitmapRoundTrips() {
        val bits = List(NonogramSession.CELLS) { if (it % 2 == 0) 1 else 0 }
        assertEquals(bits, NonogramCodec.decodeBitmap(NonogramCodec.encodeBitmap(bits)))
    }

    @Test
    fun cellsRoundTrip() {
        val cells = List(NonogramSession.CELLS) { it % 3 }
        assertEquals(cells, NonogramCodec.decodeCells(NonogramCodec.encodeCells(cells)))
    }

    @Test
    fun shortBitmapIsRejected() {
        assertNull(NonogramCodec.decodeBitmap("0101"))
    }

    @Test
    fun badCharactersAreRejected() {
        assertNull(NonogramCodec.decodeBitmap("2".repeat(NonogramSession.CELLS)))
        assertNull(NonogramCodec.decodeCells("3".repeat(NonogramSession.CELLS)))
        assertNull(NonogramCodec.decodeBitmap(null))
        assertNull(NonogramCodec.decodeCells(""))
    }

    @Test
    fun sessionFieldsSurviveTheStore() {
        val s = session()
        // The store splits fields across keys; each half must round-trip alone.
        assertEquals(s.solution, NonogramCodec.decodeBitmap(NonogramCodec.encodeBitmap(s.solution)))
        assertEquals(s.cells, NonogramCodec.decodeCells(NonogramCodec.encodeCells(s.cells)))
    }
}
