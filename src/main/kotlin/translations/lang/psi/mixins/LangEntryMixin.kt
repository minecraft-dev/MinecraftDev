/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang.psi.mixins

import com.intellij.psi.PsiNameIdentifierOwner

interface LangEntryMixin : PsiNameIdentifierOwner {
    val key: String

    val trimmedKey: String
        get() = key.trim()

    val value: String
}
