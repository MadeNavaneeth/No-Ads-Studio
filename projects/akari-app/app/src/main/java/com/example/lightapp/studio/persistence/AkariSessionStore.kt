package com.example.lightapp.studio.persistence

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * akari(10) persistence — wraps the shell's game-agnostic [SessionStore] with the
 * akari-specific encode/decode.
 *
 * The shell never learns what an akari run looks like: it stores opaque string
 * fields per game and hands them back. This class owns the field names and the
 * cells/walls encoding, and exposes akari-typed accessors to the game's
 * ViewModel and MainActivity.
 */
class AkariSessionStore(context: Context) {

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
    private val gameId = "akari"

    private fun decode(fields: Map<String, String>): AkariSession? {
        val cells = fields["cells"] ?: return null
        val walls = fields["walls"] ?: return null
        // AkariSession's init throws on malformed strings, so the decodes gate first.
        AkariCodec.decodeCells(cells) ?: return null
        AkariCodec.decodeWalls(walls) ?: return null
        return AkariSession(
            cells = cells,
            walls = walls,
            difficulty = fields["difficulty"] ?: "Moderate",
            elapsedMs = fields["elapsedMs"]?.toLongOrNull() ?: 0L,
            mistakes = fields["mistakes"]?.toIntOrNull() ?: 0,
            progress = fields["progress"]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
        )
    }

    private fun encode(session: AkariSession): Map<String, String> = mapOf(
        "cells" to session.cells,
        "walls" to session.walls,
        "difficulty" to session.difficulty,
        "elapsedMs" to session.elapsedMs.toString(),
        "mistakes" to session.mistakes.toString(),
        "progress" to session.progress.toString(),
    )

    /** The akari run, or null when none is stored. */
    fun session(): Flow<AkariSession?> = store.sessionFields(gameId).map { fields ->
        fields?.let { decode(it) }
    }

    suspend fun saveSession(session: AkariSession) {
        store.storeSession(gameId, encode(session))
    }

    suspend fun clearSession() = store.clearSession(gameId)

    /** All resume-card snapshots — keyed by game id. For akari, just this one. */
    val allSummaries: Flow<Map<String, GameSessionSnapshot>> = store.allSummaries

    // ── daily puzzle ───────────────────────────────────────────────────────────
    /**
     * Today's daily, or null when none is stored for [day].
     * The shell returns the stored fields only when the stored day equals [day],
     * so a stale daily decodes to null — earlier day is leftovers, not progress.
     */
    fun daily(day: Long): Flow<AkariSession?> = store.dailyFields(gameId, day).map { fields ->
        fields?.let { decode(it) }
    }

    suspend fun saveDaily(day: Long, session: AkariSession) {
        store.storeDaily(gameId, day, encode(session))
    }

    suspend fun clearDaily() = store.clearDaily(gameId)

    val activeDailies: Flow<Set<String>> = store.activeDailies
}