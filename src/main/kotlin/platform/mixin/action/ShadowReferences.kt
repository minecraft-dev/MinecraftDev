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

import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.memberReference
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod

fun collectRequiredMembers(element: PsiElement, psiClass: PsiClass): List<PsiMember> {
    val visitor = CollectRequiredClassMembersVisitor(psiClass)
    element.accept(visitor)
    return visitor.members
}

private class CollectRequiredClassMembersVisitor(private val psiClass: PsiClass) :
    JavaRecursiveElementWalkingVisitor() {

    val members = ArrayList<PsiMember>()

    override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
        val resolved = reference.advancedResolve(false).element as? PsiMember ?: return
        if (resolved !is PsiMethod && resolved !is PsiField) {
            return
        }

        val resolvedOwner = resolved.containingClass ?: return

        if (psiClass.isEquivalentTo(resolvedOwner)) {
            // Reference points to the owning class
            members.add(resolved)
        }

        super.visitReferenceElement(reference)
    }
}

fun filterNewShadows(requiredMembers: Collection<PsiMember>, psiClass: PsiClass): Sequence<PsiMember> {
    return requiredMembers.asSequence().filter { m ->
        when (m) {
            is PsiMethod -> psiClass.findMethods(m.memberReference).none()
            is PsiField -> psiClass.findField(m.memberReference) == null
            else -> throw UnsupportedOperationException("Unsupported member type: ${m::class.java.name}")
        }
    }
}
