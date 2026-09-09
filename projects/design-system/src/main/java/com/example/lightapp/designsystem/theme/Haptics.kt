package com.example.lightapp.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * The haptic channel — decision D15.
 *
 * Haptics carry most of the felt response in this design language, because the two
 * channels an interface normally uses for feedback are both spent: colour is monochrome
 * with red reserved for genuine error, and motion is opacity-only at two durations. A
 * tap has to be *felt* because it is barely allowed to be seen.
 *
 * That makes the "haptics off" setting load-bearing rather than cosmetic, and it is why
 * this goes through a composition local instead of each component reaching for
 * `LocalHapticFeedback` directly. A component that calls the platform API straight cannot
 * be turned off, which is how the setting came to be persisted, rendered in Settings, and
 * silently ignored at all three call sites.
 *
 * ## Why only two physical patterns
 *
 * Compose's `HapticFeedbackType` on this version exposes exactly two constants. So the
 * three semantic events below collapse onto two primitives. The events stay distinct
 * anyway: the call sites express intent, and a future Compose version with a richer
 * palette is then a change to this file alone.
 *
 * Deliberately *not* `Vibrator`/`VibrationEffect`, which would give finer control at the
 * cost of the `VIBRATE` permission. The manifest declares zero permissions and that is
 * not negotiable — see `ideas/avoid.md`. `performHapticFeedback` needs none, and it
 * no-ops cleanly on a device with no vibrator or with system touch feedback switched off.
 */
enum class HapticEvent {
    /** A committed input: a digit placed, a button pressed. The lightest tick. */
    Tick,

    /**
     * The input was accepted but created a conflict. Heavier, so a mistake is felt
     * without spending the screen's single red accent to announce it.
     */
    Conflict,

    /** The puzzle is solved. Heavier, and fires exactly once. */
    Complete,
}

/**
 * Whether haptics are delivered at all. Provided by the shell from the persisted
 * preference; defaults to on so a component used outside the shell still feels right.
 */
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/**
 * Performs [HapticEvent]s, or nothing at all when the user has switched haptics off.
 *
 * Obtain via [rememberNothingHaptics]. Components should never hold
 * `LocalHapticFeedback` themselves.
 */
class NothingHaptics internal constructor(private val delegate: HapticFeedback?) {

    /** No-ops when haptics are disabled, so callers never branch on the setting. */
    fun perform(event: HapticEvent) {
        val feedback = delegate ?: return
        feedback.performHapticFeedback(
            when (event) {
                HapticEvent.Tick -> HapticFeedbackType.TextHandleMove
                HapticEvent.Conflict -> HapticFeedbackType.LongPress
                HapticEvent.Complete -> HapticFeedbackType.LongPress
            }
        )
    }
}

/** The haptics handle for the current composition, honouring [LocalHapticsEnabled]. */
@Composable
fun rememberNothingHaptics(): NothingHaptics {
    val enabled = LocalHapticsEnabled.current
    val feedback = androidx.compose.ui.platform.LocalHapticFeedback.current
    return remember(enabled, feedback) {
        NothingHaptics(if (enabled) feedback else null)
    }
}
