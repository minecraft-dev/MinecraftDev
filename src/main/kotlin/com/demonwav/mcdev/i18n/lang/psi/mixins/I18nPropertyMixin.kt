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
    fun getKey(): String

    fun getValue(): String
}