package com.example.lightapp.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * Motion — Requirement 13, derived in rationale.md §5.
 *
 * Two durations, one easing curve, and nothing else. Both durations are whole frame
 * counts at 60fps (9 and 18 frames), so nothing truncates unevenly into a stutter.
 *
 * The curve is deceleration-dominant, which reads as *arriving and settling*.
 * Ease-in-out reads as travelling; spring reads as playful. The requirement is calm.
 */
object NothingMotion {
    /**
     * Press feedback and state changes. Inside the 100–200ms band where motion is
     * seen but still feels caused by the touch.
     */
    val micro = Duration(MotionTokens.durationMicro)

    /**
     * Screen transitions. A tracked transition that stays clear of the ~400ms
     * threshold at which a transition starts to read as waiting.
     */
    val transition = Duration(MotionTokens.durationTransition)

    /** The only easing curve in the system. */
    val easing: Easing = CubicBezierEasing(
        MotionTokens.easingStandard[0],
        MotionTokens.easingStandard[1],
        MotionTokens.easingStandard[2],
        MotionTokens.easingStandard[3],
    )

    /**
     * Specs for the only two animated properties permitted: opacity and colour.
     *
     * Requirement 13 criterion 3 forbids animating position, size, scale, and
     * rotation, so no spec for those is offered here. Making the wrong thing
     * unavailable beats documenting that it is forbidden.
     */
    fun <T> microSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = micro.millis, easing = easing)

    fun <T> transitionSpec(): FiniteAnimationSpec<T> =
        tween(durationMillis = transition.millis, easing = easing)
}
