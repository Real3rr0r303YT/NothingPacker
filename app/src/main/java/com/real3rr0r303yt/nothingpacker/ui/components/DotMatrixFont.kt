/*
 * NothingPacker – offline APK ⇄ IPA toolkit for Android
 * Copyright (C) 2026 Real3rr0r303YT
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.real3rr0r303yt.nothingpacker.ui.components

/**
 * A built-in 5×7 dot-matrix font (no font files, no downloads).
 * Each glyph is 7 rows; `#` marks a lit dot. Lower case is rendered as upper case.
 */
object DotMatrixFont {

    const val ROWS = 7

    class Glyph(rows: List<String>) {
        val width: Int = rows.maxOf { it.length }
        internal val isWellFormed: Boolean = rows.size == ROWS && rows.all { it.length == width }
        /** Lit dots as (column, row) pairs. */
        val dots: List<Pair<Int, Int>> = rows.flatMapIndexed { y, row ->
            row.mapIndexedNotNull { x, c -> if (c == '#') x to y else null }
        }
    }

    private val glyphs: Map<Char, Glyph> = mapOf(
        'A' to g(".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
        'B' to g("####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."),
        'C' to g(".###.", "#...#", "#....", "#....", "#....", "#...#", ".###."),
        'D' to g("####.", "#...#", "#...#", "#...#", "#...#", "#...#", "####."),
        'E' to g("#####", "#....", "#....", "####.", "#....", "#....", "#####"),
        'F' to g("#####", "#....", "#....", "####.", "#....", "#....", "#...."),
        'G' to g(".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####"),
        'H' to g("#...#", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"),
        'I' to g("###", ".#.", ".#.", ".#.", ".#.", ".#.", "###"),
        'J' to g("..###", "...#.", "...#.", "...#.", "#..#.", "#..#.", ".##.."),
        'K' to g("#...#", "#..#.", "#.#..", "##...", "#.#..", "#..#.", "#...#"),
        'L' to g("#....", "#....", "#....", "#....", "#....", "#....", "#####"),
        'M' to g("#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"),
        'N' to g("#...#", "#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#"),
        'O' to g(".###.", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
        'P' to g("####.", "#...#", "#...#", "####.", "#....", "#....", "#...."),
        'Q' to g(".###.", "#...#", "#...#", "#...#", "#.#.#", "#..#.", ".##.#"),
        'R' to g("####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"),
        'S' to g(".####", "#....", "#....", ".###.", "....#", "....#", "####."),
        'T' to g("#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."),
        'U' to g("#...#", "#...#", "#...#", "#...#", "#...#", "#...#", ".###."),
        'V' to g("#...#", "#...#", "#...#", "#...#", "#...#", ".#.#.", "..#.."),
        'W' to g("#...#", "#...#", "#...#", "#.#.#", "#.#.#", "#.#.#", ".#.#."),
        'X' to g("#...#", "#...#", ".#.#.", "..#..", ".#.#.", "#...#", "#...#"),
        'Y' to g("#...#", "#...#", ".#.#.", "..#..", "..#..", "..#..", "..#.."),
        'Z' to g("#####", "....#", "...#.", "..#..", ".#...", "#....", "#####"),
        'Ä' to g("#...#", ".....", ".###.", "#...#", "#####", "#...#", "#...#"),
        'Ö' to g("#...#", ".....", ".###.", "#...#", "#...#", "#...#", ".###."),
        'Ü' to g("#...#", ".....", "#...#", "#...#", "#...#", "#...#", ".###."),
        '0' to g(".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."),
        '1' to g(".#.", "##.", ".#.", ".#.", ".#.", ".#.", "###"),
        '2' to g(".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"),
        '3' to g("####.", "....#", "....#", ".###.", "....#", "....#", "####."),
        '4' to g("...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."),
        '5' to g("#####", "#....", "####.", "....#", "....#", "#...#", ".###."),
        '6' to g(".###.", "#....", "#....", "####.", "#...#", "#...#", ".###."),
        '7' to g("#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."),
        '8' to g(".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."),
        '9' to g(".###.", "#...#", "#...#", ".####", "....#", "....#", ".###."),
        ' ' to g("...", "...", "...", "...", "...", "...", "..."),
        '.' to g(".", ".", ".", ".", ".", ".", "#"),
        ',' to g("..", "..", "..", "..", "..", ".#", "#."),
        ':' to g(".", "#", ".", ".", ".", "#", "."),
        '·' to g(".", ".", ".", "#", ".", ".", "."),
        '-' to g("....", "....", "....", "####", "....", "....", "...."),
        '+' to g(".....", "..#..", "..#..", "#####", "..#..", "..#..", "....."),
        '/' to g("....#", "....#", "...#.", "..#..", ".#...", "#....", "#...."),
        '%' to g("##..#", "##..#", "...#.", "..#..", ".#...", "#..##", "#..##"),
        '!' to g("#", "#", "#", "#", "#", ".", "#"),
        '?' to g(".###.", "#...#", "....#", "...#.", "..#..", ".....", "..#.."),
        '(' to g("..#", ".#.", "#..", "#..", "#..", ".#.", "..#"),
        ')' to g("#..", ".#.", "..#", "..#", "..#", ".#.", "#.."),
        '→' to g(".....", "..#..", "...#.", "#####", "...#.", "..#..", "....."),
        '⇄' to g("..#..", ".#...", "#####", ".#.#.", "#####", "...#.", "..#.."),
        '…' to g(".....", ".....", ".....", ".....", ".....", ".....", "#.#.#"),
    )

    private fun g(vararg rows: String) = Glyph(rows.toList())

    private val fallback = glyphs.getValue(' ')

    /** Normalises [text] to the characters this font can draw. */
    fun normalize(text: String): String = text
        .uppercase()
        .replace(' ', ' ')
        .replace(' ', ' ')

    fun glyph(c: Char): Glyph = glyphs[c] ?: fallback

    fun supports(c: Char): Boolean = c in glyphs

    /** Every glyph must be exactly [ROWS] rows high and rectangular. */
    internal fun isWellFormed(): Boolean = glyphs.values.all { it.isWellFormed }

    /** Total width in dot columns, including one empty column between glyphs. */
    fun measure(text: String): Int {
        val normalized = normalize(text)
        if (normalized.isEmpty()) return 0
        return normalized.sumOf { glyph(it).width } + normalized.length - 1
    }
}
