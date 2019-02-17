/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import java.util.Comparator

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
private val MC_1_8_8 = SemanticVersion.release(1, 8, 8)

fun <T> sortVersions(versions: Collection<T>, convert: (T) -> String = {it.toString()}): List<T> {
    if (versions.isEmpty()) {
        return listOf()
    }
    return versions.asSequence()
        .map { convert(it) to it }
        .map { SemanticVersion.parse(it.first) to it.second }
        .sortedByDescending { it.first }
        .filter { it.first >= MC_1_8_8 }
        .map { it.second }
        .toList()
}

fun getMajorVersion(version: String): String {
    val index = version.lastIndexOf('.')
    return if (index != -1 && index != version.indexOf('.')) {
        version.substring(0, index)
    } else {
        version
    }
}
