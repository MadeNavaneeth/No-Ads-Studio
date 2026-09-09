package com.example.lightapp.designsystem.theme

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * Requirement 18 — Token Set Self-Validation.
 *
 * Validates the token set as a set of properties, not as used by any screen.
 * Every other enforcement layer assumes these tokens are correct and will
 * faithfully propagate a bad value everywhere. This is the layer that stops that.
 *
 * Derivations for every threshold asserted here: design-canon/rationale.md
 */
class TokenValidationTest {

    private val tokens = TOKEN_MANIFEST
    private fun ofCategory(c: String) = tokens.filter { it.category == c }

    private fun dpOf(v: String) = v.removeSuffix("dp").toDouble()
    private fun msOf(v: String) = v.removeSuffix("ms").toInt()

    // ── R18.2 — grid alignment ──────────────────────────────────────────────
    // 4dp is the smallest step that is a whole pixel at every density bucket.
    // rationale.md §1
    @Test
    fun `every spacing token is divisible by four, except the optical token`() {
        val offenders = ofCategory("spacing step")
            .filter { it.name != "optical2" }
            .filter { dpOf(it.value!!) % 4.0 != 0.0 }
            .map { "${it.name}=${it.value}" }
        assertTrue("Spacing not on the 4dp grid: $offenders", offenders.isEmpty())
    }

    // ── R18.3 — scale monotonicity and perceptual separation ────────────────
    // Below ~1.3x the eye cannot tell an intentional change from a mistake.
    @Test
    fun `spacing scale is strictly increasing with adjacent ratios of at least 1_3`() {
        val scale = ofCategory("spacing step")
            .filter { it.name != "optical2" }
            .map { dpOf(it.value!!) }
            .sorted()

        assertTrue("Spacing scale must hold at least two steps", scale.size >= 2)
        assertTrue("Spacing scale must be strictly increasing", scale.zipWithNext().all { it.first < it.second })

        val worst = scale.zipWithNext().minOf { it.second / it.first }
        assertTrue("Smallest adjacent spacing ratio is $worst, needs >= 1.3", worst >= 1.3)
    }

    // ── R18.4 — radius ceiling ──────────────────────────────────────────────
    // A card radius must stay below its content padding or the arc eats content.
    @Test
    fun `no corner radius exceeds 16dp except the pill token`() {
        val offenders = ofCategory("corner radius")
            .filter { it.name != "radiusPill" }
            .filter { dpOf(it.value!!) > 16.0 }
            .map { "${it.name}=${it.value}" }
        assertTrue("Corner radius above 16dp: $offenders", offenders.isEmpty())
    }

    // ── R18.5 — frame alignment ─────────────────────────────────────────────
    // Durations off a frame boundary truncate unevenly and show as a stutter.
    @Test
    fun `every motion duration is a whole frame count at 60fps`() {
        val frame = 1000.0 / 60.0
        val offenders = ofCategory("motion duration").mapNotNull {
            val frames = msOf(it.value!!) / frame
            if (abs(frames - round(frames)) > 0.06) "${it.name}=${it.value} (${"%.2f".format(frames)} frames)" else null
        }
        assertTrue("Motion durations not frame-aligned: $offenders", offenders.isEmpty())
    }

    // ── R18.6 — contrast minimums ───────────────────────────────────────────
    @Test
    fun `every text token clears its contrast floor on every surface in both modes`() {
        val textLevels = listOf("textDisplay", "textPrimary", "textSecondary", "textDisabled")
        val surfaces = listOf("bg", "surface", "surfaceRaised")
        val failures = mutableListOf<String>()

        for (mode in listOf(Mode.DARK, Mode.LIGHT)) {
            for (t in textLevels) {
                val floor = if (t == "textDisabled") 3.0 else 4.5
                for (s in surfaces) {
                    val ratio = contrast(hex(t, mode), hex(s, mode))
                    if (ratio < floor) {
                        failures += "$mode $t on $s = ${"%.2f".format(ratio)}:1, needs $floor:1"
                    }
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    // ── R18.7 — the one contrast MAXIMUM ────────────────────────────────────
    // The dot texture must stay below the threshold of reading as content,
    // or it competes with the grid. rationale.md §7
    @Test
    fun `the visible border token stays below 2 to 1 against its background`() {
        for (mode in listOf(Mode.DARK, Mode.LIGHT)) {
            val ratio = contrast(hex("borderVisible", mode), hex("bg", mode))
            assertTrue(
                "$mode borderVisible on bg = ${"%.2f".format(ratio)}:1, must stay below 2:1 " +
                    "so the dot texture cannot read as content",
                ratio < 2.0
            )
        }
    }

    // ── R18.8 — red is never text ───────────────────────────────────────────
    // accentRed measures below 4.5:1 on every surface, so it cannot carry text.
    @Test
    fun `no type style declares the red accent`() {
        val offenders = ofCategory("type style")
            .filter { (it.value ?: "").contains("D71921", ignoreCase = true) }
            .map { it.name }
        assertTrue("Red accent used in a type style: $offenders", offenders.isEmpty())
    }

    @Test
    fun `the red accent would fail as red text, which is why red is never a type style`() {
        // Guards the reasoning, not just the outcome: red as a *type colour* fails on
        // every surface, so red is never a type style. A red *shape fill* is a different
        // pairing — white textDisplay over #D71921 measures 5.19:1 — which is what the
        // whole-fill conflict cell uses (rationale.md §8, Requirement 14 criterion 7).
        val red = hex("accentRed", Mode.DARK)
        val worst = listOf("bg", "surface", "surfaceRaised").minOf { contrast(red, hex(it, Mode.DARK)) }
        assertTrue(
            "accentRed now clears 4.5:1 (worst ${"%.2f".format(worst)}:1). " +
                "Revisit the red-as-text rule in rationale.md §8 and Requirement 14.",
            worst < 4.5
        )
    }

    // ── R18.9 — dot-matrix face floor ───────────────────────────────────────
    // Doto builds glyphs on a 6x10 dot grid; below 36sp the dots merge or vanish.
    @Test
    fun `no type style below 36sp uses the dot-matrix face`() {
        val offenders = ofCategory("type style").mapNotNull { t ->
            val v = t.value ?: return@mapNotNull null
            val family = Regex("\"family\"\\s*:\\s*\"([^\"]+)\"").find(v)?.groupValues?.get(1)
            val size = Regex("\"size\"\\s*:\\s*\"(\\d+)sp\"").find(v)?.groupValues?.get(1)?.toInt()
            if (family == "Doto" && size != null && size < 36) "${t.name}=${size}sp" else null
        }
        assertTrue("Dot-matrix face below its 36sp floor: $offenders", offenders.isEmpty())
    }

    // ── R18.10 — tabular figures on counting styles ──────────────────────────
    // Proportional digits reflow as a value updates, so a timer visibly jitters.
    @Test
    fun `every counting type style enables tabular figures`() {
        val counting = setOf("data", "cellNumeral", "displayLg", "displayMd", "displaySm")
        val offenders = ofCategory("type style")
            .filter { it.name in counting }
            .filter { !(it.value ?: "").contains("\"tabularFigures\":true") }
            .map { it.name }
        assertTrue("Counting styles without tabular figures: $offenders", offenders.isEmpty())
    }

    // ── R18.11 — no redundancy ──────────────────────────────────────────────
    // Two tokens with one value means one of them has no job.
    @Test
    fun `no two tokens in a category share a value`() {
        val failures = mutableListOf<String>()
        tokens.groupBy { it.category }.forEach { (category, group) ->
            group.groupBy { listOf(it.value, it.dark, it.light).toString() }
                .filterValues { it.size > 1 }
                .forEach { (value, dupes) ->
                    failures += "$category: ${dupes.map { it.name }} all hold $value"
                }
        }
        assertTrue(
            "Duplicate values mean a redundant token:\n" + failures.joinToString("\n"),
            failures.isEmpty()
        )
    }

    // ── R18.12 — every token carries a job and a derivation ─────────────────
    // The strongest governance rule: no token exists without a stated reason.
    @Test
    fun `every token states a job and a why`() {
        val offenders = tokens.filter { it.job.isBlank() || it.why.isBlank() }.map { it.name }
        assertTrue("Tokens missing job or why: $offenders", offenders.isEmpty())
    }

    @Test
    fun `every token declares either a single value or both modes, never both forms`() {
        val offenders = tokens.mapNotNull {
            val single = it.value != null
            val modes = it.dark != null || it.light != null
            when {
                single && modes -> "${it.name}: declares value AND dark/light"
                !single && !modes -> "${it.name}: declares neither"
                modes && (it.dark == null || it.light == null) -> "${it.name}: only one mode"
                else -> null
            }
        }
        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
    }

    // ── S10 / S12 — the motion vocabulary is closed ─────────────────────────
    // Two durations and one curve. Both rules were listed as automatically enforced
    // and neither was, so a third duration could be added without anything objecting.
    // A closed vocabulary is the only reason motion reads as one system rather than
    // as a pile of individual decisions.
    @Test
    fun `exactly two motion durations exist`() {
        val durations = ofCategory("motion duration").map { "${it.name}=${it.value}" }
        assertTrue(
            "Motion is micro and transition, nothing else. Found: $durations",
            durations.size == 2
        )
    }

    @Test
    fun `exactly one easing curve exists`() {
        val easings = ofCategory("easing").map { "${it.name}=${it.value}" }
        assertTrue(
            "One deceleration-dominant curve, so motion cannot drift into a second " +
                "personality. Found: $easings",
            easings.size == 1
        )
    }

    @Test
    fun `the manifest is not empty`() {
        // Catches a generator that silently produced nothing.
        assertTrue("Token manifest is empty — did generateTokens run?", tokens.size >= 20)
    }

    // ── WCAG contrast, computed rather than asserted ─────────────────────────

    private enum class Mode { DARK, LIGHT }

    private fun hex(name: String, mode: Mode): String {
        val t = tokens.firstOrNull { it.name == name } ?: fail("Unknown token: $name").let { error("") }
        return when {
            t.value != null -> t.value.trim().split(" ")[0]
            mode == Mode.DARK -> t.dark!!.trim().split(" ")[0]
            else -> t.light!!.trim().split(" ")[0]
        }
    }

    private fun relativeLuminance(hex: String): Double {
        val h = hex.removePrefix("#")
        fun channel(i: Int): Double {
            val c = h.substring(i, i + 2).toInt(16) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(0) + 0.7152 * channel(2) + 0.0722 * channel(4)
    }

    private fun contrast(a: String, b: String): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }
}
