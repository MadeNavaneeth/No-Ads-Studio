package com.example.lightapp.studio.persistence

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * sudoku(9) persistence — wraps the shell's game-agnostic [SessionStore] with the
 * sudoku-specific encode/decode.
 *
 * The shell never learns what a sudoku session looks like: it stores opaque string
 * fields per game and hands them back. This class owns the field names and the grid
 * encoding, and exposes sudoku-typed accessors to the game's ViewModel and MainActivity.
 */
class SudokuSessionStore(context: Context) {

    private val store = SessionStore(context)

    // ── settings & stats — delegate to the shell ───────────────────────────────
    val themeMode: Flow<String?> = store.themeMode
    val hapticsEnabled: Flow<Boolean> = store.hapticsEnabled
    val showRemaining: Flow<Boolean> = store.showRemaining
    val showTimer: Flow<Boolean> = store.showTimer
    val showPeers: Flow<Boolean> = store.showPeers
    val mistakeLimit: Flow<Int> = store.mistakeLimit
    val accentChoice: Flow<String> = store.accentChoice
    val autoCleanNotes: Flow<Boolean> = store.autoCleanNotes

    suspend fun setThemeMode(mode: String) = store.setThemeMode(mode)
    suspend fun setHapticsEnabled(enabled: Boolean) = store.setHapticsEnabled(enabled)
    suspend fun setShowRemaining(show: Boolean) = store.setShowRemaining(show)
    suspend fun setShowTimer(show: Boolean) = store.setShowTimer(show)
    suspend fun setShowPeers(show: Boolean) = store.setShowPeers(show)
    suspend fun setMistakeLimit(limit: Int) = store.setMistakeLimit(limit)
    suspend fun setAccentChoice(choice: String) = store.setAccentChoice(choice)
    suspend fun setAutoCleanNotes(enabled: Boolean) = store.setAutoCleanNotes(enabled)

    fun stats(gameId: String): Flow<GameStats> = store.stats(gameId)
    val allStats: Flow<Map<String, GameStats>> = store.allStats
    suspend fun recordResult(
        gameId: String,
        difficulty: String,
        won: Boolean,
        durationMs: Long,
    ) = store.recordResult(gameId, difficulty, won, durationMs)
    suspend fun clearAllStats() = store.clearAllStats()

    // ── game sessions ──────────────────────────────────────────────────────────
    private val gameId = "sudoku"

    /** The sudoku session, or null when none is stored. */
    fun session(gameId: String): Flow<SudokuSession?> = store.sessionFields(gameId).map { fields ->
        fields?.let { SudokuCodec.decodeSession(it) }
    }

    /** Saves a sudoku session, encoding it to opaque string fields. */
    suspend fun saveSession(gameId: String, session: SudokuSession) {
        store.storeSession(gameId, SudokuCodec.encodeSession(session))
    }

    suspend fun clearSession(gameId: String) = store.clearSession(gameId)

    /** All resume-card snapshots — keyed by game id. For sudoku, just this one. */
    val allSummaries: Flow<Map<String, GameSessionSnapshot>> = store.allSummaries

    // —— daily puzzle —————————————————————————————————————————————————————
    /** The UTC day number for right now. */
    fun today(): Long = LocalDate.now(ZoneOffset.UTC).toEpochDay()

    /**
     * Today's daily puzzle, or null when none is stored for today.
     * The shell returns the stored fields only when the stored day equals [today()],
     * so a stale daily decodes to null — and the decoded puzzle's day is today by
     * construction.
     */
    fun dailyPuzzle(): Flow<DailyPuzzle?> = store.dailyFields(gameId, today()).map { fields ->
        fields?.let { SudokuCodec.decodeDaily(it, today()) }
    }

    val activeDailies: Flow<Set<String>> = store.activeDailies

    suspend fun saveDailyPuzzle(day: Long, session: SudokuSession) {
        store.storeDaily(gameId, day, SudokuCodec.encodeSession(session))
    }

    suspend fun clearDailyPuzzle() = store.clearDaily(gameId)
}


