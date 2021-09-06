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

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.parseMixinSelector
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceField
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

object FieldTargetReference : TargetReference.QualifiedHandler<PsiField>() {
    override fun createNavigationVisitor(context: PsiElement, targetClass: PsiClass): NavigationVisitor? {
        return parseMixinSelector(context)
            ?.let { MyNavigationVisitor(targetClass, it) }
    }

    override fun createCollectVisitor(
        context: PsiElement,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiField>? {
        if (mode == CollectVisitor.Mode.COMPLETION) {
            return MyCollectVisitor(mode, context.project, MemberReference(""))
        }
        return parseMixinSelector(context)
            ?.let { MyCollectVisitor(mode, context.project, it) }
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
        private val selector: MixinSelector
    ) : NavigationVisitor() {
        override fun visitReferenceExpression(expression: PsiReferenceExpression) {
            if (expression !is PsiMethodReferenceExpression) {
                // early out for if the name does not match
                val name = expression.referenceName
                if (name == null || selector.canEverMatch(name)) {
                    (expression.resolve() as? PsiField)?.let { resolved ->
                        val matches = selector.matchField(
                            resolved,
                            QualifiedMember.resolveQualifier(expression) ?: targetClass
                        )
                        if (matches) {
                            addResult(expression)
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
        private val selector: MixinSelector
    ) : CollectVisitor<PsiField>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            insns.iterator().forEachRemaining { insn ->
                if (insn !is FieldInsnNode) return@forEachRemaining
                if (mode != Mode.COMPLETION) {
                    if (!selector.matchField(insn.owner, insn.name, insn.desc)) {
                        return@forEachRemaining
                    }
                }
                val fieldNode = insn.fakeResolve()
                val psiField = fieldNode.field.findOrConstructSourceField(
                    fieldNode.clazz,
                    project,
                    canDecompile = false
                )
                addResult(psiField, qualifier = insn.owner.replace('/', '.'))
            }
        }
    }
}
