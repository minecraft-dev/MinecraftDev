/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.index

data class TranslationIndexEntry(val sourceDomain: String, val translations: List<TranslationEntry>) {
    private val map: Map<String, TranslationEntry> = translations.associateBy { it.key }

    operator fun get(key: String) = map[key]

    operator fun contains(key: String) = this[key] != null
}

fun Iterable<TranslationIndexEntry>.merge(sourceDomain: String) =
    this.asSequence().fold(TranslationIndexEntry(sourceDomain, emptyList())) { acc, entry ->
        TranslationIndexEntry(sourceDomain, acc.translations + entry.translations.filterNot { it.key in acc })
    }
