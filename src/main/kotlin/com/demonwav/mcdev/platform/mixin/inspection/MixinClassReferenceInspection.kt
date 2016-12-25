/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.getClassOfElement
import com.intellij.codeInspection.BaseJavaBatchLocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.PsiClassReferenceType

class MixinClassReferenceInspection : BaseJavaBatchLocalInspectionTool() {

    override fun getDisplayName() = "Reference to Mixin class"
    override fun getStaticDescription() = "A Mixin class doesn't exist at runtime, and thus cannot be referenced directly."

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = MixinClassReferenceVisitor(holder)

}

private class MixinClassReferenceVisitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

    override fun visitTypeElement(type: PsiTypeElement) {
        val classType = type.type as? PsiClassReferenceType ?: return
        val referencedClass = classType.resolve() ?: return

        if (MixinUtils.getMixinAnnotation(referencedClass) == null) {
            return
        }

        // Check if the the reference is a super Mixin
        val psiClass = getClassOfElement(type) ?: return
        if (psiClass.isEquivalentTo(referencedClass) || psiClass.isInheritor(referencedClass, true)) {
            // Mixin class is part of the hierarchy
            return
        }

        holder.registerProblem(type, "${referencedClass.name} is a Mixin class, and cannot be referenced directly")
    }

}

