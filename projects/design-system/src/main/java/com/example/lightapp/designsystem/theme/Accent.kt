package com.example.lightapp.designsystem.theme

import androidx.compose.runtime.compositionLocalOf

/**
 * Teenage Engineering accent choice — the optional third accent.
 * "none" = strict Nothing (red is still error-only, normally 0).
 * "sage" / "amber" = muted TE sage/amber used for progress/solved
 * while still normally 0, max 1 element, non-colour paired (C8-C11).
 */
val LocalAccentChoice = compositionLocalOf { "none" }
