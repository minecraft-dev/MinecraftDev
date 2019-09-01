/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.structuralsearch.plugin.util.SmartPsiPointer
import com.siyeh.ig.InspectionGadgetsFix
import com.siyeh.ig.psiutils.ImportUtils

class UseGetterReturnTypeInspectionGadgetsFix(
    parameter: PsiParameter,
    newType: PsiTypeElement,
    private val name: String
) : InspectionGadgetsFix() {

    private val paramPointer: SmartPsiPointer = SmartPsiPointer(parameter)
    private val newTypePointer: SmartPsiPointer = SmartPsiPointer(newType)

    override fun doFix(project: Project, descriptor: ProblemDescriptor) {
        val parameter = paramPointer.element as? PsiParameter ?: return
        val newType = newTypePointer.element as? PsiTypeElement ?: return

        val newTypeRef = newType.type as JvmReferenceType
        for (typeParam in newTypeRef.typeArguments()) {
            val resolvedTypeParam = (typeParam as? PsiClassReferenceType)?.resolve() ?: continue
            ImportUtils.addImportIfNeeded(resolvedTypeParam, parameter)
        }

        val newTypeClass = newTypeRef.resolve() as? PsiClass ?: return
        ImportUtils.addImportIfNeeded(newTypeClass, parameter)
        parameter.typeElement!!.replace(newType)
    }

    override fun getName() = name

    override fun getFamilyName() = name
}
