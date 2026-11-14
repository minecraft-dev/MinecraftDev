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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.MinecraftProjectSettings
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.nextRealInsn
import com.demonwav.mcdev.util.BeforeOrAfter
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierList
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.VarInsnNode

class InvokeAssignReplaceWithExpressionInspection : MixinInspection() {
    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        val hasExpressions = JavaPsiFacade.getInstance(holder.project).findClass(
            MixinConstants.MixinExtras.EXPRESSION,
            holder.file.resolveScope
        ) != null
        if (!hasExpressions) {
            return PsiElementVisitor.EMPTY_VISITOR
        }
        return object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                if (!annotation.hasQualifiedName(MixinConstants.Annotations.AT)) {
                    return
                }
                val atValue = annotation.findDeclaredAttributeValue("value") ?: return
                if (atValue.constantStringValue != "INVOKE_ASSIGN") {
                    return
                }
                val atTarget = annotation.findDeclaredAttributeValue("target") ?: return
                val target = parseMixinSelector(atTarget) ?: return
                val methodInsn = resolveMethodInsn(annotation, target) ?: return
                val customArgs = AtResolver.getArgs(annotation)
                if (customArgs.containsKey("fuzz") || customArgs.containsKey("skip")) {
                    return
                }

                holder.registerProblem(
                    atValue,
                    "INVOKE_ASSIGN could be replaced with expression",
                    ReplaceWithExpressionFix(
                        annotation,
                        methodInsn.name.toJavaIdentifier(),
                        methodInsn.opcode == Opcodes.INVOKESTATIC,
                        Type.getArgumentCount(methodInsn.desc)
                    )
                )
            }
        }
    }

    private fun resolveMethodInsn(at: PsiAnnotation, target: MixinSelector): MethodInsnNode? {
        val injectorAnnotation = AtResolver.findInjectorAnnotation(at) ?: return null
        val insns = MixinAnnotationHandler.resolveTarget(injectorAnnotation)
            .flatMap { targetMember ->
                if (targetMember !is MethodTargetMember) {
                    return@flatMap emptyList()
                }
                val instructions = targetMember.classAndMethod.method.instructions ?: return@flatMap emptyList()
                instructions.asSequence()
                    .filterIsInstance<MethodInsnNode>()
                    .filter { target.matchMethod(it.owner, it.name, it.desc) }
                    .asIterable()
            }
        if (insns.isEmpty()) {
            return null
        }
        for (insn in insns) {
            val assignmentInsn = insn.nextRealInsn as? VarInsnNode ?: return null
            if (assignmentInsn.opcode < Opcodes.ISTORE) {
                // it's a load insn
                return null
            }
        }

        val result = insns.first()
        for (insn in insns) {
            if (insn.opcode != result.opcode || insn.owner != result.owner || insn.name != result.name || insn.desc != result.desc) {
                return null
            }
        }

        return result
    }

    private class ReplaceWithExpressionFix(
        at: PsiAnnotation,
        private val definitionId: String,
        private val isStatic: Boolean,
        private val argCount: Int,
    ) : LocalQuickFixOnPsiElement(at) {
        override fun getFamilyName() = "Replace with expression"
        override fun getText() = "Replace with expression"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val at = startElement as? PsiAnnotation ?: return
            val modifierList = AtResolver.findInjectorAnnotation(at)?.parent as? PsiModifierList ?: return
            val atTarget = at.findDeclaredAttributeValue("target") ?: return

            val factory = JavaPsiFacade.getElementFactory(project)

            val definition = factory.createAnnotationFromText(
                "@${MixinConstants.MixinExtras.DEFINITION}(id = \"$definitionId\")",
                at
            )
            definition.setDeclaredAttributeValue("method", atTarget)

            val exprText = buildString {
                append("? = ")
                if (!isStatic) {
                    append("?.")
                }
                append(definitionId)
                append("(")
                if (argCount > 0) {
                    append("?")
                    repeat(argCount - 1) {
                        append(", ?")
                    }
                }
                append(")")
            }
            val expression = factory.createAnnotationFromText(
                "@${MixinConstants.MixinExtras.EXPRESSION}(\"$exprText\")",
                at
            )

            val definitionPosRelativeToExpression =
                MinecraftProjectSettings.getInstance(project).definitionPosRelativeToExpression
            if (definitionPosRelativeToExpression == BeforeOrAfter.BEFORE) {
                modifierList.addAfter(expression, null)
                modifierList.addAfter(definition, null)
            } else {
                modifierList.addAfter(definition, null)
                modifierList.addAfter(expression, null)
            }

            val newAt = factory.createAnnotationFromText(
                "@${MixinConstants.Annotations.AT}(value = \"MIXINEXTRAS:EXPRESSION\", shift = ${MixinConstants.Classes.SHIFT}.AFTER)",
                at
            )
            at.replace(newAt)

            JavaCodeStyleManager.getInstance(project).shortenClassReferences(modifierList)
        }
    }
}
