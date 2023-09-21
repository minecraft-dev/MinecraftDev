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

package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.injector.ParameterGroup
import com.demonwav.mcdev.util.Parameter
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

private const val OPERATION = "com.llamalad7.mixinextras.injector.wrapoperation.Operation"

class WrapOperationHandler : MixinExtrasInjectorAnnotationHandler() {
    override val supportedElementTypes = listOf(
        ElementType.METHOD_CALL, ElementType.FIELD_GET, ElementType.FIELD_SET, ElementType.INSTANCEOF
    )

    override fun getAtKey(annotation: PsiAnnotation): String {
        return if (annotation.hasAttribute("constant")) "constant" else "at"
    }

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode
    ): Pair<ParameterGroup, PsiType>? {
        val params = getPsiParameters(insn, targetClass, annotation) ?: return null
        val returnType = getPsiReturnType(insn, annotation) ?: return null
        val operationType = getOperationType(annotation, returnType) ?: return null
        return ParameterGroup(
            params + Parameter("original", operationType)
        ) to returnType
    }

    private fun getOperationType(context: PsiElement, type: PsiType): PsiType? {
        val project = context.project
        val boxedType = if (type is PsiPrimitiveType) {
            type.getBoxedType(context) ?: return null
        } else {
            type
        }
        val psiClass =
            JavaPsiFacade.getInstance(project).findClass(OPERATION, GlobalSearchScope.allScope(project))
                ?: return null
        return JavaPsiFacade.getElementFactory(project).createType(psiClass, boxedType)
    }
}
