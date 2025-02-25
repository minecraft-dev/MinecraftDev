/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinTargetMember
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.util.memberReference
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.tree.ClassNode

class OverwriteHandler : MixinMemberAnnotationHandler {
    override fun resolveTarget(annotation: PsiAnnotation, targetClass: ClassNode): List<MixinTargetMember> {
        val method = annotation.parentOfType<PsiMethod>() ?: return emptyList()
        val targetMethod = targetClass.findMethod(method.memberReference) ?: return emptyList()
        return listOf(MethodTargetMember(targetClass, targetMethod))
    }

    override fun createUnresolvedMessage(annotation: PsiAnnotation): String? {
        val method = annotation.parentOfType<PsiMethod>() ?: return null
        return "Unresolved method ${method.name} in target class"
    }

    override val isEntryPoint = true
}
