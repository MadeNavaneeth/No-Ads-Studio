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
import androidx.datastore.preferences.core.floatPreferencesKey
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
 * DataStore Preferences, not Room. Room needs KSP annotation processing, which taxes
 * every build on a 2-core machine for the rest of the project's life, and there is one
 * table's worth of state here. One store, two key prefixes: `sudoku9.` for the game and
 * `settings.` for preferences.
 *
 * Nothing leaves the device. This is the whole persistence layer.
 *
 * ## Failure is not optional here
 *
 * The store is read during composition in `MainActivity` and in the Sudoku view model's
 * `init`. Neither can catch an exception, so an unreadable preferences file would take
 * the app down at launch with no way for the user to recover short of clearing app data.
 * Two layers prevent that, because they fail for different reasons:
 *
 * - [ReplaceFileCorruptionHandler] handles an unparseable file — a process killed
 *   mid-write, or storage that lost a block. DataStore discards it and starts empty.
 * - [safeData] handles an `IOException` on read, which corruption handling does not
 *   cover: a permissions problem, or a full disk.
 *
 * A save the app cannot read means "no saved game". It never means a crash.
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
     * loudly rather than be hidden behind an empty grid.
     */
    private val safeData: Flow<Preferences> = context.dataStore.data
        .catch { cause ->
            if (cause is IOException) {
                Log.w("SessionStore", "could not read preferences, using defaults", cause)
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
     *
     * Defaults on. The count was always computable by the player, so showing it exposes the
     * mechanism rather than doing the puzzle for them — but some players read any such aid as
     * assistance, which is why it is a preference and not a decision made for them.
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
     *
     * Defaults on: the sweep removes only marks the placed digit has *proven* wrong,
     * so it is bookkeeping the board already knows — the same standard as the remaining
     * counts. Undo reverses it, so nothing is ever lost to it.
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
    // `GameResult` and never learns what the game is. Contrast the sudoku(9) session keys
    // below, which are game-specific and are the part of this class that does not scale.

    private fun statsKey(gameId: String) = stringPreferencesKey("$STATS_PREFIX$gameId")

    /** Stats for [gameId]. Empty for a game that has never been finished. */
    fun stats(gameId: String): Flow<GameStats> =
        safeData.map { StatsCodec.decode(it[statsKey(gameId)]) }

    /**
     * Every game's stats, keyed by game id.
     *
     * Derived by scanning for the key prefix rather than by asking the registry, so this
     * still returns the record of a game that has since been removed from the build — a
     * standalone flavour reads the same stats the studio build wrote. Losing someone's
     * history because they installed a different edition would be the wrong behaviour.
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
     * Folds a finished game into the stored stats.
     *
     * Read-modify-write inside one `edit` block, which DataStore serialises, so two games
     * finishing at once cannot lose a record.
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

    // ─── the daily puzzle slot ──────────────────────────────────────────────────
    //
    // The one-per-day local puzzle keeps its own keys so that starting, playing, or
    // finishing the day's puzzle never touches the regular in-progress session, and vice
    // versa. Two deliberate choices make this work:
    //
    // - The prefix is `daily.sudoku9.`, **not** `session.`. `allSessions` scans for the
    //   `session.` prefix to build the home resume cards, and each card resolves its
    //   game id through `GameRegistry`. A daily session has no registry entry — it is
    //   reached through the game's own NEW picker, not through a resume card — so it
    //   must stay invisible to that scan.
    // - The day is stored alongside the session. A daily session only ever belongs to
    //   one UTC day, and knowing which one lets the game decide whether a stored session
    //   is today's (resume it) or yesterday's (discard it and generate today's).
    private object Daily {
        val day = longPreferencesKey("daily.sudoku9.day")
        val givens = stringPreferencesKey("daily.sudoku9.givens")
        val entries = stringPreferencesKey("daily.sudoku9.entries")
        val notes = stringPreferencesKey("daily.sudoku9.notes")
        val elapsedMs = longPreferencesKey("daily.sudoku9.elapsedMs")
        val difficulty = stringPreferencesKey("daily.sudoku9.difficulty")
        val mistakes = intPreferencesKey("daily.sudoku9.mistakes")
    }

    /**
     * The stored daily session, with the UTC day it belongs to, or `null` when there is
     * none or it is unreadable. Day and session travel together so the caller can tell
     * "today's puzzle, in progress" from "a previous day's leftovers" without decoding
     * the grids first.
     */
    fun dailyPuzzle(): Flow<DailyPuzzle?> = safeData.map { prefs ->
        val day = prefs[Daily.day] ?: return@map null
        val givens = SudokuCodec.decodeGrid(prefs[Daily.givens]) ?: return@map null
        val entries = SudokuCodec.decodeGrid(prefs[Daily.entries]) ?: return@map null
        val notes = SudokuCodec.decodeNotes(prefs[Daily.notes]) ?: SudokuCodec.emptyGrid()
        DailyPuzzle(
            day = day,
            session = SudokuSession(
                givens = givens,
                entries = entries,
                notes = notes,
                elapsedMs = prefs[Daily.elapsedMs] ?: 0L,
                difficulty = prefs[Daily.difficulty] ?: DAILY_DIFFICULTY,
                mistakes = prefs[Daily.mistakes] ?: 0,
            ),
        )
    }

    /** Saves [session] as the daily puzzle belonging to [day]. */
    suspend fun saveDailyPuzzle(day: Long, session: SudokuSession) = edit { prefs ->
        prefs[Daily.day] = day
        prefs[Daily.givens] = SudokuCodec.encodeGrid(session.givens)
        prefs[Daily.entries] = SudokuCodec.encodeGrid(session.entries)
        prefs[Daily.notes] = SudokuCodec.encodeNotes(session.notes)
        prefs[Daily.elapsedMs] = session.elapsedMs
        prefs[Daily.difficulty] = session.difficulty
        prefs[Daily.mistakes] = session.mistakes
    }

    /**
     * Removes the stored daily puzzle — called when the day's puzzle is finished, so it
     * is not restored as in-progress, or when it turns out to belong to an earlier day.
     */
    suspend fun clearDailyPuzzle() = edit { prefs ->
        prefs.remove(Daily.day)
        prefs.remove(Daily.givens)
        prefs.remove(Daily.entries)
        prefs.remove(Daily.notes)
        prefs.remove(Daily.elapsedMs)
        prefs.remove(Daily.difficulty)
        prefs.remove(Daily.mistakes)
    }

    // ─── daily slots for the other four games (decision D32) ───────────────────
    //
    // Decision D32 generalises sudoku's D31 daily to every game: one deterministic
    // board per UTC day, seeded from the day, carried under the game's Daily label,
    // stored in its own `daily.<gameId>.*` keys so `allSessions` never offers it as
    // a resume card — a daily is re-entered through the game's own picker. The shape
    // of each slot mirrors that game's regular slot exactly; only the prefix and the
    // stored day differ.

    private fun dailyKey(gameId: String, name: String) = stringPreferencesKey("daily.$gameId.$name")

    /** The stored day is numeric — sudoku's `DailyPuzzle` carries it the same way. */
    private fun dailyDayKey(gameId: String) = longPreferencesKey("daily.$gameId.day")

    // ── nonogram daily ──
    /** Today's nonogram daily, or null when none is stored or [day] differs. */
    fun dailyNonogram(day: Long): Flow<NonogramSession?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(NONOGRAM_GAME_ID)] != day) return@map null
        decodeNonogramDaily(prefs)
    }

    private fun decodeNonogramDaily(prefs: Preferences): NonogramSession? {
        val solution = NonogramCodec.decodeBitmap(prefs[dailyKey(NONOGRAM_GAME_ID, "solution")])
            ?: return null
        val cells = NonogramCodec.decodeCells(prefs[dailyKey(NONOGRAM_GAME_ID, "cells")])
            ?: return null
        return NonogramSession(
            solution = solution,
            cells = cells,
            elapsedMs = prefs[dailyKey(NONOGRAM_GAME_ID, "elapsedMs")]?.toLongOrNull() ?: 0L,
            difficulty = prefs[dailyKey(NONOGRAM_GAME_ID, "difficulty")] ?: DAILY_DIFFICULTY,
            mistakes = prefs[dailyKey(NONOGRAM_GAME_ID, "mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[dailyKey(NONOGRAM_GAME_ID, "progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveDailyNonogram(day: Long, session: NonogramSession) = edit { prefs ->
        prefs[dailyDayKey(NONOGRAM_GAME_ID)] = day
        prefs[dailyKey(NONOGRAM_GAME_ID, "solution")] = NonogramCodec.encodeBitmap(session.solution)
        prefs[dailyKey(NONOGRAM_GAME_ID, "cells")] = NonogramCodec.encodeCells(session.cells)
        prefs[dailyKey(NONOGRAM_GAME_ID, "elapsedMs")] = session.elapsedMs.toString()
        prefs[dailyKey(NONOGRAM_GAME_ID, "difficulty")] = session.difficulty
        prefs[dailyKey(NONOGRAM_GAME_ID, "mistakes")] = session.mistakes.toString()
        prefs[dailyKey(NONOGRAM_GAME_ID, "progress")] = session.progress.toString()
    }

    suspend fun clearDailyNonogram() = edit { prefs ->
        listOf("day", "solution", "cells", "elapsedMs", "difficulty", "mistakes", "progress")
            .forEach { prefs.remove(dailyKey(NONOGRAM_GAME_ID, it)) }
    }

    // ── minesweeper daily ──
    /** Today's minesweeper daily, or null when none is stored or [day] differs. */
    fun dailyMinesweeper(day: Long): Flow<MinesweeperSession?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(MINESWEEPER_GAME_ID)] != day) return@map null
        decodeMinesweeperDaily(prefs)
    }

    private fun decodeMinesweeperDaily(prefs: Preferences): MinesweeperSession? {
        val mines = MinesweeperCodec.decodeMines(prefs[dailyKey(MINESWEEPER_GAME_ID, "mines")])
            ?: return null
        val cells = MinesweeperCodec.decodeCells(prefs[dailyKey(MINESWEEPER_GAME_ID, "cells")])
            ?: return null
        return MinesweeperSession(
            mines = mines,
            cells = cells,
            difficulty = prefs[dailyKey(MINESWEEPER_GAME_ID, "difficulty")] ?: DAILY_DIFFICULTY,
            elapsedMs = prefs[dailyKey(MINESWEEPER_GAME_ID, "elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = prefs[dailyKey(MINESWEEPER_GAME_ID, "mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[dailyKey(MINESWEEPER_GAME_ID, "progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveDailyMinesweeper(day: Long, session: MinesweeperSession) = edit { prefs ->
        prefs[dailyDayKey(MINESWEEPER_GAME_ID)] = day
        prefs[dailyKey(MINESWEEPER_GAME_ID, "mines")] = MinesweeperCodec.encodeMines(session.mines)
        prefs[dailyKey(MINESWEEPER_GAME_ID, "cells")] = MinesweeperCodec.encodeCells(session.cells)
        prefs[dailyKey(MINESWEEPER_GAME_ID, "difficulty")] = session.difficulty
        prefs[dailyKey(MINESWEEPER_GAME_ID, "elapsedMs")] = session.elapsedMs.toString()
        prefs[dailyKey(MINESWEEPER_GAME_ID, "mistakes")] = session.mistakes.toString()
        prefs[dailyKey(MINESWEEPER_GAME_ID, "progress")] = session.progress.toString()
    }

    suspend fun clearDailyMinesweeper() = edit { prefs ->
        listOf("day", "mines", "cells", "difficulty", "elapsedMs", "mistakes", "progress")
            .forEach { prefs.remove(dailyKey(MINESWEEPER_GAME_ID, it)) }
    }

    // ── connect daily ──
    /** Today's connect daily, or null when none is stored or [day] differs. */
    fun dailyConnect(day: Long): Flow<ConnectSession?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(CONNECT_GAME_ID)] != day) return@map null
        decodeConnectDaily(prefs)
    }

    private fun decodeConnectDaily(prefs: Preferences): ConnectSession? {
        val board = ConnectCodec.decodeBoard(prefs[dailyKey(CONNECT_GAME_ID, "board")])
            ?: return null
        return ConnectSession(
            board = board,
            difficulty = prefs[dailyKey(CONNECT_GAME_ID, "difficulty")] ?: DAILY_DIFFICULTY,
            elapsedMs = prefs[dailyKey(CONNECT_GAME_ID, "elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = prefs[dailyKey(CONNECT_GAME_ID, "mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[dailyKey(CONNECT_GAME_ID, "progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveDailyConnect(day: Long, session: ConnectSession) = edit { prefs ->
        prefs[dailyDayKey(CONNECT_GAME_ID)] = day
        prefs[dailyKey(CONNECT_GAME_ID, "board")] = ConnectCodec.encodeBoard(session.board)
        prefs[dailyKey(CONNECT_GAME_ID, "difficulty")] = session.difficulty
        prefs[dailyKey(CONNECT_GAME_ID, "elapsedMs")] = session.elapsedMs.toString()
        prefs[dailyKey(CONNECT_GAME_ID, "mistakes")] = session.mistakes.toString()
        prefs[dailyKey(CONNECT_GAME_ID, "progress")] = session.progress.toString()
    }

    suspend fun clearDailyConnect() = edit { prefs ->
        listOf("day", "board", "difficulty", "elapsedMs", "mistakes", "progress")
            .forEach { prefs.remove(dailyKey(CONNECT_GAME_ID, it)) }
    }

    // ── wordsearch daily ──
    /** Today's wordsearch daily, or null when none is stored or [day] differs. */
    fun dailyWordsearch(day: Long): Flow<WordsearchSession?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(WORDSEARCH_GAME_ID)] != day) return@map null
        decodeWordsearchDaily(prefs)
    }

    private fun decodeWordsearchDaily(prefs: Preferences): WordsearchSession? {
        val letters = WordsearchCodec.decodeLetters(prefs[dailyKey(WORDSEARCH_GAME_ID, "letters")])
            ?: return null
        val found = WordsearchCodec.decodeFound(prefs[dailyKey(WORDSEARCH_GAME_ID, "found")])
            ?: return null
        val placements = WordsearchCodec.decodePlacements(
            prefs[dailyKey(WORDSEARCH_GAME_ID, "placements")]
        ) ?: return null
        return WordsearchSession(
            letters = letters,
            found = found,
            placements = placements,
            difficulty = prefs[dailyKey(WORDSEARCH_GAME_ID, "difficulty")] ?: DAILY_DIFFICULTY,
            elapsedMs = prefs[dailyKey(WORDSEARCH_GAME_ID, "elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = prefs[dailyKey(WORDSEARCH_GAME_ID, "mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[dailyKey(WORDSEARCH_GAME_ID, "progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveDailyWordsearch(day: Long, session: WordsearchSession) = edit { prefs ->
        prefs[dailyDayKey(WORDSEARCH_GAME_ID)] = day
        prefs[dailyKey(WORDSEARCH_GAME_ID, "letters")] = WordsearchCodec.encodeLetters(session.letters)
        prefs[dailyKey(WORDSEARCH_GAME_ID, "found")] = WordsearchCodec.encodeFound(session.found)
        prefs[dailyKey(WORDSEARCH_GAME_ID, "placements")] =
            WordsearchCodec.encodePlacements(session.placements)
        prefs[dailyKey(WORDSEARCH_GAME_ID, "difficulty")] = session.difficulty
        prefs[dailyKey(WORDSEARCH_GAME_ID, "elapsedMs")] = session.elapsedMs.toString()
        prefs[dailyKey(WORDSEARCH_GAME_ID, "mistakes")] = session.mistakes.toString()
        prefs[dailyKey(WORDSEARCH_GAME_ID, "progress")] = session.progress.toString()
    }

    suspend fun clearDailyWordsearch() = edit { prefs ->
        listOf("day", "letters", "found", "placements", "difficulty", "elapsedMs", "mistakes", "progress")
            .forEach { prefs.remove(dailyKey(WORDSEARCH_GAME_ID, it)) }
    }

    /**
     * Which games currently hold an unfinished daily (decision D32), for the home
     * screen's per-game TODAY badge.
     *
     * Key presence **is** the state: a daily session is saved the moment its run
     * starts and its slot is cleared the moment the run finishes, so `daily.<id>.day`
     * existing means exactly "that game's daily is in progress". The day the session
     * belongs to is not checked here — a stale session from yesterday is a leftover
     * the game's picker will discard, and hiding the badge for it would be worse than
     * showing it one session early. Sudoku's daily carries its own legacy key shape,
     * so its prefix is listed separately.
     */
    val activeDailies: Flow<Set<String>> = safeData.map { prefs ->
        prefs.asMap().keys.mapNotNull { key ->
            val name = key.name
            when {
                name.startsWith("daily.sudoku9.") -> SUDOKU_GAME_ID
                name.startsWith("daily.") -> {
                    // `daily.<gameId>.<field>` — the game id is the second segment.
                    name.removePrefix("daily.").substringBefore('.').takeIf { it.isNotEmpty() }
                }
                else -> null
            }
        }.toSet()
    }

    // ─── game sessions — one key per game (refactor from hard-coded sudoku9.*) ──
    // Old hard-coded sudoku(9) keys kept for migration; new games use session.<gameId>.*
    private object Sudoku {
        val givens = stringPreferencesKey("sudoku9.givens")
        val entries = stringPreferencesKey("sudoku9.entries")
        val notes = stringPreferencesKey("sudoku9.notes")
        val elapsedMs = longPreferencesKey("sudoku9.elapsedMs")
        val difficulty = stringPreferencesKey("sudoku9.difficulty")
        val mistakes = intPreferencesKey("sudoku9.mistakes")
    }

    private fun sessionGivensKey(gameId: String) = stringPreferencesKey("session.$gameId.givens")
    private fun sessionEntriesKey(gameId: String) = stringPreferencesKey("session.$gameId.entries")
    private fun sessionNotesKey(gameId: String) = stringPreferencesKey("session.$gameId.notes")
    private fun sessionElapsedKey(gameId: String) = longPreferencesKey("session.$gameId.elapsedMs")
    private fun sessionDifficultyKey(gameId: String) = stringPreferencesKey("session.$gameId.difficulty")
    private fun sessionMistakesKey(gameId: String) = intPreferencesKey("session.$gameId.mistakes")

    private fun decodeSessionFor(
        prefs: Preferences,
        gameId: String,
    ): SudokuSession? {
        // Prefer new per-game keys; fall back to legacy sudoku9.* for migration when
        // gameId is the sudoku id. That keeps an existing install's save alive after the
        // update without a manual migration step.
        val givensEnc = prefs[sessionGivensKey(gameId)]
            ?: if (gameId == SUDOKU_GAME_ID) prefs[Sudoku.givens] else null
        val entriesEnc = prefs[sessionEntriesKey(gameId)]
            ?: if (gameId == SUDOKU_GAME_ID) prefs[Sudoku.entries] else null
        val notesEnc = prefs[sessionNotesKey(gameId)]
            ?: if (gameId == SUDOKU_GAME_ID) prefs[Sudoku.notes] else null
        val givens = SudokuCodec.decodeGrid(givensEnc) ?: return null
        val entries = SudokuCodec.decodeGrid(entriesEnc) ?: return null
        val notes = SudokuCodec.decodeNotes(notesEnc) ?: SudokuCodec.emptyGrid()
        return SudokuSession(
            givens = givens,
            entries = entries,
            notes = notes,
            elapsedMs = (prefs[sessionElapsedKey(gameId)]
                ?: if (gameId == SUDOKU_GAME_ID) prefs[Sudoku.elapsedMs] else null) ?: 0L,
            difficulty = (prefs[sessionDifficultyKey(gameId)]
                ?: if (gameId == SUDOKU_GAME_ID) prefs[Sudoku.difficulty] else null) ?: "Moderate",
            mistakes = (prefs[sessionMistakesKey(gameId)]
                ?: if (gameId == SUDOKU_GAME_ID) prefs[Sudoku.mistakes] else null) ?: 0,
        )
    }

    /** Session for a specific game id — the new per-game API. */
    fun session(gameId: String): Flow<SudokuSession?> = safeData.map { prefs ->
        decodeSessionFor(prefs, gameId)
    }

    /**
     * All unfinished sessions, keyed by game id.
     * Scans for `session.<id>.givens` rather than asking GameRegistry, so a stale
     * install that still holds a save for a removed flavour keeps the resume card.
     */
    val allSessions: Flow<Map<String, GameSession>> = safeData.map { prefs ->
        prefs.asMap().keys
            .mapNotNull { key ->
                if (!key.name.startsWith(SESSION_PREFIX) || !key.name.endsWith(".givens")) return@mapNotNull null
                key.name.removePrefix(SESSION_PREFIX).removeSuffix(".givens")
                    .takeIf { it.isNotEmpty() }
            }
            .distinct()
            .mapNotNull { gameId ->
                decodeSessionFor(prefs, gameId)?.let { gameId to it }
            }
            .toMap()
            // Include legacy sudoku9.* save if no new session.sudoku.givens yet
            .let { map ->
                val withLegacy = if (map.containsKey(SUDOKU_GAME_ID)) map else {
                    val g = SudokuCodec.decodeGrid(prefs[Sudoku.givens]) ?: return@let map
                    val e = SudokuCodec.decodeGrid(prefs[Sudoku.entries]) ?: return@let map
                    val n = SudokuCodec.decodeNotes(prefs[Sudoku.notes]) ?: SudokuCodec.emptyGrid()
                    val legacySession = SudokuSession(
                        givens = g, entries = e, notes = n,
                        elapsedMs = prefs[Sudoku.elapsedMs] ?: 0L,
                        difficulty = prefs[Sudoku.difficulty] ?: "Moderate",
                        mistakes = prefs[Sudoku.mistakes] ?: 0,
                    )
                    map + (SUDOKU_GAME_ID to legacySession)
                }
                // The picture-logic slot decodes under its own codec — a third game
                // adds one line here rather than another scan or another map.
                val withNonogram = decodeNonogram(prefs)?.let { withLegacy + (NONOGRAM_GAME_ID to it) } ?: withLegacy
                // The minefield slot, same shape — the fourth game, one more line.
                val withMinesweeper = decodeMinesweeper(prefs)?.let { withNonogram + (MINESWEEPER_GAME_ID to it) } ?: withNonogram
                // The route-drawing slot, same shape — the fifth game, one more line.
                val withConnect = decodeConnect(prefs)?.let { withMinesweeper + (CONNECT_GAME_ID to it) } ?: withMinesweeper
                // The letter-hunt slot, same shape — the sixth game, one more line.
                val withWordsearch = decodeWordsearch(prefs)?.let { withConnect + (WORDSEARCH_GAME_ID to it) } ?: withConnect
                // The block-run slot, same shape — the seventh game, one more line.
                val withBlock = decodeBlock(prefs)?.let { withWordsearch + (BLOCK_GAME_ID to it) } ?: withWordsearch
                // The light-up slot, same shape — the eighth game, one more line.
                val withAkari = decodeAkari(prefs)?.let { withBlock + (AKARI_GAME_ID to it) } ?: withBlock
                // The binary-line slot, same shape — the ninth game, one more line.
                decodeBinairo(prefs)?.let { withAkari + (BINAIRO_GAME_ID to it) } ?: withAkari
            }
    }

    suspend fun saveSession(gameId: String, session: SudokuSession) = edit { prefs ->
        prefs[sessionGivensKey(gameId)] = SudokuCodec.encodeGrid(session.givens)
        prefs[sessionEntriesKey(gameId)] = SudokuCodec.encodeGrid(session.entries)
        prefs[sessionNotesKey(gameId)] = SudokuCodec.encodeNotes(session.notes)
        prefs[sessionElapsedKey(gameId)] = session.elapsedMs
        prefs[sessionDifficultyKey(gameId)] = session.difficulty
        prefs[sessionMistakesKey(gameId)] = session.mistakes
    }

    suspend fun clearSession(gameId: String) = edit { prefs ->
        prefs.remove(sessionGivensKey(gameId))
        prefs.remove(sessionEntriesKey(gameId))
        prefs.remove(sessionNotesKey(gameId))
        prefs.remove(sessionElapsedKey(gameId))
        prefs.remove(sessionDifficultyKey(gameId))
        prefs.remove(sessionMistakesKey(gameId))
        // Also clear legacy keys when clearing the canonical sudoku id, so a
        // completed puzzle doesn't resurrect from the old location.
        if (gameId == SUDOKU_GAME_ID) {
            prefs.remove(Sudoku.givens)
            prefs.remove(Sudoku.entries)
            prefs.remove(Sudoku.notes)
            prefs.remove(Sudoku.elapsedMs)
            prefs.remove(Sudoku.difficulty)
            prefs.remove(Sudoku.mistakes)
        }
    }

    /**
     * The most recent incomplete session, or `null` if there is none or the stored data
     * is unreadable. A corrupt save means "start fresh", never a crash.
     * Kept for backward compat — delegates to the per-game API.
     */
    val sudokuSession: Flow<SudokuSession?> = session(SUDOKU_GAME_ID)

    // ─── nonogram session — one game, one slot, same key shape ──────────────
    //
    // Decoded under NonogramCodec rather than SudokuCodec: same `session.<id>.*`
    // shape, game-specific value layout. allSessions above picks it up under
    // NONOGRAM_GAME_ID, so the resume card needs no other wiring.

    private fun nonogramKey(name: String) = stringPreferencesKey("session.$NONOGRAM_GAME_ID.$name")

    fun nonogramSession(): Flow<NonogramSession?> = safeData.map { prefs ->
        decodeNonogram(prefs)
    }

    private fun decodeNonogram(prefs: Preferences): NonogramSession? {
        // Structural validity only — null on malformed. Semantic checks (fill
        // range, already-complete) belong to the game's restore step, which owns
        // those invariants; see NonogramRestore. Mirrors decodeSessionFor above.
        val solution = NonogramCodec.decodeBitmap(prefs[nonogramKey("solution")]) ?: return null
        val cells = NonogramCodec.decodeCells(prefs[nonogramKey("cells")]) ?: return null
        return NonogramSession(
            solution = solution,
            cells = cells,
            elapsedMs = prefs[nonogramKey("elapsedMs")]?.toLongOrNull() ?: 0L,
            difficulty = prefs[nonogramKey("difficulty")] ?: "Moderate",
            mistakes = prefs[nonogramKey("mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[nonogramKey("progress")]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
        )
    }

    suspend fun saveNonogramSession(session: NonogramSession) = edit { prefs ->
        prefs[nonogramKey("solution")] = NonogramCodec.encodeBitmap(session.solution)
        prefs[nonogramKey("cells")] = NonogramCodec.encodeCells(session.cells)
        prefs[nonogramKey("elapsedMs")] = session.elapsedMs.toString()
        prefs[nonogramKey("difficulty")] = session.difficulty
        prefs[nonogramKey("mistakes")] = session.mistakes.toString()
        prefs[nonogramKey("progress")] = session.progress.toString()
    }

    /** Called on completion, so a finished puzzle is not restored as in-progress. */
    suspend fun clearNonogramSession() = edit { prefs ->
        prefs.remove(nonogramKey("solution"))
        prefs.remove(nonogramKey("cells"))
        prefs.remove(nonogramKey("elapsedMs"))
        prefs.remove(nonogramKey("difficulty"))
        prefs.remove(nonogramKey("mistakes"))
        prefs.remove(nonogramKey("progress"))
    }

    // ─── minesweeper session — one game, one slot, same key shape ───────────
    //
    // Decoded under MinesweeperCodec rather than NonogramCodec: same
    // `session.<id>.*` shape, game-specific value layout. allSessions above
    // picks it up under MINESWEEPER_GAME_ID, so the resume card needs no other
    // wiring. A fourth game adds one more block here.

    private fun minesweeperKey(name: String) = stringPreferencesKey("session.$MINESWEEPER_GAME_ID.$name")

    fun minesweeperSession(): Flow<MinesweeperSession?> = safeData.map { prefs ->
        decodeMinesweeper(prefs)
    }

    private fun decodeMinesweeper(prefs: Preferences): MinesweeperSession? {
        // Structural validity only — null on malformed. Semantic checks (mine count
        // against the difficulty, an already-detonated board) belong to the game's
        // restore step, which owns those invariants; see MinesweeperRestore.
        val mines = MinesweeperCodec.decodeMines(prefs[minesweeperKey("mines")]) ?: return null
        val cells = MinesweeperCodec.decodeCells(prefs[minesweeperKey("cells")]) ?: return null
        return MinesweeperSession(
            mines = mines,
            cells = cells,
            difficulty = prefs[minesweeperKey("difficulty")] ?: "Moderate",
            elapsedMs = prefs[minesweeperKey("elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = prefs[minesweeperKey("mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[minesweeperKey("progress")]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
        )
    }

    suspend fun saveMinesweeperSession(session: MinesweeperSession) = edit { prefs ->
        prefs[minesweeperKey("mines")] = MinesweeperCodec.encodeMines(session.mines)
        prefs[minesweeperKey("cells")] = MinesweeperCodec.encodeCells(session.cells)
        prefs[minesweeperKey("difficulty")] = session.difficulty
        prefs[minesweeperKey("elapsedMs")] = session.elapsedMs.toString()
        prefs[minesweeperKey("mistakes")] = session.mistakes.toString()
        prefs[minesweeperKey("progress")] = session.progress.toString()
    }

    /** Called on completion, so a finished board is not restored as in-progress. */
    suspend fun clearMinesweeperSession() = edit { prefs ->
        prefs.remove(minesweeperKey("mines"))
        prefs.remove(minesweeperKey("cells"))
        prefs.remove(minesweeperKey("difficulty"))
        prefs.remove(minesweeperKey("elapsedMs"))
        prefs.remove(minesweeperKey("mistakes"))
        prefs.remove(minesweeperKey("progress"))
    }

    // ─── connect session — one game, one slot, same key shape ───────────────
    //
    // Decoded under ConnectCodec: same `session.<id>.*` shape, game-specific value
    // layout. allSessions above picks it up under CONNECT_GAME_ID, so the resume
    // card needs no other wiring. A sixth game adds one more block here.

    private fun connectKey(name: String) = stringPreferencesKey("session.$CONNECT_GAME_ID.$name")

    fun connectSession(): Flow<ConnectSession?> = safeData.map { prefs ->
        decodeConnect(prefs)
    }

    private fun decodeConnect(prefs: Preferences): ConnectSession? {
        // Structural validity only — null on malformed. Semantic checks (routable
        // segments, an already-solved board) belong to the game's restore step, which
        // owns those invariants; see games.connect.ConnectRestore.
        val board = ConnectCodec.decodeBoard(prefs[connectKey("board")]) ?: return null
        return ConnectSession(
            board = board,
            difficulty = prefs[connectKey("difficulty")] ?: "Moderate",
            elapsedMs = prefs[connectKey("elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = prefs[connectKey("mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[connectKey("progress")]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
        )
    }

    suspend fun saveConnectSession(session: ConnectSession) = edit { prefs ->
        prefs[connectKey("board")] = ConnectCodec.encodeBoard(session.board)
        prefs[connectKey("difficulty")] = session.difficulty
        prefs[connectKey("elapsedMs")] = session.elapsedMs.toString()
        prefs[connectKey("mistakes")] = session.mistakes.toString()
        prefs[connectKey("progress")] = session.progress.toString()
    }

    /** Called on completion, so a solved board is not restored as in-progress. */
    suspend fun clearConnectSession() = edit { prefs ->
        prefs.remove(connectKey("board"))
        prefs.remove(connectKey("difficulty"))
        prefs.remove(connectKey("elapsedMs"))
        prefs.remove(connectKey("mistakes"))
        prefs.remove(connectKey("progress"))
    }

    // ─── wordsearch session — one game, one slot, same key shape ────────────
    //
    // Decoded under WordsearchCodec: same `session.<id>.*` shape, game-specific
    // value layout. allSessions above picks it up under WORDSEARCH_GAME_ID, so the
    // resume card needs no other wiring. A seventh game adds one more block here.

    private fun wordsearchKey(name: String) = stringPreferencesKey("session.$WORDSEARCH_GAME_ID.$name")

    fun wordsearchSession(): Flow<WordsearchSession?> = safeData.map { prefs ->
        decodeWordsearch(prefs)
    }

    private fun decodeWordsearch(prefs: Preferences): WordsearchSession? {
        // Structural validity only — null on malformed. Semantic checks (placements
        // that spell their words, an already-complete grid) belong to the game's
        // restore step; see games.wordsearch.WordsearchRestore.
        val letters = WordsearchCodec.decodeLetters(prefs[wordsearchKey("letters")]) ?: return null
        val found = WordsearchCodec.decodeFound(prefs[wordsearchKey("found")]) ?: return null
        val placements = WordsearchCodec.decodePlacements(prefs[wordsearchKey("placements")])
            ?: return null
        return WordsearchSession(
            letters = letters,
            found = found,
            placements = placements,
            difficulty = prefs[wordsearchKey("difficulty")] ?: "Moderate",
            elapsedMs = prefs[wordsearchKey("elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = prefs[wordsearchKey("mistakes")]?.toIntOrNull() ?: 0,
            progress = prefs[wordsearchKey("progress")]?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 0f,
        )
    }

    suspend fun saveWordsearchSession(session: WordsearchSession) = edit { prefs ->
        prefs[wordsearchKey("letters")] = WordsearchCodec.encodeLetters(session.letters)
        prefs[wordsearchKey("found")] = WordsearchCodec.encodeFound(session.found)
        prefs[wordsearchKey("placements")] = WordsearchCodec.encodePlacements(session.placements)
        prefs[wordsearchKey("difficulty")] = session.difficulty
        prefs[wordsearchKey("elapsedMs")] = session.elapsedMs.toString()
        prefs[wordsearchKey("mistakes")] = session.mistakes.toString()
        prefs[wordsearchKey("progress")] = session.progress.toString()
    }

    /** Called on completion, so a finished grid is not restored as in-progress. */
    suspend fun clearWordsearchSession() = edit { prefs ->
        prefs.remove(wordsearchKey("letters"))
        prefs.remove(wordsearchKey("found"))
        prefs.remove(wordsearchKey("placements"))
        prefs.remove(wordsearchKey("difficulty"))
        prefs.remove(wordsearchKey("elapsedMs"))
        prefs.remove(wordsearchKey("mistakes"))
        prefs.remove(wordsearchKey("progress"))
    }

    // ─── blockpuzzle session — one game, one slot, same key shape ──────────
    //
    // Decoded under BlockCodec: same `session.<id>.*` shape, game-specific value
    // layout. allSessions above picks it up under BLOCK_GAME_ID, so the resume card
    // needs no other wiring. The tray travels as `family:variant:density` triples —
    // never geometry, the catalog is the geometry.

    private fun blockKey(name: String) = stringPreferencesKey("session.$BLOCK_GAME_ID.$name")

    fun blockSession(): Flow<BlockSession?> = safeData.map { prefs ->
        decodeBlock(prefs)
    }

    private fun decodeBlock(prefs: Preferences): BlockSession? {
        // Structural validity only — null on malformed. Semantic checks (tray triples
        // naming real catalog shapes, an already-finished run) belong to the game's
        // restore step; see games.blockpuzzle.BlockRestore.
        val cells = BlockCodec.decodeCells(prefs[blockKey("cells")]) ?: return null
        val tray = BlockCodec.decodeTray(prefs[blockKey("tray")]) ?: return null
        return BlockSession(
            cells = cells,
            tray = tray,
            score = prefs[blockKey("score")]?.toIntOrNull() ?: 0,
            difficulty = prefs[blockKey("difficulty")] ?: "Classic",
            elapsedMs = prefs[blockKey("elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = 0,
            progress = prefs[blockKey("progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveBlockSession(session: BlockSession) = edit { prefs ->
        prefs[blockKey("cells")] = BlockCodec.encodeCells(session.cells)
        prefs[blockKey("tray")] = BlockCodec.encodeTray(session.tray)
        prefs[blockKey("score")] = session.score.toString()
        prefs[blockKey("difficulty")] = session.difficulty
        prefs[blockKey("elapsedMs")] = session.elapsedMs.toString()
        prefs[blockKey("progress")] = session.progress.toString()
    }

    /** Called on completion, so a finished run is not restored as in-progress. */
    suspend fun clearBlockSession() = edit { prefs ->
        listOf("cells", "tray", "score", "difficulty", "elapsedMs", "progress")
            .forEach { prefs.remove(blockKey(it)) }
    }

    // ─── blockpuzzle daily (decision D32) ──────────────────────────────────

    /** Today's block daily, or null when none is stored or [day] differs. */
    fun dailyBlock(day: Long): Flow<BlockSession?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(BLOCK_GAME_ID)] != day) return@map null
        val cells = BlockCodec.decodeCells(prefs[dailyKey(BLOCK_GAME_ID, "cells")])
            ?: return@map null
        val tray = BlockCodec.decodeTray(prefs[dailyKey(BLOCK_GAME_ID, "tray")])
            ?: return@map null
        BlockSession(
            cells = cells,
            tray = tray,
            score = prefs[dailyKey(BLOCK_GAME_ID, "score")]?.toIntOrNull() ?: 0,
            difficulty = prefs[dailyKey(BLOCK_GAME_ID, "difficulty")] ?: DAILY_DIFFICULTY,
            elapsedMs = prefs[dailyKey(BLOCK_GAME_ID, "elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = 0,
            progress = prefs[dailyKey(BLOCK_GAME_ID, "progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveDailyBlock(day: Long, session: BlockSession) = edit { prefs ->
        prefs[dailyDayKey(BLOCK_GAME_ID)] = day
        prefs[dailyKey(BLOCK_GAME_ID, "cells")] = BlockCodec.encodeCells(session.cells)
        prefs[dailyKey(BLOCK_GAME_ID, "tray")] = BlockCodec.encodeTray(session.tray)
        prefs[dailyKey(BLOCK_GAME_ID, "score")] = session.score.toString()
        prefs[dailyKey(BLOCK_GAME_ID, "difficulty")] = session.difficulty
        prefs[dailyKey(BLOCK_GAME_ID, "elapsedMs")] = session.elapsedMs.toString()
        prefs[dailyKey(BLOCK_GAME_ID, "progress")] = session.progress.toString()
    }

    suspend fun clearDailyBlock() = edit { prefs ->
        listOf("day", "cells", "tray", "score", "difficulty", "elapsedMs", "progress")
            .forEach { prefs.remove(dailyKey(BLOCK_GAME_ID, it)) }
    }

    // ─── akari session — one game, one slot, same key shape ──────────────────
    //
    // Decoded under AkariCodec: same `session.<id>.*` shape, game-specific value
    // layout. allSessions picks it up under AKARI_GAME_ID, so the resume card needs
    // no other wiring. The board and its blank-wall mask travel as two 100-character
    // strings; the mask is what lets restore verify the layout exactly.

    private fun akariKey(name: String) = stringPreferencesKey("session.$AKARI_GAME_ID.$name")

    fun akariSession(): Flow<AkariSession?> = safeData.map { prefs ->
        decodeAkari(prefs)
    }

    private fun decodeAkari(prefs: Preferences): AkariSession? {
        // Structural validity only — null on malformed. Semantic checks (a layout
        // that never generated, an already-solved board) belong to the game's
        // restore step; see games.akari.AkariRestore.
        val cells = prefs[akariKey("cells")] ?: return null
        val walls = prefs[akariKey("walls")] ?: return null
        // AkariSession's init throws on malformed strings, so the decodes gate first:
        // corrupt input is "no saved run", never a crash on launch.
        AkariCodec.decodeCells(cells) ?: return null
        AkariCodec.decodeWalls(walls) ?: return null
        return AkariSession(
            cells = cells,
            walls = walls,
            difficulty = prefs[akariKey("difficulty")] ?: "Moderate",
            elapsedMs = prefs[akariKey("elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = 0,
            progress = prefs[akariKey("progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveAkariSession(session: AkariSession) = edit { prefs ->
        prefs[akariKey("cells")] = session.cells
        prefs[akariKey("walls")] = session.walls
        prefs[akariKey("difficulty")] = session.difficulty
        prefs[akariKey("elapsedMs")] = session.elapsedMs.toString()
        prefs[akariKey("progress")] = session.progress.toString()
    }

    /** Called on completion, so a finished run is not restored as in-progress. */
    suspend fun clearAkariSession() = edit { prefs ->
        listOf("cells", "walls", "difficulty", "elapsedMs", "progress")
            .forEach { prefs.remove(akariKey(it)) }
    }

    // ─── akari daily (decision D32) ─────────────────────────────────────────

    /** Today's akari daily, or null when none is stored or [day] differs. */
    fun dailyAkari(day: Long): Flow<AkariSession?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(AKARI_GAME_ID)] != day) return@map null
        decodeAkari(prefs)
    }

    suspend fun saveDailyAkari(day: Long, session: AkariSession) = edit { prefs ->
        prefs[dailyDayKey(AKARI_GAME_ID)] = day
        prefs[dailyKey(AKARI_GAME_ID, "cells")] = session.cells
        prefs[dailyKey(AKARI_GAME_ID, "walls")] = session.walls
        prefs[dailyKey(AKARI_GAME_ID, "difficulty")] = session.difficulty
        prefs[dailyKey(AKARI_GAME_ID, "elapsedMs")] = session.elapsedMs.toString()
        prefs[dailyKey(AKARI_GAME_ID, "progress")] = session.progress.toString()
    }

    suspend fun clearDailyAkari() = edit { prefs ->
        listOf("day", "cells", "walls", "difficulty", "elapsedMs", "progress")
            .forEach { prefs.remove(dailyKey(AKARI_GAME_ID, it)) }
    }

    // ─── binairo session — one game, one slot, same key shape ────────────────
    //
    // Decoded under BinairoCodec: same `session.<id>.*` shape, game-specific value
    // layout. allSessions picks it up under BINAIRO_GAME_ID, so the resume card needs
    // no other wiring. The merged board and its given mask travel as two 100-character
    // strings; the mask is what lets restore tell a given from a player entry.

    private fun binairoKey(name: String) = stringPreferencesKey("session.$BINAIRO_GAME_ID.$name")

    fun binairoSession(): Flow<BinairoSession?> = safeData.map { prefs ->
        decodeBinairo(prefs)
    }

    private fun decodeBinairo(prefs: Preferences): BinairoSession? {
        // Structural validity only — null on malformed. Semantic checks (a clue set
        // that never generated, an already-solved board) belong to the game's restore
        // step; see games.binairo.BinairoRestore.
        val cells = prefs[binairoKey("cells")] ?: return null
        val givens = prefs[binairoKey("givens")] ?: return null
        // BinairoSession's init throws on malformed strings, so the decodes gate first:
        // corrupt input is "no saved run", never a crash on launch.
        BinairoCodec.decodeCells(cells) ?: return null
        BinairoCodec.decodeGivens(givens) ?: return null
        return BinairoSession(
            cells = cells,
            givens = givens,
            difficulty = prefs[binairoKey("difficulty")] ?: "Moderate",
            elapsedMs = prefs[binairoKey("elapsedMs")]?.toLongOrNull() ?: 0L,
            mistakes = 0,
            progress = prefs[binairoKey("progress")]?.toFloatOrNull() ?: 0f,
        )
    }

    suspend fun saveBinairoSession(session: BinairoSession) = edit { prefs ->
        prefs[binairoKey("cells")] = session.cells
        prefs[binairoKey("givens")] = session.givens
        prefs[binairoKey("difficulty")] = session.difficulty
        prefs[binairoKey("elapsedMs")] = session.elapsedMs.toString()
        prefs[binairoKey("progress")] = session.progress.toString()
    }

    /** Called on completion, so a finished run is not restored as in-progress. */
    suspend fun clearBinairoSession() = edit { prefs ->
        listOf("cells", "givens", "difficulty", "elapsedMs", "progress")
            .forEach { prefs.remove(binairoKey(it)) }
    }

    // ─── binairo daily (decision D32) ────────────────────────────────────────

    /** Today's binairo daily, or null when none is stored or [day] differs. */
    fun dailyBinairo(day: Long): Flow<BinairoSession?> = safeData.map { prefs ->
        if (prefs[dailyDayKey(BINAIRO_GAME_ID)] != day) return@map null
        decodeBinairo(prefs)
    }

    suspend fun saveDailyBinairo(day: Long, session: BinairoSession) = edit { prefs ->
        prefs[dailyDayKey(BINAIRO_GAME_ID)] = day
        prefs[dailyKey(BINAIRO_GAME_ID, "cells")] = session.cells
        prefs[dailyKey(BINAIRO_GAME_ID, "givens")] = session.givens
        prefs[dailyKey(BINAIRO_GAME_ID, "difficulty")] = session.difficulty
        prefs[dailyKey(BINAIRO_GAME_ID, "elapsedMs")] = session.elapsedMs.toString()
        prefs[dailyKey(BINAIRO_GAME_ID, "progress")] = session.progress.toString()
    }

    suspend fun clearDailyBinairo() = edit { prefs ->
        listOf("day", "cells", "givens", "difficulty", "elapsedMs", "progress")
            .forEach { prefs.remove(dailyKey(BINAIRO_GAME_ID, it)) }
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
            Log.w("SessionStore", "could not write preferences, change not saved", e)
        }
    }

    companion object {
        /** One key per game, so a new game adds a record rather than a schema change. */
        private const val STATS_PREFIX = "stats."
        private const val SESSION_PREFIX = "session."

        /**
         * The label a stored daily session decodes with when its own key is missing
         * (partial corruption that the grid decode already would have rejected — this
         * default exists only so the decode is total). Mirrors how the regular slot
         * defaults to `Moderate`. The daily puzzle is always the game's "Daily"
         * difficulty label; the game's view model is what writes it.
         */
        private const val DAILY_DIFFICULTY = "Daily"

        /**
         * The game id the `sudoku9.` session keys belong to.
         *
         * This is the visible edge of the per-game-session wart documented on this class: the
         * shell needs a game id to attach a saved session to, and the session itself does not
         * carry one because the keys are hardcoded per game. It lives here rather than at the
         * call site because `MainActivity` is in the shared source set and genuinely *cannot*
         * see `SudokuDefinition.ID` — that class is in a flavour-scoped source set.
         *
         * When sessions become keyed by game id, this constant and the `sudokuSession` API
         * above go together.
         */
        const val SUDOKU_GAME_ID = "sudoku"

        /** The game id the `session.nonogram.*` keys belong to. Same wall as above. */
        const val NONOGRAM_GAME_ID = "nonogram"

    /** The game id the `session.minesweeper.*` keys belong to. Same wall as above. */
    const val MINESWEEPER_GAME_ID = "minesweeper"

    /** The game id the `session.connect.*` keys belong to. Same wall as above. */
    const val CONNECT_GAME_ID = "connect"

    /** The game id the `session.wordsearch.*` keys belong to. Same wall as above. */
    const val WORDSEARCH_GAME_ID = "wordsearch"

    /** The game id the `session.block.*` keys belong to. Same wall as above. */
    const val BLOCK_GAME_ID = "block"

    /** The game id the `session.akari.*` keys belong to. Same wall as above. */
    const val AKARI_GAME_ID = "akari"

    /** The game id the `session.binairo.*` keys belong to. Same wall as above. */
    const val BINAIRO_GAME_ID = "binairo"
    }
}
