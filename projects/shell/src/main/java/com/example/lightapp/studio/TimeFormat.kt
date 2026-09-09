package com.example.lightapp.studio

/**
 * Elapsed time as `mm:ss`, or `h:mm:ss` once it passes an hour.
 *
 * One copy, in `studio` so every screen and every game can reach it. There were three
 * near-identical private versions of this — the game's timer, the stats records, and then the
 * resume card would have been a fourth — and they had already drifted: two of them truncated
 * anything past 59:59 into a wrong-but-plausible reading rather than showing hours.
 *
 * Hours appear only when there are hours. A sudoku that reads `1:04:12` is unusual but real;
 * one that reads `0:04:12` every time is padding the display for a case that rarely happens.
 *
 * Every style this is rendered in carries tabular figures (rule T5), so the digits do not
 * shift width as the clock advances.
 */
fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
