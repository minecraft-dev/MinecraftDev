/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.MixinSelectorParser
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.AT
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByPsiClass
import com.demonwav.mcdev.platform.mixin.util.findMethod
import com.demonwav.mcdev.platform.mixin.util.shortName
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.shortName
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.util.parentOfType
import com.intellij.util.ArrayUtilRt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode

class NewInsnInjectionPoint : InjectionPoint<PsiMember>() {
    override fun onCompleted(editor: Editor, reference: PsiLiteral) {
        completeExtraStringAtAttribute(editor, reference, "target")
    }

    override fun getArgsKeys(at: PsiAnnotation) = ARGS_KEYS

    override fun getArgsValues(at: PsiAnnotation, key: String): Array<Any> {
        if (key != "class") {
            return ArrayUtilRt.EMPTY_OBJECT_ARRAY
        }

        val injectorAnnotation = AtResolver.findInjectorAnnotation(at) ?: return ArrayUtilRt.EMPTY_OBJECT_ARRAY

        return MixinAnnotationHandler.resolveTarget(injectorAnnotation).asSequence()
            .filterIsInstance<MethodTargetMember>()
            .flatMap { target ->
                target.classAndMethod.method.instructions?.asSequence()?.mapNotNull { insn ->
                    (insn as? TypeInsnNode)?.desc?.takeIf { insn.opcode == Opcodes.NEW }
                } ?: emptySequence()
            }
            .toTypedArray()
    }

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
        targetClass: PsiClass,
    ): NavigationVisitor? {
        return getTarget(at, target)?.let { MyNavigationVisitor(it) }
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
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
                val descriptorArgs = target.descriptor?.dropLast(1) ?: return null
                val qualifierInternalName = result.qualifier?.replace('.', '/')
                return JavaLookupElementBuilder.forMethod(
                    target,
                    "${descriptorArgs}L$qualifierInternalName;",
                    PsiSubstitutor.EMPTY,
                    null,
                )
                    .setBoldIfInClass(target, targetClass)
                    .withPresentableText(ownerName + "." + target.internalName)
                    .withLookupString(target.internalName)
            }
            else -> return null
        }
    }

    private class MyNavigationVisitor(
        private val selector: MixinSelector,
    ) : NavigationVisitor() {
        override fun visitNewExpression(expression: PsiNewExpression) {
            val anonymousClass = expression.anonymousClass
            val anonymousName = anonymousClass?.fullQualifiedName?.replace('.', '/')
            if (anonymousName != null) {
                val method = findClassNodeByPsiClass(anonymousClass)?.findMethod(selector)
                if (method != null && selector.matchMethod(anonymousName, method.name, method.desc)) {
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
        private val selector: MixinSelector,
    ) : CollectVisitor<PsiMember>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return
            insns.iterator().forEachRemaining { insn ->
                if (insn !is TypeInsnNode) return@forEachRemaining
                if (insn.opcode != Opcodes.NEW) return@forEachRemaining
                val initCall = findInitCall(insn) ?: return@forEachRemaining

                val sourceMethod = nodeMatchesSelector(initCall, mode, selector, project) ?: return@forEachRemaining
                addResult(
                    insn,
                    sourceMethod,
                    qualifier = initCall.owner.replace('/', '.'),
                )
            }
        }
    }

    companion object {
        private val ARGS_KEYS = arrayOf("class")

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

        val strippedValue = value.replace(" ", "")
        return if (strippedValue.startsWith('(')) {
            NewInsnDescriptorSelector(strippedValue)
        } else {
            NewInsnTypeSelector(strippedValue.removeSurrounding("L", ";"))
        }
    }
}

private class NewInsnTypeSelector(
    override val owner: String,
) : MixinSelector {
    override fun matchField(owner: String, name: String, desc: String) = false

    override fun matchMethod(owner: String, name: String, desc: String): Boolean {
        return name == "<init>" && owner == this.owner
    }

    override val fieldDescriptor = null
    override val methodDescriptor = null
    override val displayName = owner
}

private class NewInsnDescriptorSelector(
    override val methodDescriptor: String,
) : MixinSelector {
    override fun matchField(owner: String, name: String, desc: String): Boolean = false

    override fun matchMethod(owner: String, name: String, desc: String): Boolean {
        if (name != "<init>" || desc.last() != 'V') {
            return false
        }

        val lastParen = methodDescriptor.lastIndexOf(')')
        val argsDesc = methodDescriptor.substring(0, lastParen + 1)
        val descRet = methodDescriptor.substringAfterLast(')').removeSurrounding("L", ";")
        return desc.dropLast(1) == argsDesc && descRet == owner
    }

    override val owner = null
    override val fieldDescriptor = null
    override val displayName = methodDescriptor
}

private fun classToMemberReference(value: String): MemberReference? {
    val fqn = value.replace('/', '.')
    if (fqn.isNotEmpty() && !fqn.startsWith('.') && !fqn.endsWith('.') && !fqn.contains("..")) {
        if (StringUtil.isJavaIdentifier(fqn.replace('.', '_'))) {
            return MemberReference("<init>", owner = fqn)
        }
    }

    return null
}
