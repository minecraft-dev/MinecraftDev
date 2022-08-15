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
import com.demonwav.mcdev.platform.mixin.reference.MixinSelectorParser
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.shortName
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.parentOfType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

class NewInsnInjectionPoint : InjectionPoint<PsiMember>() {
    private fun getTarget(at: PsiAnnotation, target: MixinSelector?): MixinSelector? {
        if (target != null) {
            return target
        }
        val clazz = AtResolver.getArgs(at)["class"] ?: return null
        return classToMemberReference(clazz)
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor? {
        return getTarget(at, target)?.let { MyNavigationVisitor(it) }
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiMember>? {
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, at.project, MemberReference(""))
        }
        return getTarget(at, target)?.let { MyCollectVisitor(mode, at.project, it) }
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
                    target.getQualifiedMemberReference(result.qualifier).toMixinString(),
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
        private val selector: MixinSelector
    ) : NavigationVisitor() {
        override fun visitNewExpression(expression: PsiNewExpression) {
            val anonymousName = expression.anonymousClass?.fullQualifiedName?.replace('.', '/')
            if (anonymousName != null) {
                // guess descriptor
                val hasThis = expression.parentOfType<PsiMethod>()?.hasModifierProperty(PsiModifier.STATIC) == false
                val thisType = if (hasThis) expression.parentOfType<PsiClass>()?.internalName else null
                val argTypes = expression.argumentList?.expressionTypes?.map { it.descriptor } ?: emptyList()
                val bytecodeArgTypes = if (thisType != null) listOf(thisType) + argTypes else argTypes
                val methodDesc = Type.getMethodDescriptor(
                    Type.VOID_TYPE,
                    *bytecodeArgTypes.mapToArray { Type.getType(it) }
                )
                if (selector.matchMethod(anonymousName, "<init>", methodDesc)) {
                    addResult(expression)
                }
            } else {
                val ctor = expression.resolveConstructor()
                val containingClass = ctor?.containingClass
                if (ctor != null && containingClass != null) {
                    if (selector.matchMethod(ctor, containingClass)) {
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
        private val selector: MixinSelector
    ) : CollectVisitor<PsiMember>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            insns.iterator().forEachRemaining { insn ->
                if (insn !is TypeInsnNode) return@forEachRemaining
                if (insn.opcode != Opcodes.NEW) return@forEachRemaining
                val initCall = findInitCall(insn) ?: return@forEachRemaining
                if (mode != Mode.COMPLETION) {
                    if (!selector.matchMethod(initCall.owner, initCall.name, initCall.desc)) {
                        return@forEachRemaining
                    }
                }

                val targetMethod = initCall.fakeResolve()
                addResult(
                    insn,
                    targetMethod.method.findOrConstructSourceMethod(
                        targetMethod.clazz,
                        project,
                        canDecompile = false
                    ),
                    qualifier = insn.desc.replace('/', '.')
                )
            }
        }
    }

    companion object {
        fun findInitCall(newInsn: TypeInsnNode): MethodInsnNode? {
            var newInsns = 0
            var insn: AbstractInsnNode? = newInsn
            while (insn != null) {
                when (insn) {
                    is TypeInsnNode -> {
                        if (insn.opcode == Opcodes.NEW) {
                            newInsns++
                        }
                    }
                    is MethodInsnNode -> {
                        if (insn.opcode == Opcodes.INVOKESPECIAL && insn.name == "<init>") {
                            newInsns--
                            if (newInsns == 0) {
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

class NewInsnSelectorParser : MixinSelectorParser {
    override fun parse(value: String, context: PsiElement): MixinSelector? {
        // check we're inside NEW
        val at = context.parentOfType<PsiAnnotation>() ?: return null
        if (!at.hasQualifiedName(AT)) return null
        if (at.findAttributeValue("value")?.constantStringValue != "NEW") return null

        return classToMemberReference(value)
    }
}

private fun classToMemberReference(value: String): MemberReference? {
    val fqn = value.replace('/', '.').replace('$', '.')
    if (fqn.isNotEmpty() && !fqn.startsWith('.') && !fqn.endsWith('.') && !fqn.contains("..")) {
        if (StringUtil.isJavaIdentifier(fqn.replace('.', '_'))) {
            return MemberReference("<init>", owner = fqn)
        }
    }

    return null
}
