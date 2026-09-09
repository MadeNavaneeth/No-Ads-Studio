package com.example.lightapp.studio.persistence

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * On-device persistence — decision D7.
 *
 * DataStore Preferences, not Room. One store, three key prefixes: `session.<gameId>.`
 * for game saves, `daily.<gameId>.` for the per-day puzzles, and `stats.` + `settings.`
 * for everything else.
 *
 * This is the **game-agnostic** half. It stores, per game, a map of opaque encoded
 * *strings* — the game's own codec produces and consumes them. The shell never learns
 * what a Sudoku session looks like: the game's ViewModel encodes its state to fields,
 * and the shell hands the same fields back to its restore step. The resume card needs
 * only the five plain values in [GameSessionSnapshot], which every game supplies.
 *
 * Nothing leaves the device. This is the whole persistence layer.
 *
 * ## Failure is not optional here
 *
 * The store is read during composition in `MainActivity` and in each game view model's
 * `init`. Neither can catch an exception. [ReplaceFileCorruptionHandler] handles an
 * unparseable file; [safeData] handles an `IOException` on read. A save the app cannot
 * read means "no saved game", never a crash.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nothing_games",
    corruptionHandler = ReplaceFileCorruptionHandler {
        Log.w("SessionStore", "preferences file was unreadable, starting empty", it)
        emptyPreferences()
    },
)

class SessionStore(private val context: Context) {

    /**
     * The preferences stream, with read failures degraded to defaults.
     *
     * Every read below goes through this rather than `dataStore.data` directly. Only
     * `IOException` is swallowed: anything else is a bug in this file and should surface
     * loudly rather than be hidden behind an empty store.
     */
    private val safeData: Flow<Preferences> = context.dataStore.data
        .catch { cause ->
            if (cause is IOException) {
                Log.w(TAG, "could not read preferences, using defaults", cause)
                emit(emptyPreferences())
            } else {
                throw cause
            }
        }

    // ─── settings ─────────────────────────────────────────────────────────────
    private object Settings {
        val themeMode = stringPreferencesKey("settings.themeMode")
        val hapticsEnabled = booleanPreferencesKey("settings.hapticsEnabled")
        val showRemaining = booleanPreferencesKey("settings.showRemaining")
        val showTimer = booleanPreferencesKey("settings.showTimer")
        val showPeers = booleanPreferencesKey("settings.showPeers")
        val mistakeLimit = intPreferencesKey("settings.mistakeLimit")
        val accentChoice = stringPreferencesKey("settings.accentChoice")
    val autoCleanNotes = booleanPreferencesKey("settings.autoCleanNotes")
    }

    val themeMode: Flow<String?> = safeData.map { it[Settings.themeMode] }
    val hapticsEnabled: Flow<Boolean> = safeData.map { it[Settings.hapticsEnabled] ?: true }

    /**
     * Whether the number pad shows how many of each digit are left.
     * Defaults on: the count was always computable by the player, so showing it exposes the
     * mechanism rather than doing the puzzle for them.
     */
    val showRemaining: Flow<Boolean> = safeData.map { it[Settings.showRemaining] ?: true }

    /** Whether the elapsed-time readout is visible. The clock runs either way. */
    val showTimer: Flow<Boolean> = safeData.map { it[Settings.showTimer] ?: true }

    /** Whether selecting a cell highlights its peers / same-digit set (D30). Defaults on. */
    val showPeers: Flow<Boolean> = safeData.map { it[Settings.showPeers] ?: true }

    /** Max mistakes before the run is failed. 0 = unlimited. Defaults 0. */
    val mistakeLimit: Flow<Int> = safeData.map { it[Settings.mistakeLimit] ?: 0 }

    /** Third accent choice — TE sage/amber or none. Defaults none (red is still error-only). */
    val accentChoice: Flow<String> = safeData.map { it[Settings.accentChoice] ?: "none" }

    /**
     * Whether placing a digit sweeps that digit from its peers' pencil marks.
     * Defaults on: the sweep removes only marks the placed digit has *proven* wrong,
     * so it is bookkeeping the board already knows. Undo reverses it.
     */
    val autoCleanNotes: Flow<Boolean> = safeData.map { it[Settings.autoCleanNotes] ?: true }

    suspend fun setThemeMode(mode: String) = edit { it[Settings.themeMode] = mode }

    suspend fun setHapticsEnabled(enabled: Boolean) =
        edit { it[Settings.hapticsEnabled] = enabled }

    suspend fun setShowRemaining(show: Boolean) = edit { it[Settings.showRemaining] = show }

    suspend fun setShowTimer(show: Boolean) = edit { it[Settings.showTimer] = show }

    suspend fun setShowPeers(show: Boolean) = edit { it[Settings.showPeers] = show }

    suspend fun setMistakeLimit(limit: Int) = edit { it[Settings.mistakeLimit] = limit }

    suspend fun setAccentChoice(choice: String) = edit { it[Settings.accentChoice] = choice }

    suspend fun setAutoCleanNotes(enabled: Boolean) = edit { it[Settings.autoCleanNotes] = enabled }
// ─── statistics ───────────────────────────────────────────────────────────
    //
    // Keyed by game id, so this stays game-agnostic: it records what a game reports through
    // `GameResult` and never learns what the game is.

    private fun statsKey(gameId: String) = stringPreferencesKey("$STATS_PREFIX$gameId")

    /** Stats for [gameId]. Empty for a game that has never been finished. */
    fun stats(gameId: String): Flow<GameStats> =
        safeData.map { StatsCodec.decode(it[statsKey(gameId)]) }

    /**
     * Every game's stats, keyed by game id. Derived by scanning for the key prefix
     * rather than by asking the registry, so a standalone app still reads the record
     * the studio build wrote.
     */
    val allStats: Flow<Map<String, GameStats>> = safeData.map { prefs ->
        prefs.asMap()
            .mapNotNull { (key, value) ->
                if (!key.name.startsWith(STATS_PREFIX) || value !is String) return@mapNotNull null
                val gameId = key.name.removePrefix(STATS_PREFIX)
                if (gameId.isEmpty()) null else gameId to StatsCodec.decode(value)
            }
            .toMap()

}
    /**
     * Folds a finished game into the stored stats. Read-modify-write inside one `edit`
     * block, which DataStore serialises, so two games finishing at once cannot lose a record.
     */
    suspend fun recordResult(
        gameId: String,
        difficulty: String,
        won: Boolean,
        durationMs: Long,
    ) = edit { prefs ->
        val key = statsKey(gameId)
        val updated = StatsCodec.decode(prefs[key])
            .recording(difficulty = difficulty, won = won, durationMs = durationMs)
        prefs[key] = StatsCodec.encode(updated)

}
    suspend fun clearAllStats() = edit { prefs ->
        prefs.asMap().keys
            .filter { it.name.startsWith(STATS_PREFIX) }
            .forEach { prefs.remove(it) }

}
    // ─── game sessions — one key prefix per game, opaque fields ───────────────

    private fun sessionFieldKey(gameId: String, field: String) =
        stringPreferencesKey("$SESSION_PREFIX$gameId.$field")

    /**
     * The raw encoded state of the most recent incomplete [gameId] session's fields,
     * or `null` when there is none or it is unreadable. The game's own codec turns
     * these back into a session.
     */
    fun sessionFields(gameId: String): Flow<Map<String, String>?> = safeData.map { prefs ->
        fieldsOf(prefs, gameId, SESSION_PREFIX)

}
    /** Saves the raw encoded [fields] for [gameId] — the game's codec produced them. */
    suspend fun storeSession(gameId: String, fields: Map<String, String>) = edit { prefs ->
        fields.forEach { (field, value) -> prefs[sessionFieldKey(gameId, field)] = value }

}
    /** Removes every stored field for [gameId] (finished puzzle / new game). */
    suspend fun clearSession(gameId: String) = edit { prefs ->
        prefs.asMap().keys
            .filter { it.name.startsWith("$SESSION_PREFIX$gameId.") }
            .forEach { prefs.remove(it) }

}
    /**
     * The plain resume-card values for [gameId], or `null` when no session exists.
     * Deliberately separate from the raw fields: the shell renders the resume card
     * from [GameSessionSnapshot] alone, so it never needs the game's codec.
     */
    fun sessionSummary(gameId: String): Flow<GameSessionSnapshot?> = safeData.map { prefs ->
        fieldsOf(prefs, gameId, SESSION_PREFIX)?.let { fields -> snapshotOf(gameId, fields) }

}
    /**
     * Every incomplete session's resume-card values, keyed by game id.
     * Scans for `session.` rather than asking the registry, so a stale install that
     * still holds a save for a removed game keeps the resume card.
     */
    val allSummaries: Flow<Map<String, GameSessionSnapshot>> = safeData.map { prefs ->
        prefs.asMap().keys
            .mapNotNull { key ->
                val name = key.name
                if (!name.startsWith(SESSION_PREFIX)) return@mapNotNull null
                name.removePrefix(SESSION_PREFIX).substringBefore('.').takeIf { it.isNotEmpty() }
            }
            .distinct()
            .mapNotNull { gameId ->
                fieldsOf(prefs, gameId, SESSION_PREFIX)?.let { fields ->
                    snapshotOf(gameId, fields)?.let { gameId to it }
                }
            }
            .toMap()
// ─── the daily puzzle slot ──────────────────────────────────────────────
}
    //
    // One-per-day local puzzle per game, keeping its own keys so that starting,
    // playing, or finishing the day's puzzle never touches the regular in-progress
    // session, and vice versa. The prefix is `daily.<gameId>.`, NEVER `session.` —
    // `allSummaries` scans for `session.` to build the resume cards, and a daily is
    // reached through the game's own NEW picker, not a resume card. The day is stored
    // alongside the session so the game can tell "today's puzzle" from yesterday's
    // leftovers without decoding the fields.

    private fun dailyDayKey(gameId: String) = longPreferencesKey("daily.$gameId.day")
    private fun dailyFieldKey(gameId: String, field: String) =
        stringPreferencesKey("daily.$gameId.$field")

    /** The raw fields of the stored daily for [gameId] on [day], or null. */
    fun dailyFields(gameId: String, day: Long): Flow<Map<String, String>?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(gameId)] != day) return@map null
        fieldsOf(prefs, gameId, DAILY_PREFIX)

}
    /** Saves the raw daily [fields] for [gameId] under [day]. */
    suspend fun storeDaily(gameId: String, day: Long, fields: Map<String, String>) = edit { prefs ->
        prefs[dailyDayKey(gameId)] = day
        fields.forEach { (field, value) -> prefs[dailyFieldKey(gameId, field)] = value }

}
    /** Removes the stored daily for [gameId]. Called when it is finished or stale. */
    suspend fun clearDaily(gameId: String) = edit { prefs ->
        prefs.asMap().keys
            .filter { it.name.startsWith("$DAILY_PREFIX$gameId.") }
            .forEach { prefs.remove(it) }

}
    /**
     * Which games currently hold an unfinished daily, for the home screen's per-game
     * TODAY badge. Key presence **is** the state: `daily.<id>.day` existing means
     * exactly "that game's daily is in progress".
     */
    val activeDailies: Flow<Set<String>> = safeData.map { prefs ->
        prefs.asMap().keys.mapNotNull { key ->
            val name = key.name
            when {
                name.startsWith(DAILY_PREFIX) -> {
                    name.removePrefix(DAILY_PREFIX).substringBefore('.').takeIf { it.isNotEmpty() }
                }
                else -> null
            }
        }.toSet()

    // ─── shared field helpers ────────────────────────────────────────────────
}

    /** Reads every stored string field for [gameId] under [prefix]. */
    private fun fieldsOf(prefs: Preferences, gameId: String, prefix: String): Map<String, String>? =
        prefs.asMap()
            .mapNotNull { (key, value) ->
                val name = key.name
                if (!name.startsWith(prefix) || !name.removePrefix(prefix).startsWith("$gameId.")) {
                    return@mapNotNull null
                }
                val field = name.removePrefix(prefix).removePrefix("$gameId.")
                if (field.isEmpty() || value !is String) null else field to value
            }
            .toMap()
            .takeIf { it.isNotEmpty() }

    /** Builds the resume-card snapshot from a game's raw fields, or null. */
    private fun snapshotOf(gameId: String, fields: Map<String, String>): GameSessionSnapshot? =
        fields[FIELD_DIFFICULTY]?.let { difficulty ->
            GameSessionSnapshot(
                gameId = gameId,
                difficulty = difficulty,
                progress = fields[FIELD_PROGRESS]?.toFloatOrNull() ?: 0f,
                elapsedMs = fields[FIELD_ELAPSED]?.toLongOrNull() ?: 0L,
                mistakes = fields[FIELD_MISTAKES]?.toIntOrNull() ?: 0,
            )
        }

    /**
     * Applies [transform], treating an unwritable store as a dropped save.
     *
     * Reads are not the only thing that can fail on a real device. Every caller here is a
     * fire-and-forget `launch` — autosave after a move, a settings tap — so an escaping
     * `IOException` would reach the coroutine's uncaught handler and take the process
     * down. Losing one autosave is the right trade against losing the game in progress.
     */
    private suspend fun edit(transform: (MutablePreferences) -> Unit) {
        try {
            context.dataStore.edit(transform)
        } catch (e: IOException) {
            Log.w(TAG, "could not write preferences, change not saved", e)
        }

}
    private companion object {
        const val TAG = "SessionStore"
        const val STATS_PREFIX = "stats."
        const val SESSION_PREFIX = "session."
        const val DAILY_PREFIX = "daily."
}

}
/**
 * The five plain values the shell's resume card renders for a game in progress.
 *
 * Deliberately game-agnostic: every game supplies the same shape from its own encoded
 * fields, so adding a game needs no change to the shell.
 */
data class GameSessionSnapshot(
    val gameId: String,
    val difficulty: String,
    val progress: Float,
    val elapsedMs: Long,
    val mistakes: Int,
)

/** The field names every game's session uses for the resume-card values. */
private const val FIELD_DIFFICULTY = "difficulty"
private const val FIELD_PROGRESS = "progress"
private const val FIELD_ELAPSED = "elapsedMs"
private const val FIELD_MISTAKES = "mistakes"