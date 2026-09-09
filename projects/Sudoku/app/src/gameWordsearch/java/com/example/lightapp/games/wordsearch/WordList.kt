package com.example.lightapp.games.wordsearch

/**
 * The word pool for `wordsearch(12)` — the studio's **first bundled data**, and the
 * reason the 512 KB embedded-data budget (roadmap §3) finally gets an entry in the
 * accounting. This list is a few kilobytes of source; the budget was never in doubt,
 * but now it is spent *somewhere* rather than nowhere.
 *
 * Curation rules, because a word grid is a reading surface:
 *
 * - common enough that a 12×12 hunt stays fair (no proper nouns, no slang, nothing
 *   a dictionary-obsessed generator would love but a player would resent),
 * - three to eight letters — three because a diagonal gem needs somewhere to hide,
 *   eight because the grid is only 12 across,
 * - no two words that make the pool feel like a theme the player can guess and
 *   then ride — variety is the point of a pool this size,
 * - safe for a general-audience, family-shelf product.
 */
object WordList {

    val words: List<String> = listOf(
        "ANCHOR", "ARROW", "BANGLE", "BEACON", "BLOSSOM", "BREEZE", "BRIDGE",
        "CANDLE", "CANYON", "CELLAR", "CHARM", "CINDER", "CLOVER", "COMPASS",
        "CORAL", "CRADLE", "CRESCENT", "CRYSTAL", "DAWN", "DELTA", "DOMINO",
        "DRIFTWOOD", "EMBER", "FALCON", "FERN", "FLINT", "FOSSIL", "FOUNTAIN",
        "GARNET", "GLACIER", "GROVE", "HARBOR", "HARVEST", "HOLLOW", "IVORY",
        "JASPER", "JUNGLE", "LANTERN", "LATTICE", "LEDGE", "LOTUS", "LUMEN",
        "MARBLE", "MEADOW", "MIRAGE", "MOSAIC", "NEBULA", "NEEDLE", "OASIS",
        "OBSIDIAN", "ORCHARD", "PASTEL", "PATTERN", "PEBBLE", "PENDULUM", "PHANTOM",
        "PILLAR", "PLUME", "PRISM", "QUARRY", "QUIVER", "RADIANT", "RAVEN",
        "RIDGE", "RIPPLE", "RUSTLE", "SADDLE", "SCARF", "SHADOW", "SHELTER",
        "SIGNAL", "SPIRE", "SPRUCE", "SUMMIT", "TEMPLE", "THRESHOLD", "TIMBER",
        "TRELLIS", "TWILIGHT", "VALLEY", "VELVET", "VERTEX", "VIOLET", "WANDER",
        "WHISPER", "WILLOW", "WINDING", "ZEPHYR",
    ).distinct().sorted()
}
