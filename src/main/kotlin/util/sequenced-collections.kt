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

@Suppress("JavaDefaultMethodsNotOverriddenByDelegation")
@JvmInline
value class SequencedSet<out T>(private val set: MutableSequencedSet<T>) : Set<T> by set

typealias MutableSequencedSet<T> = java.util.SequencedSet<T>

private val EMPTY_SEQUENCED_SET: SequencedSet<Nothing> = SequencedSet(linkedSetOf())

fun <T> emptySequencedSet(): SequencedSet<T> = EMPTY_SEQUENCED_SET

fun <T> sequencedSetOf(vararg elements: T): SequencedSet<T> = SequencedSet(linkedSetOf(*elements))

inline fun <T> buildSequencedSet(action: MutableSequencedSet<T>.() -> Unit): SequencedSet<T> =
    SequencedSet(linkedSetOf<T>().apply(action))

operator fun <T> SequencedSet<T>.minus(element: T): SequencedSet<T> {
    val result = linkedSetOf<T>()
    var removed = false
    this.filterTo(result) {
        if (!removed && it == element) {
            removed = true; false
        } else true
    }
    return SequencedSet(result)
}

@JvmInline
value class SequencedMap<K, out V>(private val map: MutableSequencedMap<K, V>) : Map<K, V> by map

typealias MutableSequencedMap<K, V> = java.util.SequencedMap<K, V>

private val EMPTY_SEQUENCED_MAP: SequencedMap<Nothing, Nothing> = SequencedMap(linkedMapOf())

@Suppress("UNCHECKED_CAST")
fun <K, V> emptySequencedMap(): SequencedMap<K, V> = EMPTY_SEQUENCED_MAP as SequencedMap<K, V>

fun <K, V> sequencedMapOf(vararg pairs: Pair<K, V>): SequencedMap<K, V> = SequencedMap(linkedMapOf(*pairs))
