package com.example.lightapp.studio.persistence

/**
 * What a game records about one difficulty.
 *
 * [bestMs] is `null` until something is actually won, which is the only honest value for
 * "best time" before there is a time. Zero would sort as an unbeatable record.
 */
data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val bestMs: Long? = null,
    val sumMs: Long = 0L,
) {
    val hasRecord: Boolean get() = bestMs != null
    val avgMs: Long? get() = if (won > 0) sumMs / won else null
}

/**
 * Every difficulty a game has been played at, keyed by difficulty name.
 *
 * Difficulty is a plain `String` rather than an enum because difficulty names belong to the
 * individual game — persistence is shared by all of them and must not know that `sudoku(9)`
 * calls one of its levels `Challenge`.
 */
data class GameStats(
    val perDifficulty: Map<String, DifficultyStats> = emptyMap(),
) {
    val played: Int get() = perDifficulty.values.sumOf { it.played }
    val won: Int get() = perDifficulty.values.sumOf { it.won }

    /** The fastest win across every difficulty, or `null` if there has not been one. */
    val bestMs: Long? get() = perDifficulty.values.mapNotNull { it.bestMs }.minOrNull()

    /** Records a finished game and returns the updated stats. Pure — no I/O. */
    fun recording(difficulty: String, won: Boolean, durationMs: Long): GameStats {
        val current = perDifficulty[difficulty] ?: DifficultyStats()
        val updated = DifficultyStats(
            played = current.played + 1,
            won = current.won + if (won) 1 else 0,
            bestMs = when {
                !won -> current.bestMs
                // A duration of zero means the clock never ran, which is a bug elsewhere
                // rather than a world record. Refuse to enshrine it.
                durationMs <= 0L -> current.bestMs
                current.bestMs == null -> durationMs
                else -> minOf(current.bestMs, durationMs)
            },
            sumMs = if (won && durationMs > 0L) current.sumMs + durationMs else current.sumMs,
        )
        return GameStats(perDifficulty + (difficulty to updated))
    }

    /** True when [durationMs] would beat the stored record for [difficulty]. */
    fun isPersonalBest(difficulty: String, durationMs: Long): Boolean {
        if (durationMs <= 0L) return false
        val best = perDifficulty[difficulty]?.bestMs ?: return true
        return durationMs < best
    }
}

/**
 * Encoding for [GameStats] — the same reasoning as `SudokuCodec`, decision D7.
 *
 * One compact string per game rather than a row per difficulty, so adding a difficulty does
 * not add a preference key. Format, one record per difficulty separated by `;`:
 *
 * ```
 * Moderate:12:9:243000:2910000;Hard:3:1:501000:1503000
 * difficulty:played:won:bestMs:sumMs  bestMs -1 = no record, sumMs 0 = no avg
 * ```
 *
 * Every decode returns a usable value rather than throwing. Stats are not worth crashing
 * over: a record the app cannot read means "no record", which is exactly what a new install
 * shows anyway. Difficulty names containing `:` or `;` are rejected on encode rather than
 * silently corrupting the row.
 */
object StatsCodec {

    fun encode(stats: GameStats): String =
        stats.perDifficulty.entries
            .filter { (name, _) -> name.isNotEmpty() && name.none { it == ':' || it == ';' } }
            .sortedBy { it.key }
            .joinToString(";") { (name, s) ->
                "$name:${s.played}:${s.won}:${s.bestMs ?: NO_RECORD}:${s.sumMs}"
            }

    fun decode(encoded: String?): GameStats {
        if (encoded.isNullOrBlank()) return GameStats()
        val out = mutableMapOf<String, DifficultyStats>()
        for (row in encoded.split(';')) {
            if (row.isBlank()) continue
            val parts = row.split(':')
            // 4-field rows are legacy (pre-sumMs) — treat sum as 0 so old installs migrate.
            if (parts.size != FIELDS && parts.size != FIELDS_LEGACY) continue
            val name = parts[0]
            val played = parts[1].toIntOrNull() ?: continue
            val won = parts[2].toIntOrNull() ?: continue
            val best = parts[3].toLongOrNull() ?: continue
            val sum = if (parts.size == FIELDS) parts[4].toLongOrNull() ?: continue else 0L
            if (name.isEmpty() || played < 0 || won < 0 || won > played || sum < 0L) continue
            out[name] = DifficultyStats(
                played = played,
                won = won,
                bestMs = best.takeIf { it > 0L },
                sumMs = sum,
            )
        }
        return GameStats(out)
    }

    private const val NO_RECORD = -1L
    private const val FIELDS = 5
    private const val FIELDS_LEGACY = 4
}
