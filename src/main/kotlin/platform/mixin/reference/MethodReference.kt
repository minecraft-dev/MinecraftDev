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

package com.demonwav.mcdev.platform.mixin.reference

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MemberInfo
import com.demonwav.mcdev.platform.mixin.util.findMethods
import com.demonwav.mcdev.platform.mixin.util.findReferences
import com.demonwav.mcdev.platform.mixin.util.memberReference
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.Quantifier
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.insideAnnotationAttribute
import com.demonwav.mcdev.util.reference.completeToLiteral
import com.demonwav.mcdev.util.toTypedArray
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.util.ProcessingContext
import org.objectweb.asm.tree.ClassNode

object MethodReference : AbstractMethodReference() {
    val ELEMENT_PATTERN: ElementPattern<PsiLiteral> =
        PsiJavaPatterns.psiLiteral(StandardPatterns.string()).withAncestor(
            1,
            PsiJavaPatterns.psiElement().insideAnnotationAttribute(
                PsiJavaPatterns.psiAnnotation().with(
                    object : PatternCondition<PsiAnnotation>("injector") {
                        override fun accepts(t: PsiAnnotation, context: ProcessingContext?): Boolean {
                            val qName = t.qualifiedName ?: return false
                            return isValidAnnotation(qName, t.project)
                        }
                    },
                ),
                "method",
            ),
        )

    override val description = "method '%s' in target class"

    override fun isValidAnnotation(name: String, project: Project) =
        MixinAnnotationHandler.forMixinAnnotation(name, project) is InjectorAnnotationHandler

    override fun parseSelector(context: PsiElement): MixinSelector? {
        return parseMixinSelector(context)
    }

    override fun parseSelector(stringValue: String, context: PsiElement): MixinSelector? {
        return parseMixinSelector(stringValue, context)
    }

    override fun isUnresolved(context: PsiElement): Boolean {
        return if (super.isUnresolved(context)) {
            val stringValue = context.constantStringValue ?: return true
            !isMiscDynamicSelector(context.project, stringValue)
        } else {
            false
        }
    }

    override fun getSuggestions(context: PsiElement, targets: Collection<ClassNode>): Array<Any> {
        val value = context.constantStringValue ?: return emptyArray()
        if (value.trimStart().startsWith('@')) {
            return getDynamicSuggestions(context)
        }
        val info = MemberInfo.parse(value) ?: return emptyArray()
        if (isCompleting(info.headToString())) {
            return getRootSuggestions(context, targets, info.tailToString())
        }
        return getNestedSuggestions(context, targets, info)
    }

    private fun getDynamicSuggestions(context: PsiElement): Array<Any> = buildList {
        for (dynamicSelector in MixinSelectorParser.EP_NAME.extensionList) {
            if (dynamicSelector !is DynamicSelectorParser) {
                continue
            }
            val lookupString = "@${dynamicSelector.id}"
            add(LookupElementBuilder.create(lookupString).completeDynamicSelector(context))
        }
    }.toTypedArray()

    private fun getRootSuggestions(
        context: PsiElement,
        targets: Collection<ClassNode>,
        tail: String,
    ): Array<Any> {
        val result = mutableListOf<LookupElement>()

        val selector = if (tail.isNotBlank()) {
            MemberInfo(quantifier = Quantifier.Any, nextDepth = Quantifier.Exact(0, 0), next = MemberInfo())
        } else {
            MemberInfo(quantifier = Quantifier.Any)
        }
        val groupedMethods = targets.asSequence()
            .flatMap { target ->
                target.findMethods(listOf(selector), allowStatic = true)
                    .map { ClassAndMethodNode(target, it) }
            }
            .groupBy { it.method.memberReference }
            .values

        // All methods which are not unique by their name need to be qualified with the descriptor
        val visitedMethods = HashSet<String>()
        val uniqueMethods = HashSet<String>()

        val allMethods = ArrayList<ClassAndMethodNode>(groupedMethods.size)

        for (methods in groupedMethods) {
            val firstMethod = methods.first()
            val name = firstMethod.method.name
            if (visitedMethods.add(name)) {
                uniqueMethods.add(name)
            } else {
                uniqueMethods.remove(name)
            }

            // If we have a method with the same name and descriptor in at least
            // as many classes as targets it should be present in all of them.
            // Not sure how you would have more methods than targets but who cares.
            if (methods.size >= targets.size) {
                allMethods.add(firstMethod)
            }
        }

        for (m in allMethods) {
            val targetMethodInfo = if (m.method.name in uniqueMethods) {
                MemberReference(m.method.name)
            } else {
                m.method.memberReference
            }
            val customLiteral = if (tail.isNotBlank()) {
                targetMethodInfo.toMixinString() + tail
            } else {
                null
            }

            result.add(methodLookupBuilder(m, targetMethodInfo, context).completeToLiteral(context, customLiteral))
        }

        return result.toTypedArray()
    }

    private fun getNestedSuggestions(
        context: PsiElement,
        targets: Collection<ClassNode>,
        info: MemberInfo,
    ): Array<Any> {
        val parents = mutableListOf<MemberInfo>()
        var currentInfo = info
        while (true) {
            parents.add(currentInfo.copy(next = null))
            currentInfo = currentInfo.next ?: return emptyArray()
            if (isCompleting(currentInfo.headToString())) {
                break
            }
        }
        val component = when {
            isCompleting(currentInfo.owner.orEmpty()) ->
                NestedComponent.Owner(currentInfo)

            isCompleting(currentInfo.name.orEmpty()) ->
                if (currentInfo.owner == null) {
                    NestedComponent.Owner(currentInfo.copy(name = null))
                } else {
                    NestedComponent.Name(currentInfo)
                }

            isCompleting(currentInfo.descriptor.orEmpty()) ->
                NestedComponent.Desc(currentInfo)

            else ->
                return emptyArray()
        }
        return getNestedSuggestions(context, targets, parents, component)
    }

    private fun getNestedSuggestions(
        context: PsiElement,
        targets: Collection<ClassNode>,
        parents: List<MemberInfo>,
        component: NestedComponent,
    ): Array<Any> {
        val headSelector = parents.foldRight(component.searchInfo()) { it, acc -> it.copy(next = acc) }
        val candidates = targets.asSequence()
            .flatMap { target ->
                target.findReferences(listOf(headSelector), allowStatic = true)
                    .map { component.extractCandidate(it) to target }
                    .distinctBy { it.first }
            }
            .groupBy({ it.first }, { it.second })
            .asSequence()
            .filter { it.value.size == targets.size }
            .map { it.key }
        return candidates.mapNotNull { candidate ->
            val tailInfo = component.suggestedInfo(candidate)
            val newInfoPrefix = parents.foldRight(tailInfo.copy(next = null)) { it, acc -> it.copy(next = acc) }
            val newInfo = parents.foldRight(tailInfo) { it, acc -> it.copy(next = acc) }

            component.lookupBuilder(candidate, newInfoPrefix.toMixinString(), context)
                ?.completeToLiteral(context, customLiteral = newInfo.toMixinString())
        }.toTypedArray()
    }

    private fun LookupElementBuilder.completeDynamicSelector(context: PsiElement): LookupElementBuilder {
        val id = lookupString.removePrefix("@")
        val parser = MixinSelectorParser.EP_NAME.extensionList.asSequence()
            .filterIsInstance<DynamicSelectorParser>()
            .firstOrNull { id in it.validIds } ?: return completeToLiteral(context)

        return completeToLiteral(context) { editor, element ->
            parser.onCompleted(editor, element)
        }
    }

    private fun isCompleting(string: String) = CompletionUtil.DUMMY_IDENTIFIER_TRIMMED in string

    private sealed interface NestedComponent {
        val info: MemberInfo

        fun searchInfo(): MemberInfo

        fun extractCandidate(match: MemberReference): String

        fun suggestedInfo(candidate: String): MemberInfo

        fun lookupBuilder(candidate: String, lookupString: String, context: PsiElement): LookupElementBuilder? {
            return LookupElementBuilder.create(lookupString)
                .withPresentableText(candidate)
                .withIcon(AllIcons.Nodes.AbstractMethod)
        }

        data class Owner(override val info: MemberInfo) : NestedComponent {
            override fun searchInfo() = info.copy(owner = null, next = null)

            override fun extractCandidate(match: MemberReference) = match.owner!!

            override fun suggestedInfo(candidate: String) = info.copy(owner = candidate)

            override fun lookupBuilder(
                candidate: String,
                lookupString: String,
                context: PsiElement,
            ): LookupElementBuilder? {
                val source = findQualifiedClass(candidate, context) ?: return null

                return JavaLookupElementBuilder.forClass(source, lookupString, true)
                    .withPresentableText(candidate.substringAfterLast('.'))
            }
        }

        data class Name(override val info: MemberInfo) : NestedComponent {
            override fun searchInfo() = info.copy(name = null, next = null)

            override fun extractCandidate(match: MemberReference) = match.name

            override fun suggestedInfo(candidate: String) = info.copy(name = candidate)
        }

        data class Desc(override val info: MemberInfo) : NestedComponent {
            override fun searchInfo() = info.copy(descriptor = null, next = null)

            override fun extractCandidate(match: MemberReference) = match.descriptor!!

            override fun suggestedInfo(candidate: String) = info.copy(descriptor = candidate)
        }
    }
}
