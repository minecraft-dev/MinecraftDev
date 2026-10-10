/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.util

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

/**
 * An interface which matches members, that's it really.
 */
interface MemberMatcher {
    fun matchField(owner: String, name: String, desc: String): Boolean
    fun matchMethod(owner: String, name: String, desc: String): Boolean

    fun matchField(field: PsiField, qualifier: PsiClass): Boolean {
        if (!canEverMatch(field.name)) {
            return false
        }
        val fqn = qualifier.fullQualifiedName ?: return false
        val desc = field.descriptor ?: return false
        return matchField(fqn.replace('.', '/'), field.name, desc)
    }

    fun matchField(field: FieldNode, qualifier: ClassNode): Boolean {
        return matchField(qualifier.name, field.name, field.desc)
    }

    fun matchMethod(method: PsiMethod, qualifier: PsiClass): Boolean {
        if (!canEverMatch(method.internalName)) {
            return false
        }
        val fqn = qualifier.fullQualifiedName ?: return false
        val desc = method.descriptor ?: return false
        return matchMethod(fqn.replace('.', '/'), method.internalName, desc)
    }

    fun matchMethod(method: MethodNode, qualifier: ClassNode): Boolean {
        return matchMethod(qualifier.name, method.name, method.desc)
    }

    fun getCustomOwner(owner: ClassNode): ClassNode {
        return owner
    }

    /**
     * Implement this to return false for early-out optimizations, so you don't need to resolve the member in the
     * navigation visitor
     */
    fun canEverMatch(name: String): Boolean {
        return true
    }

    val owner: String?
    val methodDescriptor: String?
    val fieldDescriptor: String?
    val qualified
        get() = owner != null
    val canMatchFields
        get() = methodDescriptor == null
    val canMatchMethods
        get() = fieldDescriptor == null
}
