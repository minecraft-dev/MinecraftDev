/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.actions

import com.demonwav.mcdev.util.findFieldByNameAndDescriptor
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.demonwav.mcdev.util.nameAndDescriptor
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import java.util.stream.Stream

internal fun collectRequiredMembers(element: PsiElement, psiClass: PsiClass): List<PsiMember> {
    val visitor = CollectRequiredClassMembersVisitor(psiClass)
    element.accept(visitor)
    return visitor.members
}

private class CollectRequiredClassMembersVisitor(val psiClass: PsiClass) : JavaRecursiveElementWalkingVisitor() {

    internal val members = ArrayList<PsiMember>()

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

internal fun filterNewShadows(requiredMembers: Collection<PsiMember>, psiClass: PsiClass): Stream<PsiMember> {
    return requiredMembers.stream().filter { m ->
        when(m) {
            is PsiMethod -> !psiClass.findMethodsByInternalNameAndDescriptor(m.internalNameAndDescriptor).findAny().isPresent
            is PsiField -> psiClass.findFieldByNameAndDescriptor(m.nameAndDescriptor) == null
            else -> throw UnsupportedOperationException("Unsupported member type: ${m.javaClass.name}")
        }
    }
}

