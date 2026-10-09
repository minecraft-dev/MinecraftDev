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
import com.demonwav.mcdev.platform.mixin.inspection.injector.ModifierSignatures
import com.demonwav.mcdev.platform.mixin.inspection.injector.SuggestedSignature
import com.demonwav.mcdev.platform.mixin.inspection.injector.collectSignatures
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.getBytecodeParameter
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.SequencedMap
import com.demonwav.mcdev.util.constantValue
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiMethod
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class ModifyArgHandler : InsnInjectorAnnotationHandler() {
    override fun isInsnAllowed(insn: AbstractInsnNode, decorations: Map<String, Any?>): Boolean {
        return insn is MethodInsnNode
    }

    override val allowedInsnDescription = "method invocations"

    override fun expectedMethodSignature(
        annotation: PsiAnnotation,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        targetInsn: TargetInsn,
    ): ExpectedSignatures<ModifierSignatures> {
        val insn = targetInsn.insn
        if (insn !is MethodInsnNode) {
            return ExpectedSignatures.Invalid
        }
        val project = annotation.project
        val index = annotation.findDeclaredAttributeValue("index")?.constantValue as? Int

        val argTypes = Type.getArgumentTypes(insn.desc)

        val validTypes = if (index == null) {
            argTypes.groupingBy { it }.eachCount().asSequence().filter { it.value == 1 }.map { it.key }.toList()
        } else {
            listOfNotNull(argTypes.getOrNull(index))
        }

        if (validTypes.isEmpty()) {
            return ExpectedSignatures.Invalid
        }

        // get the source method for parameter names
        val sourceMethod = MemberReference(
            insn.name,
            insn.desc,
            insn.owner.replace('/', '.')
        ).resolveMember(project) as PsiMethod?
        val elementFactory = JavaPsiFacade.getElementFactory(annotation.project)
        val psiParams = argTypes.indices.map { index -> sourceMethod?.getBytecodeParameter(index) }
        val paramOptions = validTypes.associateWithTo(linkedMapOf()) { type ->
            val targetParam = psiParams[index ?: argTypes.indexOf(type)]
            val psiType = targetParam?.type ?: type.toPsiType(elementFactory)
            sanitizedParameter(psiType, targetParam?.name)
        }
        val fullParams = if (argTypes.size > 1) {
            psiParams.zip(argTypes) { param, argType ->
                sanitizedParameter(
                    param?.type ?: argType.toPsiType(elementFactory),
                    param?.name,
                )
            }
        } else null
        return ExpectedSignatures.Valid(
            ModifierSignatures(
                SequencedMap(paramOptions),
                fullParams,
            )
        )
    }

    override fun suggestedMethodSignature(
        annotation: PsiAnnotation,
        targets: List<ClassAndMethodNode>
    ): SuggestedSignature? {
        return SuggestedSignature.modifier(
            annotation,
            expectedMethodSignatures(annotation, targets).collectSignatures<ModifierSignatures>() ?: return null,
        )
    }

    override val mixinExtrasExpressionContextType = ExpressionContext.Type.MODIFY_ARG
}
