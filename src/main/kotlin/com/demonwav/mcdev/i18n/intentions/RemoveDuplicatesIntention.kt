/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.intentions

import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nTypes
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class RemoveDuplicatesIntention(private val keep: I18nEntry) : BaseIntentionAction() {
    override fun getText() = "Remove duplicates (keep this translation)"

    override fun getFamilyName() = "Minecraft localization"

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = true

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        for (elem in psiFile.children) {
            if (elem is I18nEntry && elem !== keep && keep.key == elem.key) {
                if (elem.nextSibling?.node?.elementType === I18nTypes.LINE_ENDING) {
                    elem.nextSibling.delete()
                }
                elem.delete()
            }
        }
    }
}
