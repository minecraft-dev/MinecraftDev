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

package com.demonwav.mcdev.platform.mixin.reference.target

import com.demonwav.mcdev.platform.mixin.expression.MEExpressionMatchUtil
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.reference.MixinReference
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingModifierList
import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.reference.PolyReferenceResolver
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMember
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.sequenceOfNotNull
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.LMFInfo
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode

abstract class AbstractDefinitionReference : PolyReferenceResolver(), MixinReference {
    abstract fun getFullReferenceIfMatches(memberReference: MemberReference, node: FlowValue): MemberReference?
    abstract fun getMatchesInClass(memberReference: MemberReference, clazz: PsiClass): Sequence<PsiMember>
    abstract fun referenceToString(memberReference: MemberReference): String

    override fun isUnresolved(context: PsiElement) = resolveInBytecode(context).isNotEmpty()

    override fun isValidAnnotation(name: String, project: Project) = name == MixinConstants.MixinExtras.DEFINITION

    override fun resolveReference(context: PsiElement): Array<ResolveResult> {
        return resolveForNavigation(context).mapToArray(::PsiElementResolveResult)
    }

    fun resolveForNavigation(context: PsiElement): Array<PsiElement> {
        val project = context.project
        val facade = JavaPsiFacade.getInstance(project)
        return resolveInBytecode(context).asSequence().flatMap { memberReference ->
            val ownerClass = facade.findClass(
                memberReference.owner!!.replace('$', '.'),
                GlobalSearchScope.allScope(project)
            ) ?: return@flatMap emptySequence()
            getMatchesInClass(memberReference.withoutOwner, ownerClass)
        }.toTypedArray()
    }

    override fun collectVariants(context: PsiElement) =
        resolveInBytecode(
            context,
            MemberReference("*", null, null, matchAllNames = true, matchAllDescs = true)
        ).mapToArray<MemberReference, Any> {
            LookupElementBuilder.create(referenceToString(it))
                .withPresentableText(it.presentableText)
                .withLookupString(it.name)
        }

    fun resolveInBytecode(context: PsiElement): List<MemberReference> {
        val memberReference = context.constantStringValue?.let(MemberReference::parse) ?: return emptyList()
        return resolveInBytecode(context, memberReference)
    }

    private fun resolveInBytecode(context: PsiElement, memberReference: MemberReference): List<MemberReference> {
        val project = context.project
        val modifierList = context.findContainingModifierList() ?: return emptyList()
        val annotation = modifierList.annotations.firstOrNull {
            MixinAnnotationHandler.forMixinAnnotation(it, project) != null
        } ?: return emptyList()

        val result = mutableListOf<MemberReference>()

        for (target in MixinAnnotationHandler.resolveTarget(annotation)) {
            if (target !is MethodTargetMember) {
                continue
            }

            if (target.classAndMethod.method.instructions == null) {
                continue
            }

            val flow = MEExpressionMatchUtil.getFlowMap(
                project,
                target.classAndMethod.clazz,
                target.classAndMethod.method
            ) ?: continue

            for (node in flow.values) {
                val fullReference = getFullReferenceIfMatches(memberReference, node) ?: continue
                result += fullReference
            }
        }

        return result
    }
}

object FieldDefinitionReference : AbstractDefinitionReference() {
    val ELEMENT_PATTERN = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(MixinConstants.MixinExtras.DEFINITION, "field")

    override fun getFullReferenceIfMatches(memberReference: MemberReference, node: FlowValue): MemberReference? {
        val insn = node.insn
        if (insn !is FieldInsnNode || !memberReference.matchField(insn.owner, insn.name, insn.desc)) {
            return null
        }

        return MemberReference(insn.name, insn.desc, insn.owner.replace('/', '.'))
    }

    override fun getMatchesInClass(memberReference: MemberReference, clazz: PsiClass) =
        sequenceOfNotNull(clazz.findField(memberReference, checkBases = true))

    override fun referenceToString(memberReference: MemberReference) =
        "L${memberReference.owner?.replace('.', '/')};${memberReference.name}:${memberReference.descriptor}"

    override val description = "defined field '%s'"
}

object MethodDefinitionReference : AbstractDefinitionReference() {
    val ELEMENT_PATTERN = PsiJavaPatterns.psiLiteral(StandardPatterns.string())
        .insideAnnotationAttribute(MixinConstants.MixinExtras.DEFINITION, "method")

    override fun getFullReferenceIfMatches(memberReference: MemberReference, node: FlowValue): MemberReference? {
        val info = node.getDecoration<LMFInfo>(FlowDecorations.LMF_INFO)
        val insn = node.insn
        val (owner, name, desc) = when {
            info != null && (info.type == LMFInfo.Type.FREE_METHOD || info.type == LMFInfo.Type.BOUND_METHOD) ->
                Triple(info.impl.owner, info.impl.name, info.impl.desc)

            insn is MethodInsnNode -> Triple(insn.owner, insn.name, insn.desc)
            else -> return null
        }
        if (!memberReference.matchMethod(owner, name, desc)) {
            return null
        }

        return MemberReference(name, desc, owner.replace('/', '.'))
    }

    override fun getMatchesInClass(memberReference: MemberReference, clazz: PsiClass) =
        clazz.findMethods(memberReference, checkBases = true)

    override fun referenceToString(memberReference: MemberReference) =
        "L${memberReference.owner?.replace('.', '/')};${memberReference.name}${memberReference.descriptor}"

    override val description = "defined method '%s'"
}
