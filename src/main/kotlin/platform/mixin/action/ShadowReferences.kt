/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
