/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection.reference

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinAnnotationAttributeInspection
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.util.constantStringValue
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiBinaryExpression
import com.intellij.psi.PsiLiteral

class AmbiguousReferenceInspection : MixinAnnotationAttributeInspection("method") {

    override fun getStaticDescription() = "Reports ambiguous references in Mixin annotations"

    override fun visitAnnotationAttribute(
        annotation: PsiAnnotation,
        value: PsiAnnotationMemberValue,
        holder: ProblemsHolder
    ) {
        val qName = annotation.qualifiedName ?: return
        val handler = MixinAnnotationHandler.forMixinAnnotation(qName, annotation.project)
        if (handler !is InjectorAnnotationHandler || handler.isSoft) {
            return
        }

        when (value) {
            is PsiLiteral -> checkMember(value, holder)
            is PsiArrayInitializerMemberValue -> value.initializers.forEach { checkMember(it, holder) }
        }
    }

    private fun checkMember(value: PsiAnnotationMemberValue, holder: ProblemsHolder) {
        val ambiguousReference = MethodReference.getReferenceIfAmbiguous(value) ?: return
        if (ambiguousReference.matchAllNames || ambiguousReference.matchAllDescs) {
            // the intent of ambiguity is clear
            return
        }
        holder.registerProblem(
            value,
            "Ambiguous reference to method '${ambiguousReference.name}' in target class",
            QuickFix
        )
    }

    private object QuickFix : LocalQuickFix {
        override fun getFamilyName() = "Add * wildcard"

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement ?: return
            val constantValue = element.constantStringValue

            val elementFactory = JavaPsiFacade.getElementFactory(project)

            if (constantValue != null && element is PsiLiteral) {
                val newLiteral = "\"${StringUtil.escapeStringCharacters("$constantValue*")}\""
                element.replace(elementFactory.createExpressionFromText(newLiteral, null))
            } else {
                val replacement = elementFactory.createExpressionFromText("str + \"*\"", null) as PsiBinaryExpression
                replacement.lOperand.replace(element)
                element.replace(replacement)
            }
        }
    }
}
