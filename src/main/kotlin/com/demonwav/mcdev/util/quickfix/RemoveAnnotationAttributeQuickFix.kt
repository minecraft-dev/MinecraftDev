/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util.quickfix

import com.demonwav.mcdev.util.annotationFromValue
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotationMemberValue

class RemoveAnnotationAttributeQuickFix(val annotation: String, private val attribute: String) : LocalQuickFix {
    override fun getFamilyName() = "Remove $attribute from $annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        descriptor.psiElement.annotationFromValue?.setDeclaredAttributeValue<PsiAnnotationMemberValue?>(attribute, null)
    }
}
