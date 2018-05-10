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

fun sortVersions(versions: Collection<*>): List<String> =
    versions.asSequence()
        .map(Any?::toString)
        .map(SemanticVersion.Companion::parse)
        .sortedDescending()
        .filter { it >= MC_1_8_8 }
        .map { it.toString() }
        .toList()
