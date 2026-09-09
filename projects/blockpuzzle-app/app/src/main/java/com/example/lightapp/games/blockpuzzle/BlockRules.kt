package com.example.lightapp.games.blockpuzzle

/**
 * Rules for `blockpuzzle(8)`. Pure functions — no Android, no Compose — so every one
 * of these is unit-testable on the JVM.
 *
 * The first endless game in the studio: there is no generated puzzle and no solution,
 * so the admission criterion "solvable by deduction from the start state" is carried,
 * per decision D33, by the generator's **placeable-deal contract** instead — every
 * dealt set admits at least one legal placement at deal time, so a run can only end
 * by accumulated placement, never by an instantly-dead deal.
 *
 * Cell values are bi-state: [EMPTY] ground, [FILLED] block. There is no red anywhere
 * in this game — nothing can be wrong. A piece that does not fit simply refuses to
 * land (method §6: a mistake is found by contradiction, not announced).
 *
 * **Motion, decision D33:** a piece follows the pointer only during an active drag —
 * direct manipulation is input, not animation. On release the piece either lands at
 * its cells by an opacity fade or snaps back to the tray by one; nothing ever
 * *travels* after input ends, which is what S13 has always forbidden.
 */
object BlockRules {

    const val SIZE = 8
    const val CELLS = SIZE * SIZE

    const val EMPTY = 0
    const val FILLED = 1

    fun rowOf(index: Int) = index / SIZE
    fun colOf(index: Int) = index % SIZE

    // ─── pieces ───────────────────────────────────────────────────────────────

    /**
     * One tray piece: a shape from the catalog (family + orientation), rendered at a
     * **dot density**. Density is the monochrome answer to "three pieces at once"
     * (decision D33): solid, half-pitch dots, hollow outline — a data-bearing
     * distinction that survives grayscale by construction, never a colour trick.
     */
    data class Piece(
        /** The catalog family, e.g. `O` or `L4`. */
        val family: String,
        /** Orientation index within the family's variants. */
        val variant: Int,
        /** 0 solid, 1 half-pitch, 2 hollow. Tray pieces are dealt distinct where possible. */
        val density: Int,
    ) {
        val grid: List<List<Int>> get() = CATALOG.getValue(family)[variant]

        /** Cell count — the score a placement earns. */
        val size: Int get() = grid.sumOf { row -> row.count { it == 1 } }
    }

    /** Dot densities, dealt round-robin so concurrent tray pieces usually differ. */
    val DENSITIES = listOf(0, 1, 2)

    /**
     * The piece catalog: every family with its orientations, each orientation a small
     * row-major grid of 0/1. Single-square families (`I1`) hold one variant; `L4`
     * holds four rotations. Families are keyed by name so a persisted piece is three
     * short fields, never geometry — the catalog is the geometry.
     */
    val CATALOG: Map<String, List<List<List<Int>>>> = mapOf(
        "I1" to listOf(listOf(listOf(1))),
        "I2" to listOf(
            listOf(listOf(1, 1)),
            listOf(listOf(1), listOf(1)),
        ),
        "I3" to listOf(
            listOf(listOf(1, 1, 1)),
            listOf(listOf(1), listOf(1), listOf(1)),
        ),
        "L3" to listOf(
            listOf(listOf(1, 0), listOf(1, 1)),
            listOf(listOf(1, 1), listOf(1, 0)),
            listOf(listOf(1, 1), listOf(0, 1)),
            listOf(listOf(0, 1), listOf(1, 1)),
        ),
        "O" to listOf(listOf(listOf(1, 1), listOf(1, 1))),
        "I4" to listOf(
            listOf(listOf(1, 1, 1, 1)),
            listOf(listOf(1), listOf(1), listOf(1), listOf(1)),
        ),
        "L4" to listOf(
            listOf(listOf(1, 0), listOf(1, 0), listOf(1, 1)),
            listOf(listOf(1, 1, 1), listOf(1, 0, 0)),
            listOf(listOf(1, 1), listOf(0, 1), listOf(0, 1)),
            listOf(listOf(0, 0, 1), listOf(1, 1, 1)),
        ),
        "T4" to listOf(
            listOf(listOf(1, 1, 1), listOf(0, 1, 0)),
            listOf(listOf(0, 1), listOf(1, 1), listOf(0, 1)),
            listOf(listOf(0, 1, 0), listOf(1, 1, 1)),
            listOf(listOf(1, 0), listOf(1, 1), listOf(1, 0)),
        ),
        "S" to listOf(
            listOf(listOf(0, 1, 1), listOf(1, 1, 0)),
            listOf(listOf(1, 0), listOf(1, 1), listOf(0, 1)),
        ),
        "Z" to listOf(
            listOf(listOf(1, 1, 0), listOf(0, 1, 1)),
            listOf(listOf(0, 1), listOf(1, 1), listOf(1, 0)),
        ),
    )

    /** Every (family, variant) pair flattened — the deal's candidate space. */
    val ALL_SHAPES: List<Pair<String, Int>> =
        CATALOG.flatMap { (family, variants) -> variants.indices.map { family to it } }

    // ─── placement ────────────────────────────────────────────────────────────

    /**
     * Whether [piece] fits on [cells] with its top-left corner at ([row], [col]).
     * Pure geometry — the caller decides what to do with the answer.
     */
    fun canPlace(cells: List<Int>, piece: Piece, row: Int, col: Int): Boolean {
        val grid = piece.grid
        for (r in grid.indices) {
            for (c in grid[r].indices) {
                if (grid[r][c] == 0) continue
                val br = row + r
                val bc = col + c
                if (br !in 0 until SIZE || bc !in 0 until SIZE) return false
                if (cells[br * SIZE + bc] != EMPTY) return false
            }
        }
        return true
    }

    /**
     * [cells] with [piece] filled at ([row], [col]). The caller must have checked
     * [canPlace]; this is the commit, not the gate.
     */
    fun placed(cells: List<Int>, piece: Piece, row: Int, col: Int): List<Int> {
        val out = cells.toMutableList()
        val grid = piece.grid
        for (r in grid.indices) {
            for (c in grid[r].indices) {
                if (grid[r][c] == 1) out[(row + r) * SIZE + (col + c)] = FILLED
            }
        }
        return out
    }

    /** Every legal top-left anchor for [piece] on [cells], or an empty list. */
    fun placements(cells: List<Int>, piece: Piece): List<Pair<Int, Int>> {
        val grid = piece.grid
        val maxRow = SIZE - grid.size
        val maxCol = SIZE - (grid.firstOrNull()?.size ?: 0)
        val out = ArrayList<Pair<Int, Int>>()
        for (r in 0..maxRow) {
            for (c in 0..maxCol) {
                if (canPlace(cells, piece, r, c)) out += r to c
            }
        }
        return out
    }

    /** Whether any of [pieces] fits somewhere — the game-over check after a refill. */
    fun anyPlacement(cells: List<Int>, pieces: List<Piece>): Boolean =
        pieces.any { piece -> placements(cells, piece).isNotEmpty() }

    // ─── clearing ─────────────────────────────────────────────────────────────

    /**
     * Full rows and columns clear together, the game's one satisfying moment. The
     * board after a clear is *only* the remaining blocks — a partial line survives,
     * and a cell at the crossing of a full row and a full column clears once.
     *
     * Returns the new board and how many lines cleared, so the score can be computed
     * from the same read.
     */
    fun clearFullLines(cells: List<Int>): Pair<List<Int>, Int> {
        val fullRows = (0 until SIZE).filter { r -> (0 until SIZE).all { c -> cells[r * SIZE + c] == FILLED } }
        val fullCols = (0 until SIZE).filter { c -> (0 until SIZE).all { r -> cells[r * SIZE + c] == FILLED } }
        if (fullRows.isEmpty() && fullCols.isEmpty()) return cells to 0

        val out = cells.toMutableList()
        for (r in fullRows) for (c in 0 until SIZE) out[r * SIZE + c] = EMPTY
        for (c in fullCols) for (r in 0 until SIZE) out[r * SIZE + c] = EMPTY
        return out to (fullRows.size + fullCols.size)
    }

    /** The one quantity (method §2): filled ground over all cells. */
    fun completion(cells: List<Int>): Float =
        cells.count { it == FILLED }.toFloat() / CELLS

    /** Scoring: a cell is a point, a cleared line is worth ten — the tray's economy. */
    fun scoreFor(placedSize: Int, linesCleared: Int): Int =
        placedSize + linesCleared * 10
}
