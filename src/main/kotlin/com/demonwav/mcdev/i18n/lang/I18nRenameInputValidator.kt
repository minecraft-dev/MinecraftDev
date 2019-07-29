/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.lang

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidatorEx
import com.intellij.util.ProcessingContext

class I18nRenameInputValidator : RenameInputValidatorEx {
    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext) = !newName.contains('=')

    override fun getPattern(): ElementPattern<out PsiElement> = PlatformPatterns.psiElement(I18nEntry::class.java)

    override fun getErrorMessage(newName: String, project: Project) =
        if (newName.contains('=')) "Key must not contain separator character ('=')" else null
}
