/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.intentions

import com.demonwav.mcdev.i18n.lang.gen.psi.LangEntry
import com.demonwav.mcdev.i18n.lang.gen.psi.LangTypes
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.util.IncorrectOperationException

class RemoveDuplicatesIntention(private val keep: LangEntry) : BaseIntentionAction() {
    override fun getText() = "Remove duplicates (keep this translation)"

    override fun getFamilyName() = "Minecraft localization"

    override fun isAvailable(project: Project, editor: Editor, psiFile: PsiFile) = true

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, psiFile: PsiFile) {
        for (elem in psiFile.children) {
            if (elem is LangEntry && elem !== keep && keep.key == elem.key) {
                if (elem.nextSibling?.node?.elementType === LangTypes.LINE_ENDING) {
                    elem.nextSibling.delete()
                }
                elem.delete()
            }
        }
    }
}

