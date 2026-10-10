/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

/**
 * Represents a prioritised set of elements. Elements can only be present once and assume the lowest priority value with
 * which they were added. Lower priority values indicate more important elements. Iteration yields the elements in
 * priority order, with ties broken by insertion order.
 */
class PrioritySet<T> : Iterable<T> {
    private val buckets = sortedMapOf<Int, MutableSequencedSet<T>>()
    private val priorities = hashMapOf<T, Int>()

    fun add(element: T, priority: Int) {
        val existing = priorities[element]
        when {
            existing == null -> {}
            priority < existing -> buckets.getValue(existing).remove(element)
            priority >= existing -> return
        }
        priorities[element] = priority
        buckets.getOrPut(priority, ::linkedSetOf).add(element)
    }

    override fun iterator(): Iterator<T> =
        buckets.values.asSequence().flatten().iterator()
}
