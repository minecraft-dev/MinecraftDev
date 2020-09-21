/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.impl.BaseIntentionAction
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiUtilCore

class MakeInferredMcdevAnnotationExplicit : BaseIntentionAction() {
    override fun getFamilyName() = "Make Inferred MinecraftDev Annotations Explicit"

    override fun getText() = "Make Inferred MinecraftDev Annotations Explicit"

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        val leaf = file.findElementAt(editor.caretModel.offset) ?: return false
        val owner = leaf.parent as? PsiModifierListOwner
        return isAvailable(file, owner)
    }

    fun isAvailable(file: PsiFile, owner: PsiModifierListOwner?): Boolean {
        if (owner != null &&
            owner.language.isKindOf(JavaLanguage.INSTANCE) &&
            isWritable(owner) &&
            file.findModule() != null
        ) {
            val annotation = SideOnlyUtil.getInferredAnnotationOnly(owner, SideHardness.HARD)
                ?: SideOnlyUtil.getInferredAnnotationOnly(owner, SideHardness.SOFT)
            if (annotation != null) {
                text = "Insert '@CheckEnv(Env.${annotation.side})'"
                return true
            }
        }
        return false
    }

    private fun isWritable(owner: PsiModifierListOwner): Boolean {
        if (owner is PsiCompiledElement) return false
        val vFile = PsiUtilCore.getVirtualFile(owner)
        return vFile != null && vFile.isInLocalFileSystem
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val leaf = file.findElementAt(editor.caretModel.offset) ?: return
        val owner = leaf.parent as? PsiModifierListOwner ?: return
        makeAnnotationExplicit(project, file, owner)
    }

    fun makeAnnotationExplicit(project: Project, file: PsiFile, owner: PsiModifierListOwner) {
        val modifierList = owner.modifierList ?: return
        val module = file.findModule() ?: return
        if (!SideOnlyUtil.ensureMcdevDependencyPresent(project, module, familyName, file.resolveScope)) {
            return
        }
        if (!FileModificationService.getInstance().preparePsiElementForWrite(owner)) return
        val facade = JavaPsiFacade.getInstance(project)
        val inferredSide = SideOnlyUtil.getInferredAnnotationOnly(owner, SideHardness.HARD)
            ?: SideOnlyUtil.getInferredAnnotationOnly(owner, SideHardness.SOFT) ?: return
        val inferred = facade.elementFactory.createAnnotationFromText(
            "@${SideOnlyUtil.MCDEV_SIDEONLY_ANNOTATION}(${SideOnlyUtil.MCDEV_SIDE}.${inferredSide.side})",
            owner
        )
        WriteCommandAction.runWriteCommandAction(project) {
            DumbService.getInstance(project).withAlternativeResolveEnabled {
                JavaCodeStyleManager.getInstance(project)
                    .shortenClassReferences(modifierList.addAfter(inferred, null))
            }
        }
    }

    override fun startInWriteAction() = false
}
