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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InsnResolutionInfo
import com.demonwav.mcdev.platform.mixin.util.FieldTargetMember
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.findSourceElement
import com.demonwav.mcdev.platform.mixin.util.findSourceField
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import org.objectweb.asm.tree.ClassNode

interface MixinMemberAnnotationHandler : MixinAnnotationHandler {
    override fun isUnresolved(annotation: PsiAnnotation, targetClass: ClassNode): InsnResolutionInfo.Failure? {
        return if (resolveTarget(annotation, targetClass).isEmpty()) {
            InsnResolutionInfo.Failure()
        } else {
            null
        }
    }

    override fun resolveForNavigation(annotation: PsiAnnotation, targetClass: ClassNode): List<PsiElement> {
        val targets = resolveTarget(annotation, targetClass)
        return targets.mapNotNull {
            when (it) {
                is FieldTargetMember ->
                    it.classAndField.field.findSourceField(
                        it.classAndField.clazz,
                        annotation.project,
                        annotation.resolveScope,
                        canDecompile = true,
                    )
                is MethodTargetMember ->
                    it.classAndMethod.method.findSourceElement(
                        it.classAndMethod.clazz,
                        annotation.project,
                        annotation.resolveScope,
                        canDecompile = true,
                    )
            }
        }
    }
}
