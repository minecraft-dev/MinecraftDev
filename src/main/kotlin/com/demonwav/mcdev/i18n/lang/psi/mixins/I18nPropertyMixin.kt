/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang.psi.mixins

import com.intellij.psi.PsiNameIdentifierOwner

interface I18nPropertyMixin : PsiNameIdentifierOwner {
    val key: String
        get

    val trimmedKey: String
        get() = key.trim()

    val value: String
        get
}
