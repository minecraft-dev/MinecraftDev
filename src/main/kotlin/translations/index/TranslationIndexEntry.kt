/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
