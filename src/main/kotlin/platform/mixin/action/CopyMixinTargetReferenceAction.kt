/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.action

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.QualifiedMember
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.util.findReferencedMember
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.CARET
import com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import java.awt.datatransfer.StringSelection

class CopyMixinTargetReferenceAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(PSI_FILE) ?: return
        val caret = e.getData(CARET) ?: return

        val element = file.findElementAt(caret.offset) ?: return
        val member = element.findReferencedMember() ?: return
        val targetClass = (element.parent as? PsiQualifiedReference)?.let { QualifiedMember.resolveQualifier(it) }

        val targetReference = when (member) {
            is PsiMethod -> if (targetClass != null) {
                member.getQualifiedMemberReference(targetClass)
            } else {
                member.qualifiedMemberReference
            }
            is PsiField -> if (targetClass != null) {
                member.getQualifiedMemberReference(targetClass)
            } else {
                member.qualifiedMemberReference
            }
            else -> return
        }

        CopyPasteManager.getInstance().setContents(StringSelection(targetReference.toMixinString()))
        WindowManager.getInstance().getStatusBar(project).info = "Mixin target reference has been copied."
    }
}
