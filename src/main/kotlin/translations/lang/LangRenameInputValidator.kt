/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.lang

import com.demonwav.mcdev.translations.lang.gen.psi.LangEntry
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidatorEx
import com.intellij.util.ProcessingContext

class LangRenameInputValidator : RenameInputValidatorEx {
    override fun isInputValid(newName: String, element: PsiElement, context: ProcessingContext) = !newName.contains('=')

    override fun getPattern(): ElementPattern<out PsiElement> = PlatformPatterns.psiElement(LangEntry::class.java)

    override fun getErrorMessage(newName: String, project: Project) =
        if (newName.contains('=')) "Key must not contain separator character ('=')" else null
}
