package com.example.lightapp.sudoku.core

import android.os.Process
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Vendored QQWing driver (GPL-3.0 — see `design-canon/resource-map.md`).
 *
 * Local modifications to the upstream file, all of them about running it safely under a
 * coroutine on a phone rather than as a command-line tool:
 *
 * 1. [generated] is a [ConcurrentLinkedQueue]. Upstream used a plain `LinkedList` written
 *    concurrently by every worker thread, which is an unsynchronised mutation of a
 *    non-thread-safe collection: it can drop an element or corrupt its internal links.
 * 2. [generate] returns `IntArray?`. Upstream declared a non-null return and then handed
 *    back `poll()`, which is null whenever the queue ends up empty — a Kotlin null-check
 *    failure inside a worker-fed path, i.e. a crash that only shows up under load.
 * 3. [abort] exists, and the `done` flag it sets is a field rather than a local. Without
 *    it the worker loop `while (!done.get())` only ever ends on success, so a caller that
 *    timed out or navigated away left threads spinning for the process's lifetime.
 * 4. Thread count is capped — see [threadCount].
 *
 * Kept deliberately close to upstream otherwise, so a future update is still diffable.
 */
class QQWingController {
    val options = QQWingOptions()
    private var level: IntArray? = null
    private var solution: IntArray = IntArray(81)

    /**
     * Written by every worker thread, drained by the caller. Concurrent because the
     * threads are genuinely concurrent — this is the one collection they share.
     */
    private val generated = ConcurrentLinkedQueue<IntArray>()

    /**
     * Set to stop every worker at its next loop check. Shared by the threads started in
     * [doAction], so it has to outlive that call.
     */
    private val done = AtomicBoolean(false)

    var isImpossible = false
        private set

    @Volatile
    var solutionCount = 0

    /**
     * Generates one puzzle at [difficulty], or `null` if it could not.
     *
     * When [seed] is given the run is deterministic: every worker board is seeded with
     * it, and only one worker runs. Several workers would each need their own stream,
     * which is fine, but a shared seeded stream would interleave draws across threads
     * and the outcome would depend on scheduling — the opposite of what a seed is for.
     * One worker keeps the single-stream order, which is what makes "same seed, same
     * puzzle" hold. The daily puzzle is the only seeded caller, and it generates at a
     * difficulty that lands on the first try, so the lost parallelism costs nothing.
     *
     * Blocking, and the block is uninterruptible from the outside — it joins its worker
     * threads. Callers must run it off the main thread and use [abort] to stop it early;
     * cancelling the surrounding coroutine alone will not.
     */
    fun generate(
        type: GameType,
        difficulty: GameDifficulty,
        seed: Int? = null,
    ): IntArray? {
        generated.clear()
        done.set(false)
        options.gameDifficulty = difficulty
        options.action = Action.GENERATE
        options.needNow = true
        options.printSolution = false
        options.threads = if (seed != null) 1 else threadCount()
        options.gameType = type
        options.seeded = seed != null
        options.seed = seed ?: 0
        doAction()
        return generated.poll()
    }

    /**
     * Stops generation as soon as each worker notices.
     *
     * Generation is a rejection loop: a worker builds a puzzle, grades it, and throws it
     * away if the grade does not match the request. There is no iteration bound, so
     * "when will it finish" has no answer — only a probability. That makes an explicit
     * stop the only way a timeout or a back press can actually end the work instead of
     * leaving it running behind a screen the user has left.
     */
    fun abort() {
        done.set(true)
    }

    /**
     * At most four workers.
     *
     * Upstream used every available core. More threads do find a matching puzzle sooner,
     * but each one runs a full solver, and the reference machine in AGENTS.md is a 2-core
     * laptop while the target is a phone that is also compositing the UI. Saturating every
     * core to shave a few hundred milliseconds off a one-time generation is the wrong
     * trade — it makes the "generating" frame itself janky.
     */
    private fun threadCount(): Int =
        Runtime.getRuntime().availableProcessors().coerceIn(1, MAX_THREADS)

    // `generateMultiple` and `generateFromSeed` were removed. Both were unreachable —
    // nothing in the app has ever called either — and both carried the same declared
    // non-null return over a nullable `poll()` described above. `generateFromSeed`
    // additionally drove the generator through QQWing's shared mutable statics, which is
    // only safe single-threaded. Unreachable code that cannot be exercised cannot be
    // trusted, so it is gone rather than nominally fixed.

    fun solve(gameBoard: IntArray?, gameType: GameType): IntArray {
        isImpossible = false
        done.set(false)
        level = gameBoard
        options.needNow = true
        options.action = Action.SOLVE
        options.printSolution = true
        options.threads = 1
        options.gameType = gameType
        doAction()
        return solution
    }

    private fun doAction() {
        // The number of puzzles solved or generated.
        val puzzleCount = AtomicInteger(0)
        val threads = arrayOfNulls<Thread>(options.threads)
        for (threadCount in threads.indices) {
            threads[threadCount] = Thread(
                object : Runnable {
                    // Create a new puzzle board and set the options
                    private val qqWing = createQQWing()
                    private fun createQQWing(): QQWing {
                        val ss = QQWing(options.gameType, options.gameDifficulty)
                        if (options.seeded) ss.setRandom(options.seed)
                        ss.setRecordHistory(options.printHistory || options.printInstructions || options.printStats || options.gameDifficulty !== GameDifficulty.Unspecified)
                        ss.setLogHistory(options.logHistory)
                        ss.setPrintStyle(options.printStyle)
                        return ss
                    }

                    override fun run() {
                        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                        try {
                            // Solve puzzle or generate puzzles
                            // until end of input for solving, or
                            // until we have generated the specified number.
                            while (!done.get()) {

                                // Record whether the puzzle was possible or not,
                                // so that we don't try to solve impossible givens.
                                var havePuzzle = false
                                if (options.action == Action.GENERATE) {
                                    // Generate a puzzle
                                    havePuzzle = qqWing.generatePuzzleSymmetry(options.symmetry)
                                } else {
                                    // Read the next puzzle on STDIN
                                    var puzzle: IntArray? = IntArray(QQWing.BOARD_SIZE)
                                    if (getPuzzleToSolve(puzzle)) {
                                        havePuzzle = qqWing.setPuzzle(puzzle)
                                        if (havePuzzle) {
                                            puzzleCount.getAndDecrement()
                                        } else {
                                            // Puzzle to solve is impossible.
                                            isImpossible = true
                                        }
                                    } else {
                                        // Set loop to terminate when nothing is
                                        // left on STDIN
                                        havePuzzle = false
                                        done.set(true)
                                    }
                                    puzzle = null
                                }

                                if (havePuzzle) {

                                    solutionCount = qqWing.countSolutionsLimited()

                                    // Solve the puzzle
                                    if (options.printSolution || options.printHistory || options.printStats || options.printInstructions || options.gameDifficulty !== GameDifficulty.Unspecified) {
                                        qqWing.solve()
                                        solution = qqWing.solution
                                    }

                                    // Bail out if it didn't meet the difficulty
                                    // standards for generation
                                    if (options.action == Action.GENERATE) {
                                        if (options.gameDifficulty != GameDifficulty.Unspecified && options.gameDifficulty != qqWing.getDifficulty()) {
                                            havePuzzle = false
                                            // check if other threads have
                                            // finished the job
                                            if (puzzleCount.get() >= options.numberToGenerate) {
                                                done.set(true)
                                            }
                                        } else {
                                            val numDone = puzzleCount.incrementAndGet()
                                            if (numDone >= options.numberToGenerate) done.set(true)
                                            if (numDone > options.numberToGenerate) havePuzzle =
                                                false
                                        }
                                    }
                                    if (havePuzzle) {
                                        generated.add(qqWing.puzzle)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("QQWing", "Exception Occured", e)
                            return
                        }
                    }
                }
            )
            threads[threadCount]!!.apply {
                // Named so a stuck generator is identifiable in a thread dump, and
                // daemon so a worker that outlives its caller can never be the reason
                // the process stays alive.
                name = "qqwing-$threadCount"
                isDaemon = true
                start()
            }
        }
        if (options.needNow) {
            for (i in threads.indices) {
                try {
                    threads[i]!!.join()
                } catch (e: InterruptedException) {
                    // Re-assert the flag the interrupt was trying to communicate, then
                    // restore it for the caller. Upstream printed the trace and carried
                    // on joining, which silently discarded the cancellation.
                    done.set(true)
                    Thread.currentThread().interrupt()
                    return
                }
            }
        }
    }

    class QQWingOptions {
        // defaults for options
        var needNow = false
        var printPuzzle = false
        var printSolution = false
        var printHistory = false
        var printInstructions = false
        var timer = false
        var countSolutions = false
        var action = Action.NONE
        var logHistory = false
        var printStyle = PrintStyle.READABLE
        var numberToGenerate = 1
        var printStats = false
        var gameDifficulty = GameDifficulty.Unspecified
        var gameType = GameType.Unspecified
        var symmetry = Symmetry.NONE
        var threads = Runtime.getRuntime().availableProcessors()

        /**
         * When [seeded], every worker board is created with [seed] and [threads] must
         * already be 1 — see [QQWingController.generate].
         */
        var seeded = false
        var seed = 0
    }

    private fun getPuzzleToSolve(puzzle: IntArray?): Boolean {
        if (level != null) {
            if (puzzle!!.size == level!!.size) {
                for (i in level!!.indices) {
                    puzzle[i] = level!![i]
                }
            }
            level = null
            return true
        }
        return false
    }

    companion object {
        /** See [threadCount]. */
        private const val MAX_THREADS = 4
    }
}