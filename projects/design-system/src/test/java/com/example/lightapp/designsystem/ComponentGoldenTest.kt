package com.example.lightapp.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.lightapp.designsystem.components.BlockCell
import com.example.lightapp.designsystem.components.BlockPiece
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.ThemeMode
import com.example.lightapp.designsystem.theme.SurfaceRole
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The screen gate (build-order step 10, decision D39): golden captures of the
 * design system's components at their token-boundary states, rendered on the JVM
 * by Robolectric through Roborazzi — no emulator, no device.
 *
 * A capture is committed. A change that reaches the pixels — a token swapped for
 * another, a density rewritten, a stroke off by a hair — must be a deliberate
 * re-capture in the same commit, or the build fails. That is the whole rule:
 * the review tests in `nothing-study.md` §12 stop being paper and start being
 * diffs.
 *
 * First captures, per `ideas/tooling-research.md` D34 Tier 1: `BlockCell` in both
 * states, `BlockPiece` at all three densities (D33's dot-density ruling — the
 * distinction the language makes instead of colour).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class ComponentGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capture(tag: String, name: String, content: @Composable () -> Unit) {
        composeRule.setContent {
            // Dark pinned: the OLED-black theme is the product's default, and a
            // golden must not move because a test host's system setting did.
            // A bounded canvas, not the bare Row: the capture clips to the tagged
            // node, so this draws the OLED-black page the row sits on plus a
            // fixed padding frame — otherwise the shot has no background at all
            // and row spacing leaks the host's own fill colour into the pixels.
            NothingTheme(mode = ThemeMode.Dark) {
                Column(
                    modifier = Modifier
                        .testTag(tag)
                        .background(NothingTheme.colors.surface(SurfaceRole.Background))
                        .padding(16.dp),
                ) { content() }
            }
        }
        composeRule.onNodeWithTag(tag).captureRoboImage("src/test/snapshots/$name.png")
    }

    @Test
    fun `block cell reads as two surface steps`() {
        capture(
            tag = "blockcell",
            name = "BlockCell_empty_and_filled",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BlockCell(filled = false, modifier = Modifier.size(48.dp))
                BlockCell(filled = true, modifier = Modifier.size(48.dp))
            }
        }
    }

    @Test
    fun `block piece distinguishes itself by dot density alone`() {
        val shape = listOf(
            listOf(1, 1),
            listOf(1, 0),
        )
        capture(
            tag = "blockpiece",
            name = "BlockPiece_solid_halfpitch_hollow",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // Fixed cell width: the piece is fillMaxWidth inside, so an
                // unconstrained Row would give it a zero-width box to fill.
                // 96.dp each: 3 x 96 + 2 x 24 = 336.dp of canvas content.
                BlockPiece(grid = shape, density = 0, modifier = Modifier.size(96.dp))
                BlockPiece(grid = shape, density = 1, modifier = Modifier.size(96.dp))
                BlockPiece(grid = shape, density = 2, modifier = Modifier.size(96.dp))
            }
        }
    }
}