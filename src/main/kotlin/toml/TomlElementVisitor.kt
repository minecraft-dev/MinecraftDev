/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toml

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlValue

abstract class TomlElementVisitor : PsiElementVisitor() {

    override fun visitElement(element: PsiElement) = when (element) {
        is TomlKeyValue -> visitKeyValue(element)
        is TomlKeySegment -> visitKeySegment(element)
        is TomlValue -> visitValue(element)
        else -> super.visitElement(element)
    }

    open fun visitKeyValue(keyValue: TomlKeyValue) = Unit

    open fun visitKeySegment(keySegment: TomlKeySegment) = Unit

    open fun visitValue(value: TomlValue) = Unit
}
