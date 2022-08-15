/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceField
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayAccessExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.util.PsiUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

class FieldInjectionPoint : QualifiedInjectionPoint<PsiField>() {
    companion object {
        private val VALID_OPCODES = setOf(Opcodes.GETFIELD, Opcodes.GETSTATIC, Opcodes.PUTFIELD, Opcodes.PUTSTATIC)
    }

    private fun getArrayAccessType(args: Map<String, String>): ArrayAccessType? {
        return when (args["array"]) {
            "length" -> ArrayAccessType.LENGTH
            "get" -> ArrayAccessType.GET
            "set" -> ArrayAccessType.SET
            else -> null
        }
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor? {
        val opcode = (at.findDeclaredAttributeValue("opcode")?.constantValue as? Int)
            ?.takeIf { it in VALID_OPCODES } ?: -1
        val args = AtResolver.getArgs(at)
        val arrayAccess = getArrayAccessType(args)
        return target?.let { MyNavigationVisitor(targetClass, it, opcode, arrayAccess) }
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiField>? {
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, at.project, MemberReference(""), -1, null, 8)
        }
        val opcode = (at.findDeclaredAttributeValue("opcode")?.constantValue as? Int)
            ?.takeIf { it in VALID_OPCODES } ?: -1
        val args = AtResolver.getArgs(at)
        val arrayAccess = getArrayAccessType(args)
        val fuzz = args["fuzz"]?.toIntOrNull()?.coerceIn(1, 32) ?: 8
        return target?.let { MyCollectVisitor(mode, at.project, it, opcode, arrayAccess, fuzz) }
    }

    override fun createLookup(targetClass: ClassNode, m: PsiField, owner: String): LookupElementBuilder {
        return JavaLookupElementBuilder.forField(
            m,
            m.getQualifiedMemberReference(owner).toMixinString(),
            null
        )
            .setBoldIfInClass(m, targetClass)
            .withPresentableText(m.name)
            .withLookupString(m.name)
    }

    private class MyNavigationVisitor(
        private val targetClass: PsiClass,
        private val selector: MixinSelector,
        private val opcode: Int,
        private val arrayAccess: ArrayAccessType?
    ) : NavigationVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            if (expression !is PsiMethodReferenceExpression) {
                // early out for if the name does not match
                val name = expression.referenceName
                if (name == null || selector.canEverMatch(name)) {
                    (expression.resolve() as? PsiField)?.let { resolved ->
                        var matches = selector.matchField(
                            resolved,
                            QualifiedMember.resolveQualifier(expression) ?: targetClass
                        )
                        if (matches && opcode != -1) {
                            // check if we match the opcode
                            val isStatic = opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC
                            if (isStatic != resolved.hasModifierProperty(PsiModifier.STATIC)) {
                                matches = false
                            } else {
                                val isWrite = opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC
                                if (isWrite && !PsiUtil.isAccessedForWriting(expression)) {
                                    matches = false
                                } else if (!isWrite && !PsiUtil.isAccessedForReading(expression)) {
                                    matches = false
                                }
                            }
                        }
                        if (matches) {
                            // figure out where the array access is.
                            // ignore fuzz, I don't even want to think about that in source code
                            val actualResult = when (arrayAccess) {
                                ArrayAccessType.LENGTH -> {
                                    val parentRef = PsiUtil.skipParenthesizedExprUp(expression.parent)
                                        as? PsiReferenceExpression ?: return
                                    parentRef.takeIf { it.referenceName == "length" }
                                }
                                ArrayAccessType.GET -> {
                                    val parentArrayAccess = PsiUtil.skipParenthesizedExprUp(expression.parent)
                                        as? PsiArrayAccessExpression ?: return
                                    parentArrayAccess.takeIf(PsiUtil::isAccessedForReading)
                                }
                                ArrayAccessType.SET -> {
                                    val parentArrayAccess = PsiUtil.skipParenthesizedExprUp(expression.parent)
                                        as? PsiArrayAccessExpression ?: return
                                    parentArrayAccess.takeIf(PsiUtil::isAccessedForWriting)
                                }
                                null -> expression
                            } ?: return

                            addResult(actualResult)
                        }
                    }
                }
            }

            super.visitReferenceExpression(expression)
        }
    }

    private class MyCollectVisitor(
        mode: Mode,
        private val project: Project,
        private val selector: MixinSelector,
        private val opcode: Int,
        private val arrayAccess: ArrayAccessType?,
        private val fuzz: Int
    ) : CollectVisitor<PsiField>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            insns.iterator().forEachRemaining { insn ->
                if (insn !is FieldInsnNode) return@forEachRemaining
                if (mode != Mode.COMPLETION) {
                    if (opcode != -1 && opcode != insn.opcode) {
                        return@forEachRemaining
                    }
                    if (!selector.matchField(insn.owner, insn.name, insn.desc)) {
                        return@forEachRemaining
                    }
                }
                val actualInsn = if (arrayAccess == null) {
                    insn
                } else {
                    findArrayInsn(insn, arrayAccess)
                } ?: return@forEachRemaining
                val fieldNode = insn.fakeResolve()
                val psiField = fieldNode.field.findOrConstructSourceField(
                    fieldNode.clazz,
                    project,
                    canDecompile = false
                )
                addResult(actualInsn, psiField, qualifier = insn.owner.replace('/', '.'))
            }
        }

        private fun findArrayInsn(fieldInsn: FieldInsnNode, arrayAccess: ArrayAccessType): AbstractInsnNode? {
            val arrayType = Type.getType(fieldInsn.desc)
            if (arrayType.sort != Type.ARRAY) {
                return null
            }
            val wantedOpcode = when (arrayAccess) {
                ArrayAccessType.LENGTH -> Opcodes.ARRAYLENGTH
                ArrayAccessType.GET -> arrayType.elementType.getOpcode(Opcodes.IALOAD)
                ArrayAccessType.SET -> arrayType.elementType.getOpcode(Opcodes.IASTORE)
            }

            var insn = fieldInsn.next
            var pos = 0
            while (insn != null) {
                if (insn.opcode == wantedOpcode) {
                    return insn
                }
                if (insn.opcode == Opcodes.ARRAYLENGTH && pos == 0) {
                    return null
                }
                if (insn is FieldInsnNode &&
                    insn.owner == fieldInsn.owner &&
                    insn.name == fieldInsn.name &&
                    insn.desc == fieldInsn.desc
                ) {
                    return null
                }
                if (pos > fuzz) {
                    return null
                }
                pos++
                insn = insn.next
            }

            return null
        }
    }

    private enum class ArrayAccessType {
        LENGTH, GET, SET
    }
}
