/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.util

fun <T> Comparator<in T>.lexicographical(): Comparator<in Iterable<T>> =
    Comparator { left, right ->
        // Zipping limits the compared parts to the shorter list, then we perform a component-wise comparison
        // Short-circuits if any component of the left side is smaller or greater
        left.zip(right).fold(0) { acc, (a, b) -> if (acc != 0) return@Comparator acc else this.compare(a, b) }.let {
            // When all the parts are equal, the longer list wins (is greater)
            if (it == 0) {
                left.count() - right.count()
            } else {
                // Still required if the last part was not equal, the short-circuiting does not cover that
                it
            }
        }
    }

/**
 * This is the lowest version value we will let users choose, to make our lives easier.
 */
private val MC_1_12 = SemanticVersion.release(1, 12)

fun <T> sortVersions(versions: Collection<T>, convert: (T) -> String = { it.toString() }): List<SemanticVersion> {
    if (versions.isEmpty()) {
        return listOf()
    }
    return versions.asSequence()
        .map { convert(it) }
        .map { SemanticVersion.parse(it) }
        .sortedDescending()
        .filter { it >= MC_1_12 }
        .toList()
}
