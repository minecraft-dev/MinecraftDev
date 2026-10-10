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

package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.inspection.injector.GeneralSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.nextRealInsn
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiTypes
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class WrapWithConditionHandler : MixinExtrasInjectorAnnotationHandler() {
    override val oldSuperBehavior = true

    override val supportedInstructionTypes = listOf(
        InstructionType.METHOD_CALL, InstructionType.FIELD_SET
    )

    override fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        if (!super.isInsnAllowed(insn, decorations)) {
            return false
        }

        return when (val size = getInsnReturnType(insn)?.size) {
            0 -> true
            1 -> insn.nextRealInsn?.opcode == Opcodes.POP
            2 -> insn.nextRealInsn?.opcode == Opcodes.POP2
            else -> throw IllegalStateException("Unexpected return type size: $size")
        }
    }

    override val allowedInsnDescription = "void method invocations and field assignments"

    fun getValidTargetType(insn: AbstractInsnNode): Type? {
        if (!isInsnAllowed(insn, emptyMap())) {
            return null
        }

        return getInsnReturnType(insn)
    }

    fun getRequiredParameterCount(insn: AbstractInsnNode, targetClass: ClassNode, annotation: PsiAnnotation): Int {
        return getPsiParameters(insn, targetClass, annotation)?.size ?: 0
    }

    override fun expectedMethodSignatureImpl(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        target: TargetInsn
    ): GeneralSignatures? {
        val params = getPsiParameters(target.insn, targetClass, annotation) ?: return null
        return GeneralSignatures(
            params,
            PsiTypes.booleanType(),
            trailingParams = collectTargetMethodParameters(annotation.project, targetClass, targetMethod),
        )
    }

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature? {
        return SuggestedSignature.general(
            annotation,
            expectedMethodSignatures(annotation, targets).collectSignatures<GeneralSignatures>() ?: return null
        )
    }

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.WRAP_WITH_CONDITION
}
