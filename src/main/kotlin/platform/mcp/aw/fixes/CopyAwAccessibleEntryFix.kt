/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.fixes

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.mcp.actions.CopyAwAction
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.daemon.QuickFixActionRegistrar
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement

class CopyAwAccessibleEntryFix(val target: PsiElement, val element: PsiElement) : IntentionAction {

    class Provider : UnresolvedReferenceQuickFixProvider<PsiJavaCodeReferenceElement>() {

        override fun registerFixes(ref: PsiJavaCodeReferenceElement, registrar: QuickFixActionRegistrar) {
            val module = ref.findModule() ?: return
            val isApplicable = MinecraftFacet.getInstance(module, FabricModuleType, SpongeModuleType) != null
            if (!isApplicable) {
                return
            }

            val resolve = ref.advancedResolve(true)
            val target = resolve.element
            if (target != null && !resolve.isAccessible) {
                registrar.register(CopyAwAccessibleEntryFix(target, ref))
            }
        }

        override fun getReferenceClass(): Class<PsiJavaCodeReferenceElement> = PsiJavaCodeReferenceElement::class.java
    }

    override fun startInWriteAction(): Boolean = false

    override fun getText(): String = "Copy AW entry"

    override fun getFamilyName(): String = "Copy AW entry for inaccessible element"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        CopyAwAction.doCopy(target, element, editor, null)
    }
}
