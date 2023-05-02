/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.translations.index

import com.demonwav.mcdev.translations.Translation

data class TranslationIndexEntry(val sourceDomain: String, val translations: List<Translation>) {
    private val map: Map<String, Translation> = translations.associateBy { it.key }

    operator fun get(key: String) = map[key]

    operator fun contains(key: String) = this[key] != null
}

fun Sequence<TranslationIndexEntry>.merge(sourceDomain: String) =
    this.fold(TranslationIndexEntry(sourceDomain, emptyList())) { acc, entry ->
        TranslationIndexEntry(sourceDomain, acc.translations + entry.translations.filterNot { it.key in acc })
    }
