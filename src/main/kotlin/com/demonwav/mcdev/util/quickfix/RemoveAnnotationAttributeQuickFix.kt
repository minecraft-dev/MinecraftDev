/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util.quickfix

import com.demonwav.mcdev.util.KotlinIsBroken
import com.demonwav.mcdev.util.annotationFromValue
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project

class RemoveAnnotationAttributeQuickFix(val annotation: String, val attribute: String) : LocalQuickFix {
    override fun getFamilyName() = "Remove $attribute from $annotation"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        KotlinIsBroken.removeAnnotationAttribute(descriptor.psiElement.annotationFromValue!!, attribute)
    }

}
