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

package com.demonwav.mcdev.platform.mixin.handlers

import com.demonwav.mcdev.platform.mixin.handlers.mixinextras.TargetInsn
import com.demonwav.mcdev.platform.mixin.inspection.injector.ExpectedSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.InjectSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.demonwav.mcdev.platform.mixin.util.getGenericReturnType
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isFabricMixin
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.Parameter
import com.demonwav.mcdev.util.findModule
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiQualifiedReference
import com.intellij.psi.util.parentOfType
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class InjectAnnotationHandler : InsnInjectorAnnotationHandler() {
    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        targetInsn: TargetInsn,
    ): ExpectedSignatures<InjectSignatures> {
        val returnType = targetMethod.getGenericReturnType(targetClass, annotation.project)

        // Parameters from injected method
        val targetParams = collectTargetMethodParameters(annotation.project, targetClass, targetMethod)

        // Captured locals (only if local capture is enabled)
        var capturedLocals = emptyList<Parameter>()

        val localCapture = (annotation.findDeclaredAttributeValue("locals") as? PsiQualifiedReference)
            ?.referenceName ?: "NO_CAPTURE"
        if (localCapture != "NO_CAPTURE") {
            annotation.findModule()?.let { module ->
                val locals = LocalVariables.getLocals(module, targetClass, targetMethod, targetInsn.insn)
                    ?.filterNotNull()
                    ?.drop(
                        Type.getArgumentTypes(targetMethod.desc).size +
                            if (targetMethod.hasAccess(Opcodes.ACC_STATIC)) 0 else 1,
                    )
                    ?.filter { it.desc != null }
                    ?: return@let

                val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
                capturedLocals = locals.map { local ->
                    val type =
                        Type.getType(local.desc).toPsiType(elementFactory, annotation.parentOfType<PsiMethod>())
                    sanitizedParameter(type, local.name)
                }
            }
        }

        return ExpectedSignatures.Valid(
            InjectSignatures.create(
                annotation,
                targetParams,
                returnType,
                capturedLocals,
            ) ?: return ExpectedSignatures.Invalid
        )
    }

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature? {
        return SuggestedSignature.inject(
            annotation,
            expectedMethodSignatures(annotation, targets).collectSignatures<InjectSignatures>() ?: return null,
        )
    }

    override fun canAlwaysBeStatic(method: PsiMethod): Boolean {
        return method.isFabricMixin
    }

    override val isShiftAlwaysDiscouraged = false

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.INJECT
}
