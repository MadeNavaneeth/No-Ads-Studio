package com.example.lightapp.sudoku.core

enum class GameType(
    val size: Int,
    val sectionHeight: Int,
    val sectionWidth: Int,
    val displayName: String
) {
    Unspecified(1, 1, 1, "Unspecified"),
    Default9x9(9, 3, 3, "9×9"),
    Default12x12(12, 3, 4, "12×12"),
    Default6x6(6, 2, 3, "6×6"),
    Killer9x9(9, 3, 3, "Killer 9×9"),
    Killer12x12(12, 3, 4, "Killer 12×12"),
    Killer6x6(6, 2, 3, "Killer 6×6"),
}
