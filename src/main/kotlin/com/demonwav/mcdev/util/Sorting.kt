/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import java.util.ArrayList
import java.util.Arrays
import java.util.Comparator
import java.util.stream.Collectors
import java.util.stream.Stream

val LEXICOGRAPHICAL_ORDER: Comparator<IntArray> = Comparator { one, two ->
    val length = Math.min(one.size, two.size)
    for (i in 0..length - 1) {
        val first = one[i]
        val second = two[i]

        if (first < second) {
            return@Comparator -1
        } else if (second < first) {
            return@Comparator 1
        }
    }

    // We've got here so they are now equal, if one has more then they are not equal
    if (one.size < two.size) {
        return@Comparator -1
    } else if (two.size < one.size) {
        return@Comparator 1
    }
    // They are the same
    return@Comparator 0
}

val REVERSE_LEXICOGRAPHICAL_ORDER: Comparator<IntArray> = LEXICOGRAPHICAL_ORDER.reversed()

/**
 * This is the lowest version value we will let users choose, to make our lives easier.
 */
private val ARRAY_1_8_8 = intArrayOf(1, 8, 8)

fun sortVersions(versions: Collection<*>): List<String> {
    // Populate a list of the keys (and cast them to String) so they can be sorted
    val list = ArrayList<String>(versions.size)
    list.addAll(versions.stream().map(Any?::toString).collect(Collectors.toList<String>()))

    // We map each version string (1.2, 1.9.4, 1.10, etc) to an array of integers {1, 2}, {1, 9, 4}, {1, 10} so we
    // can lexicographically order them. We throw out the odd-balls in the process (like 1.10-pre4)
    val intList = list.stream().distinct().mapNotNull { s ->
        try {
            return@mapNotNull Stream.of(*s.split("\\.".toRegex()).dropLastWhile(String::isEmpty).toTypedArray())
                .mapToInt { Integer.parseInt(it) }.toArray()
        } catch (e: NumberFormatException) {
            return@mapNotNull null
        }
    }.collect(Collectors.toCollection { ArrayList<IntArray>() })

    // Sort them correctly
    intList.sortWith(REVERSE_LEXICOGRAPHICAL_ORDER)
    intList.removeIf { ints -> LEXICOGRAPHICAL_ORDER.compare(ints, ARRAY_1_8_8) < 0 }

    return intList.stream().map{ ints -> Arrays.stream(ints).mapToObj(Int::toString).collect(Collectors.joining(".")) }
        .collect(Collectors.toList<String>())
}
