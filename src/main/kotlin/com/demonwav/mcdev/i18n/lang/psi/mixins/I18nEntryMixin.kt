/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.psi.mixins

import com.intellij.psi.PsiNameIdentifierOwner

interface I18nEntryMixin : PsiNameIdentifierOwner {
    val key: String

    val trimmedKey: String
        get() = key.trim()

    val value: String
}
