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

import com.demonwav.mcdev.platform.mixin.util.findSoftImplements
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findMatchingMethod
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.codeInsight.generation.GenerateMembersUtil
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.codeInsight.generation.PsiMethodMember
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import java.util.IdentityHashMap

class GenerateSoftImplementsAction : MixinCodeInsightAction() {

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val offset = editor.caretModel.offset
        val psiClass = file.findElementAt(offset)?.findContainingClass() ?: return

        val implements = psiClass.findSoftImplements() ?: return
        if (implements.isEmpty()) {
            return
        }

        val methods = IdentityHashMap<PsiMethodMember, String>()

        for ((prefix, iface) in implements) {
            for (signature in iface.visibleSignatures) {
                val method = signature.method
                if (method.isConstructor || method.hasModifierProperty(PsiModifier.STATIC)) {
                    continue
                }

                // Include only methods from interfaces
                val containingClass = method.containingClass
                if (containingClass == null || !containingClass.isInterface) {
                    continue
                }

                // Check if not already implemented
                if (psiClass.findMatchingMethod(method, false, prefix + method.name) == null) {
                    methods[PsiMethodMember(method)] = prefix
                }
            }
        }

        if (methods.isEmpty()) {
            HintManager.getInstance().showErrorHint(editor, "No methods to soft-implement have been found")
            return
        }

        val chooser = MemberChooser<PsiMethodMember>(methods.keys.toTypedArray(), false, true, project)
        chooser.title = "Select Methods to Soft-implement"
        chooser.show()

        val elements = (chooser.selectedElements ?: return).ifEmpty { return }

        runWriteAction {
            GenerateMembersUtil.insertMembersAtOffset(
                file,
                offset,
                elements.flatMap {
                    val method = it.element
                    val prefix = methods[it]

                    OverrideImplementUtil.overrideOrImplementMethod(
                        psiClass,
                        method,
                        it.substitutor,
                        chooser.isCopyJavadoc,
                        false
                    )
                        .map { m ->
                            // Apply prefix
                            m.name = prefix + m.name
                            OverrideImplementUtil.createGenerationInfo(m)
                        }
                }
            ).firstOrNull()?.positionCaret(editor, true)
        }
    }
}
