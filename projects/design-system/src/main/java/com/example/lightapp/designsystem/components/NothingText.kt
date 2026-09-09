package com.example.lightapp.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import com.example.lightapp.designsystem.guard.LocalDesignGuard
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.example.lightapp.designsystem.theme.TextRole

/**
 * The text primitive. Every piece of text in every game goes through here.
 *
 * Note the signature: it takes a [TextRole], **not a `Color`**. That is the whole
 * mechanism by which `:app` never needs to reference `Color` — Requirement 15
 * criterion 4. Consumer code names a role and the design system resolves it against
 * the active mode.
 *
 * `style` comes from [NothingTheme.typography] rather than a raw size, so a caller
 * cannot invent a type size either.
 */
@Composable
fun NothingText(
    text: String,
    role: TextRole = TextRole.Primary,
    style: TextStyle = NothingTheme.typography.body,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
) {
    // Debug-only: counts display-level elements so a screen cannot quietly grow a
    // second one. Requirement 9 criterion 7 defines display-level by **type size**,
    // 36sp or larger — not by the colour role.
    //
    // Keying this on `TextRole.Display` instead was a live crash: GridCell uses that
    // role for entered numerals and NothingButton uses it on press, so entering two
    // numbers tripped the guard and killed the debug app.
    val sizeSp = style.fontSize
    if (sizeSp != TextUnit.Unspecified && sizeSp.value >= DISPLAY_LEVEL_SP) {
        LocalDesignGuard.current.onDisplayElement()
    }

    Text(
        text = text,
        style = style,
        color = NothingTheme.colors.text(role),
        textAlign = align,
        modifier = modifier,
    )
}

/**
 * All-caps tracked mono label — the workhorse of this design system.
 *
 * Nothing labels things in words where other interfaces use icons, so this gets used
 * far more than it would elsewhere. Upper-casing happens here rather than at the call
 * site, which makes a lowercase label unexpressible.
 *
 * Defaults to [TextRole.Secondary]: a label is supporting information, and promoting
 * one to primary should be a deliberate act.
 */
@Composable
fun Label(
    text: String,
    role: TextRole = TextRole.Secondary,
    modifier: Modifier = Modifier,
    align: TextAlign? = null,
) {
    NothingText(
        text = if (NothingTheme.typography.labelIsAllCaps) text.uppercase() else text,
        role = role,
        style = NothingTheme.typography.label,
        modifier = modifier,
        align = align,
    )
}

/**
 * The display-level threshold from Requirement 9 criterion 7, and the dot-matrix face
 * floor from criterion 4. The same number governs both, which is not a coincidence:
 * below this size the dot matrix stops resolving, so display type cannot go smaller.
 */
private const val DISPLAY_LEVEL_SP = 36f

/**
 * Full-screen base surface. Exists so a screen cannot forget its background and
 * inherit a Material default.
 */
@Composable
fun NothingBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingTheme.colors.surface(SurfaceRole.Background)),
        content = content,
    )
}
