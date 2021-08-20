/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.util.MixinMemberReference
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.shortName
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiSubstitutor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

object NewInsnTargetReference : TargetReference.Handler<PsiMember>() {

    override fun resolveTarget(context: PsiElement): PsiElement? {
        val value = context.constantStringValue ?: return null
        if (value.contains("(")) {
            val ref = MixinMemberReference.parse(value)
            ref?.owner ?: return null
            return ref.resolveMember(context.project, context.resolveScope)
        }
        if (!StringUtil.isJavaIdentifier(value.replace('.', '_').replace('$', '_'))) {
            return null
        }
        val name = value.replace('/', '.')
        return findQualifiedClass(name, context)
    }

    private fun parseReference(value: String): MemberReference? {
        if (value.contains("(")) {
            val ref = MixinMemberReference.parse(value)
            ref?.owner ?: return null
            if (ref.name != "<init>") return null
            return ref
        }
        if (!StringUtil.isJavaIdentifier(value.replace('.', '_').replace('$', '_'))) {
            return null
        }
        val name = value.replace('/', '.')
        return MemberReference("<init>", owner = name)
    }

    override fun createNavigationVisitor(context: PsiElement, targetClass: PsiClass): NavigationVisitor? {
        val value = context.constantStringValue ?: return null
        val ref = parseReference(value) ?: return null
        return MyNavigationVisitor(ref)
    }

    override fun createCollectVisitor(
        context: PsiElement,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiMember>? {
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, context.project, MemberReference(""))
        }
        val value = context.constantStringValue ?: return null
        val ref = parseReference(value) ?: return null
        return MyCollectVisitor(mode, context.project, ref)
    }

    override fun createLookup(targetClass: ClassNode, result: CollectVisitor.Result<PsiMember>): LookupElementBuilder? {
        when (val target = result.target) {
            is PsiClass -> {
                return JavaLookupElementBuilder.forClass(target, target.internalName)
                    .withPresentableText(target.shortName ?: return null)
            }
            is PsiMethod -> {
                val ownerName = result.qualifier?.substringAfterLast('.')?.replace('$', '.') ?: targetClass.shortName
                return JavaLookupElementBuilder.forMethod(
                    target,
                    MixinMemberReference.toString(target.getQualifiedMemberReference(result.qualifier)),
                    PsiSubstitutor.EMPTY,
                    null
                )
                    .setBoldIfInClass(target, targetClass)
                    .withPresentableText(ownerName + "." + target.internalName)
                    .withLookupString(target.internalName)
            }
            else -> return null
        }
    }

    private class MyNavigationVisitor(
        private val reference: MemberReference
    ) : NavigationVisitor() {
        override fun visitNewExpression(expression: PsiNewExpression) {
            val anonymousClass = expression.anonymousClass
            if (anonymousClass != null) {
                if (reference.matchOwner(anonymousClass)) {
                    addResult(expression)
                }
            } else {
                val ctor = expression.resolveConstructor()
                val containingClass = ctor?.containingClass
                if (ctor != null && containingClass != null) {
                    if (reference.match(ctor, containingClass)) {
                        addResult(expression)
                    }
                }
            }
            super.visitNewExpression(expression)
        }
    }

    private class MyCollectVisitor(
        mode: Mode,
        private val project: Project,
        private val reference: MemberReference
    ) : CollectVisitor<PsiMember>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            insns.iterator().forEachRemaining { insn ->
                if (insn !is TypeInsnNode) return@forEachRemaining
                if (insn.opcode != Opcodes.NEW) return@forEachRemaining
                if (mode != Mode.COMPLETION) {
                    val owner = reference.owner
                    if (owner != null && owner.replace('.', '/') != insn.desc) return@forEachRemaining
                }
                val initCall = findInitCall(insn) ?: return@forEachRemaining
                if (mode != Mode.COMPLETION) {
                    if (reference.descriptor != null && reference.descriptor != initCall.desc) return@forEachRemaining
                }

                val targetMethod = initCall.fakeResolve()
                addResult(
                    targetMethod.method.findOrConstructSourceMethod(
                        targetMethod.clazz,
                        project,
                        canDecompile = false
                    ),
                    qualifier = insn.desc.replace('/', '.')
                )
            }
        }

        private fun findInitCall(newInsn: TypeInsnNode): MethodInsnNode? {
            var matchingNewInsns = 0
            var insn: AbstractInsnNode? = newInsn
            while (insn != null) {
                when (insn) {
                    is TypeInsnNode -> {
                        if (insn.opcode == Opcodes.NEW && insn.desc == newInsn.desc) {
                            matchingNewInsns++
                        }
                    }
                    is MethodInsnNode -> {
                        if (insn.opcode == Opcodes.INVOKESPECIAL &&
                            insn.name == "<init>" &&
                            insn.owner == newInsn.desc
                        ) {
                            matchingNewInsns--
                            if (matchingNewInsns == 0) {
                                return insn
                            }
                        }
                    }
                }
                insn = insn.next
            }

            return null
        }
    }
}
