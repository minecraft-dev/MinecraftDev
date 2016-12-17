/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.actions

import com.demonwav.mcdev.util.appendDescriptor
import com.demonwav.mcdev.util.findReferencedMember
import com.demonwav.mcdev.util.getClassOfElement
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.CARET
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.awt.datatransfer.StringSelection

class CopyMixinTargetReferenceAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent?) {
        val project = e!!.project ?: return
        val file = e.getData(PSI_FILE) ?: return
        val caret = e.getData(CARET) ?: return

        val element = file.findElementAt(caret.offset) ?: return
        val member = findReferencedMember(element) ?: return
        val classElement = getClassOfElement(member) ?: return
        classElement.qualifiedName ?: return

        val builder = StringBuilder()
        classElement.appendDescriptor(builder).append(member.name)

        when (member) {
            is PsiField -> {
                member.appendDescriptor(builder.append(':'))
            }
            is PsiMethod -> {
                member.appendDescriptor(builder)
            }
        }

        CopyPasteManager.getInstance().setContents(StringSelection(builder.toString()))
        WindowManager.getInstance().getStatusBar(project).info = "Mixin target reference has been copied."
    }

}
