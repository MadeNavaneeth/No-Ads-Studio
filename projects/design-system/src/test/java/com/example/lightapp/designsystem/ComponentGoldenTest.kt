package com.example.lightapp.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.lightapp.designsystem.components.AkariCell
import com.example.lightapp.designsystem.components.AkariCellValue
import com.example.lightapp.designsystem.components.BinairoCell
import com.example.lightapp.designsystem.components.BinairoCellValue
import com.example.lightapp.designsystem.components.BlockCell
import com.example.lightapp.designsystem.components.BlockPiece
import com.example.lightapp.designsystem.components.ButtonEmphasis
import com.example.lightapp.designsystem.components.CellState
import com.example.lightapp.designsystem.components.ConnectCell
import com.example.lightapp.designsystem.components.ConnectCellValue
import com.example.lightapp.designsystem.components.ConnectSide
import com.example.lightapp.designsystem.components.DifficultyPicker
import com.example.lightapp.designsystem.components.DotMatrixReadout
import com.example.lightapp.designsystem.components.DotPulse
import com.example.lightapp.designsystem.components.GameIcon
import com.example.lightapp.designsystem.components.GridCell
import com.example.lightapp.designsystem.components.Label
import com.example.lightapp.designsystem.components.MineCellValue
import com.example.lightapp.designsystem.components.MinesweeperCell
import com.example.lightapp.designsystem.components.NonoCellValue
import com.example.lightapp.designsystem.components.NonogramCell
import com.example.lightapp.designsystem.components.NothingBottomNav
import com.example.lightapp.designsystem.components.NothingButton
import com.example.lightapp.designsystem.components.NothingCard
import com.example.lightapp.designsystem.components.NothingText
import com.example.lightapp.designsystem.components.NothingTopBar
import com.example.lightapp.designsystem.components.NumberPad
import com.example.lightapp.designsystem.components.SlideDotIndicator
import com.example.lightapp.designsystem.components.WordCellValue
import com.example.lightapp.designsystem.components.WordsearchCell
import com.example.lightapp.designsystem.theme.NothingTheme
import com.example.lightapp.designsystem.theme.TextRole
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
 *
 * Second tier: the `Label`/text roles, `NothingButton` in its two emphases plus
 * the disabled and selected states, `NothingCard` with real text content,
 * `GridCell` across the given/entered/conflict states plus the selection ring,
 * `NumberPad` with remaining-count badges, and `DotMatrixReadout` at two
 * progress levels.
 *
 * Third tier: the chrome and the Glyph small forms — `NothingTopBar` with back,
 * title and action words, `NothingBottomNav` with a selected word over dimmed,
 * `DifficultyPicker` with its bright current over subtle options, `DotPulse`
 * mid-breath, `SlideDotIndicator` marking its index, and `GameIcon` at the 5x5
 * ceiling.
 *
 * Fourth tier: the six game cells — `AkariCell` across void, dark, lit, bulb
 * and clue; `BinairoCell` as solid dot, hollow ring and undecided ground with
 * the given hairline; `ConnectCell` as endpoint numeral, laid path and empty
 * ground with a channel run; `MinesweeperCell` as covered, counted, flagged
 * and the studio's one live red detonation; `NonogramCell` as filled ground,
 * marked dot and the peer ring; `WordsearchCell` as hunting letter, found word
 * and the live trace. Each golden pins the token-boundary states named in
 * `design-canon/component-specs.md`.
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

    @Test
    fun `labels read as capped tracked mono in three roles`() {
        capture(
            tag = "label",
            name = "Label_secondary_primary_disabled",
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Label(text = "difficulty", role = TextRole.Secondary)
                Label(text = "difficulty", role = TextRole.Primary)
                Label(text = "difficulty", role = TextRole.Disabled)
            }
        }
    }

    @Test
    fun `readout text roles make the instrument hierarchy`() {
        capture(
            tag = "readouttext",
            name = "NothingText_display_data_body",
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                NothingText(
                    text = "12:34",
                    role = TextRole.Display,
                    style = NothingTheme.typography.data,
                )
                NothingText(
                    text = "12:34",
                    role = TextRole.Secondary,
                    style = NothingTheme.typography.data,
                )
                NothingText(
                    text = "Place the digits",
                    role = TextRole.Primary,
                    style = NothingTheme.typography.body,
                )
            }
        }
    }

    @Test
    fun `buttons pin both emphases plus disabled and selected`() {
        capture(
            tag = "button",
            name = "NothingButton_normal_quiet_disabled_selected",
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Fixed width so each pill draws the same drawn bounds — the
                // height is the component's own 48dp touch-target floor.
                val fixed = Modifier.width(220.dp)
                NothingButton(text = "new game", onClick = {}, modifier = fixed)
                NothingButton(
                    text = "cancel",
                    onClick = {},
                    emphasis = ButtonEmphasis.Quiet,
                    // Quiet sits on bg — a surface bench behind it, the way the
                    // picker sits on the screen plane, keeps it readable.
                    modifier = Modifier
                        .width(220.dp)
                        .background(NothingTheme.colors.surface(SurfaceRole.Surface)),
                )
                NothingButton(text = "new game", onClick = {}, enabled = false, modifier = fixed)
                NothingButton(text = "notes", onClick = {}, selected = true, modifier = fixed)
            }
        }
    }

    @Test
    fun `card is borderless surface with real text content`() {
        capture(
            tag = "card",
            name = "NothingCard_text_content",
        ) {
            // Fixed width: the card is fillMaxWidth, so the capture canvas
            // bounds it the way a screen would.
            NothingCard(modifier = Modifier.width(300.dp)) {
                Label(text = "daily", role = TextRole.Secondary)
                NothingText(
                    text = "Sudoku · Moderate",
                    role = TextRole.Primary,
                    style = NothingTheme.typography.body,
                )
                NothingText(
                    text = "Best 04:12",
                    role = TextRole.Secondary,
                    style = NothingTheme.typography.data,
                )
            }
        }
    }

    @Test
    fun `grid cell pins given entered conflict and selection`() {
        capture(
            tag = "gridcell",
            name = "GridCell_given_entered_conflict_selected",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Aspect-ratio cells need a bounded box: this is the grid's
                // width doing its job, at the scale a phone gives one cell.
                GridCell(value = "5", state = CellState.Given, modifier = Modifier.size(48.dp))
                GridCell(value = "3", state = CellState.Entered, modifier = Modifier.size(48.dp))
                GridCell(
                    value = "3",
                    state = CellState.ConflictAccented,
                    modifier = Modifier.size(48.dp),
                )
                GridCell(value = "7", state = CellState.Entered, selected = true, modifier = Modifier.size(48.dp))
            }
        }
    }

    @Test
    fun `number pad pins badges exhausted digit and notes`() {
        capture(
            tag = "numberpad",
            name = "NumberPad_badges_exhausted_notes",
        ) {
            // The pad is fillMaxWidth; the canvas bounds it like a phone row.
            // A full remaining map shows the badge idiom; 9 exhausted at zero
            // shows the disabled digit; notesActive lights the action row.
            NumberPad(
                onDigit = {},
                onErase = {},
                onToggleNotes = {},
                onUndo = {},
                notesActive = true,
                remaining = mapOf(1 to 9, 2 to 5, 3 to 2, 4 to 7, 5 to 1, 6 to 4, 7 to 8, 8 to 3, 9 to 0),
                modifier = Modifier.width(340.dp),
            )
        }
    }

    @Test
    fun `glyph readout rises with progress`() {
        capture(
            tag = "readout",
            name = "DotMatrixReadout_progress_empty_half",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.size(120.dp)) {
                    DotMatrixReadout(progress = 0f)
                }
                Box(modifier = Modifier.size(120.dp)) {
                    DotMatrixReadout(progress = 0.5f)
                }
            }
        }
    }

    @Test
    fun `top bar pins words not icons`() {
        capture(
            tag = "topbar",
            name = "NothingTopBar_back_title_action",
        ) {
            // fillMaxWidth bar; the canvas bounds it the way a screen would.
            NothingTopBar(
                title = "sudoku",
                onBack = {},
                actionLabel = "new",
                onAction = {},
                modifier = Modifier.width(360.dp),
            )
        }
    }

    @Test
    fun `bottom nav pins selected word over dimmed`() {
        capture(
            tag = "bottomnav",
            name = "NothingBottomNav_selected_dimmed",
        ) {
            NothingBottomNav(
                items = listOf("play", "stats", "settings"),
                selected = "play",
                onSelect = {},
                modifier = Modifier.width(360.dp),
            )
        }
    }

    @Test
    fun `picker pins bright current over subtle options`() {
        capture(
            tag = "picker",
            name = "DifficultyPicker_current_bright",
        ) {
            DifficultyPicker(
                options = listOf("simple", "moderate", "hard"),
                current = "moderate",
                onSelect = {},
                onCancel = {},
                modifier = Modifier.width(320.dp),
            )
        }
    }

    @Test
    fun `pulse is a three dot breath not a spinner`() {
        capture(
            tag = "pulse",
            name = "DotPulse_three_dots",
        ) {
            // Centered with breathing room: the pulse is small by design.
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                DotPulse()
            }
        }
    }

    @Test
    fun `slide dots mark where not how much`() {
        capture(
            tag = "slidedots",
            name = "SlideDotIndicator_current_marked",
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                SlideDotIndicator(total = 4, current = 1)
            }
        }
    }

    @Test
    fun `game icon is canvas dots no asset`() {
        capture(
            tag = "gameicon",
            name = "GameIcon_dotmatrix",
        ) {
            // A 5x5 S-curve: the practical ceiling at 24dp, per the spec.
            val rows = listOf(
                listOf(false, true, true, true, false),
                listOf(true, false, false, false, false),
                listOf(false, true, true, true, false),
                listOf(false, false, false, false, true),
                listOf(false, true, true, true, false),
            )
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(96.dp),
                contentAlignment = Alignment.Center,
            ) {
                GameIcon(rows = rows)
            }
        }
    }
@Test
    fun `akari cell reads void raised and clue`() {
        capture(
            tag = "akaricell",
            name = "AkariCell_dark_lit_bulb_wall_clue",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AkariCell(value = AkariCellValue.Dark, clue = 0, onTap = {}, modifier = Modifier.size(48.dp))
                AkariCell(value = AkariCellValue.Lit, clue = 0, onTap = {}, modifier = Modifier.size(48.dp))
                AkariCell(value = AkariCellValue.Bulb, clue = 0, onTap = {}, modifier = Modifier.size(48.dp))
                AkariCell(value = AkariCellValue.Wall, clue = 0, onTap = {}, modifier = Modifier.size(48.dp))
                AkariCell(value = AkariCellValue.Clue, clue = 3, onTap = {}, modifier = Modifier.size(48.dp))
            }
        }
    }

    @Test
    fun `binairo cell pins solid ring and given hairline`() {
        capture(
            tag = "binairocell",
            name = "BinairoCell_undecided_one_zero_given",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BinairoCell(value = BinairoCellValue.Undecided, given = false, onTap = {}, modifier = Modifier.size(48.dp))
                BinairoCell(value = BinairoCellValue.One, given = false, onTap = {}, modifier = Modifier.size(48.dp))
                BinairoCell(value = BinairoCellValue.Zero, given = false, onTap = {}, modifier = Modifier.size(48.dp))
                BinairoCell(value = BinairoCellValue.One, given = true, onTap = {}, modifier = Modifier.size(48.dp))
                BinairoCell(value = BinairoCellValue.Zero, given = true, onTap = {}, modifier = Modifier.size(48.dp))
            }
        }
    }

    @Test
    fun `connect cell pins path channel and endpoint numeral`() {
        capture(
            tag = "connectcell",
            name = "ConnectCell_endpoint_path_empty_selected",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ConnectCell(
                    value = ConnectCellValue.Empty,
                    pair = null,
                    connections = emptySet(),
                    selected = false,
                    onTap = {},
                    modifier = Modifier.size(48.dp),
                )
                ConnectCell(
                    value = ConnectCellValue.Path,
                    pair = null,
                    connections = setOf(ConnectSide.East, ConnectSide.West),
                    selected = false,
                    onTap = {},
                    modifier = Modifier.size(48.dp),
                )
                // The live walk head — the selected path cell.
                ConnectCell(
                    value = ConnectCellValue.Path,
                    pair = null,
                    connections = setOf(ConnectSide.South, ConnectSide.West),
                    selected = true,
                    onTap = {},
                    modifier = Modifier.size(48.dp),
                )
                ConnectCell(
                    value = ConnectCellValue.Endpoint,
                    pair = 4,
                    connections = setOf(ConnectSide.South, ConnectSide.West),
                    selected = false,
                    onTap = {},
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }
@Test
    fun `minesweeper pins covered counted flagged and the one red`() {
        capture(
            tag = "minesweepercell",
            name = "MinesweeperCell_unknown_revealed_flagged_detonated",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MinesweeperCell(
                    value = MineCellValue.Unknown,
                    count = 0,
                    selected = false,
                    onTap = {},
                    onLongPress = {},
                    modifier = Modifier.size(48.dp),
                )
                MinesweeperCell(
                    value = MineCellValue.Revealed,
                    count = 2,
                    selected = false,
                    onTap = {},
                    onLongPress = {},
                    modifier = Modifier.size(48.dp),
                )
                MinesweeperCell(
                    value = MineCellValue.Flagged,
                    count = 0,
                    selected = false,
                    onTap = {},
                    onLongPress = {},
                    modifier = Modifier.size(48.dp),
                )
                // The studio's one live red — a detonation, and only there.
                MinesweeperCell(
                    value = MineCellValue.Detonated,
                    count = 0,
                    selected = false,
                    onTap = {},
                    onLongPress = {},
                    modifier = Modifier.size(48.dp),
                )
            }
        }
    }

    @Test
    fun `nonogram cell pins filled marked and the peer ring`() {
        capture(
            tag = "nonogramcell",
            name = "NonogramCell_unknown_filled_marked_peer",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NonogramCell(value = NonoCellValue.Unknown, peer = false, selected = false, onTap = {}, onLongPress = {}, modifier = Modifier.size(48.dp))
                NonogramCell(value = NonoCellValue.Filled, peer = false, selected = false, onTap = {}, onLongPress = {}, modifier = Modifier.size(48.dp))
                NonogramCell(value = NonoCellValue.Marked, peer = false, selected = false, onTap = {}, onLongPress = {}, modifier = Modifier.size(48.dp))
                NonogramCell(value = NonoCellValue.Unknown, peer = true, selected = false, onTap = {}, onLongPress = {}, modifier = Modifier.size(48.dp))
            }
        }
    }

    @Test
    fun `wordsearch pins hunting found and the live trace`() {
        capture(
            tag = "wordsearchcell",
            name = "WordsearchCell_hidden_found_trace",
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                WordsearchCell(letter = 'A', value = WordCellValue.Hidden, selected = false, modifier = Modifier.size(48.dp))
                WordsearchCell(letter = 'A', value = WordCellValue.Found, selected = false, modifier = Modifier.size(48.dp))
                WordsearchCell(letter = 'A', value = WordCellValue.Hidden, selected = true, modifier = Modifier.size(48.dp))
            }
        }
    }
}