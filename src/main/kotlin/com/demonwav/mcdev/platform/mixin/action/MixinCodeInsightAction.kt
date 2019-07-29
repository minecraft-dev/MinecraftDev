/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.findContainingClass
import com.intellij.codeInsight.actions.SimpleCodeInsightAction
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class MixinCodeInsightAction : SimpleCodeInsightAction() {

    override fun startInWriteAction() = false

    // Display action in Mixin classes only
    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file.language != JavaLanguage.INSTANCE) {
            return false
        }

        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        val containingClass = element.findContainingClass() ?: return false
        return containingClass.isMixin
    }
}
