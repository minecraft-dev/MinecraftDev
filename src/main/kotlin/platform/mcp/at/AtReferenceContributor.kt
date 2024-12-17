/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtClassName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFieldName
import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtFunction
import com.demonwav.mcdev.platform.mcp.at.psi.AtElement
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.getPrimitiveWrapperClass
import com.demonwav.mcdev.util.memberReference
import com.demonwav.mcdev.util.nameAndParameterTypes
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.demonwav.mcdev.util.simpleQualifiedMemberReference
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ArrayUtil
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext

class AtReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(psiElement(AtClassName::class.java), AtClassNameReferenceProvider)
        registrar.registerReferenceProvider(psiElement(AtFieldName::class.java), AtFieldNameReferenceProvider)
        registrar.registerReferenceProvider(psiElement(AtFunction::class.java), AtFuncNameReferenceProvider)
    }
}

object AtClassNameReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        element as AtClassName

        val references = mutableListOf<PsiReference>()
        val fqn = element.text

        var partStart = 0
        while (true) {
            val partEnd = fqn.indexOf('.', partStart)
            if (partEnd == -1) {
                while (true) {
                    var outerEnd = fqn.indexOf('$', partStart)
                    if (outerEnd == -1) {
                        val range = TextRange(partStart, fqn.length)
                        references.add(AtClassNamePartReference(element, range, true))
                        break
                    } else {
                        val range = TextRange(partStart, outerEnd)
                        references.add(AtClassNamePartReference(element, range, true))
                    }

                    partStart = outerEnd + 1
                }

                break
            } else {
                val range = TextRange(partStart, partEnd)
                references.add(AtClassNamePartReference(element, range, false))
            }

            partStart = partEnd + 1
        }

        return references.toTypedArray()
    }
}

class AtClassNamePartReference(element: AtClassName, range: TextRange, val isClass: Boolean) :
    PsiReferenceBase<AtClassName>(element, range) {

    override fun resolve(): PsiElement? {
        val project = element.project
        val fqn = element.text.substring(0, rangeInElement.endOffset)
        val psiFacade = JavaPsiFacade.getInstance(project)
        if (isClass) {
            val scope = element.resolveScope
            if (fqn.contains('$')) {
                val outermostClass = psiFacade.findClass(fqn.substringBefore('$'), scope)
                if (outermostClass != null) {
                    val innerClassNames = fqn.substringAfter('$').split('$')
                    return innerClassNames.fold(outermostClass) { clazz, innerClassName ->
                        clazz.findInnerClassByName(innerClassName, false) ?: return null
                    }
                }
            } else {
                val containingPackage = psiFacade.findPackage(fqn.substringBeforeLast('.'))
                val clazz = containingPackage?.findClassByShortName(fqn.substringAfterLast('.'), scope)?.firstOrNull()
                if (clazz != null) {
                    return clazz
                }
            }
        }

        return psiFacade.findPackage(fqn)
    }

    override fun getVariants(): Array<out Any?> {
        val project = element.project
        val text = element.text
        if (text.contains('$')) {
            val classFqn = text.substringBeforeLast('$').replace('$', '.')
            val scope = element.resolveScope
            val clazz = JavaPsiFacade.getInstance(project).findClass(classFqn, scope)
            if (clazz != null) {
                return clazz.allInnerClasses.mapNotNull { JavaLookupElementBuilder.forClass(it) }.toTypedArray()
            }
        } else {
            val packFqn = text.substringBeforeLast('.')
            val pack = JavaPsiFacade.getInstance(project).findPackage(packFqn)
            if (pack != null) {
                val elements = mutableListOf<LookupElement>()
                pack.classes.filter { it.name != "package-info" }
                    .mapNotNullTo(elements) { JavaLookupElementBuilder.forClass(it) }
                pack.subPackages.mapNotNullTo(elements) { subPackage ->
                    LookupElementBuilder.create(subPackage)
                        .withIcon(subPackage.getIcon(Iconable.ICON_FLAG_VISIBILITY))
                }
                return elements.toTypedArray()
            }
        }

        return ArrayUtil.EMPTY_STRING_ARRAY
    }
}

abstract class AtClassMemberReference<E : AtElement>(element: E, range: TextRange) :
    PsiReferenceBase<E>(element, range) {

    override fun getVariants(): Array<out Any?> {
        val entry = element.parent as? AtEntry ?: return ArrayUtil.EMPTY_OBJECT_ARRAY

        val module = element.findModule() ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val instance = MinecraftFacet.getInstance(module) ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        val useSrg = instance.usesSrgMemberNames() == true
        val (mapField, mapMethod) = if (!useSrg) {
            { it: PsiField -> it.memberReference } to { it: PsiMethod -> it.memberReference }
        } else {
            val mcpModule = instance.getModuleOfType(McpModuleType)!!
            val srgMap = mcpModule.mappingsManager?.mappingsNow ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
            { it: PsiField -> srgMap.getIntermediaryField(it) } to { it: PsiMethod -> srgMap.getIntermediaryMethod(it) }
        }

        val results = mutableListOf<Any>()

        val entryClass = entry.className.classNameValue ?: return ArrayUtil.EMPTY_OBJECT_ARRAY
        for (field in entryClass.fields) {
            val memberReference = mapField(field) ?: field.simpleQualifiedMemberReference
            val lookupElement = LookupElementBuilder.create(memberReference.name)
                .withLookupStrings(listOf(field.name)) // Some fields don't appear in completion without this
                .withPsiElement(field)
                .withPresentableText(field.name)
                .withIcon(PlatformIcons.FIELD_ICON)
                .withTailText(" (${memberReference.name})".takeIf { useSrg }, true)
                .withInsertHandler(AtClassMemberInsertionHandler(field.name.takeIf { useSrg }))
            results.add(PrioritizedLookupElement.withPriority(lookupElement, 1.0))
        }

        for (method in entryClass.methods) {
            val memberReference = mapMethod(method) ?: method.qualifiedMemberReference
            val lookupElement = LookupElementBuilder.create(memberReference.name + memberReference.descriptor)
                .withLookupStrings(listOf(method.name)) // For symmetry with fields, might happen too
                .withPsiElement(method)
                .withPresentableText(method.nameAndParameterTypes)
                .withIcon(PlatformIcons.METHOD_ICON)
                .withTailText(" (${memberReference.name})".takeIf { useSrg }, true)
                .withInsertHandler(AtClassMemberInsertionHandler(method.name.takeIf { useSrg }))
            results.add(PrioritizedLookupElement.withPriority(lookupElement, 0.0))
        }

        return results.toTypedArray()
    }
}

object AtFieldNameReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> = arrayOf(AtFieldNameReference(element as AtFieldName))
}

class AtFieldNameReference(element: AtFieldName) :
    AtClassMemberReference<AtFieldName>(element, TextRange(0, element.text.length)) {

    override fun resolve(): PsiElement? {
        val entry = element.parent as? AtEntry ?: return null
        val entryClass = entry.className?.classNameValue ?: return null

        val module = element.findModule() ?: return null
        val instance = MinecraftFacet.getInstance(module) ?: return null
        val mcpModule = instance.getModuleOfType(McpModuleType) ?: return null

        return if (instance.usesSrgMemberNames() != true) {
            entryClass.findFieldByName(element.text, false)
        } else {
            val srgMap = mcpModule.mappingsManager?.mappingsNow ?: return null
            val reference = srgMap.getMappedField(AtMemberReference.get(entry, element) ?: return null)
            reference.resolveMember(module.project)
        }
    }
}

object AtFuncNameReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<out PsiReference?> {
        val func = element as AtFunction
        val references = mutableListOf<PsiReference>(AtFuncNameReference(func))

        element.argumentList.mapTo(references) { AtClassValueReference(func, it) }

        references.add(AtClassValueReference(element, element.returnValue))

        return references.toTypedArray()
    }
}

class AtFuncNameReference(element: AtFunction) :
    AtClassMemberReference<AtFunction>(element, element.funcName.textRangeInParent) {

    override fun resolve(): PsiElement? {
        val entry = element.parent as? AtEntry ?: return null
        val entryClass = entry.className?.classNameValue ?: return null

        val module = element.findModule() ?: return null
        val instance = MinecraftFacet.getInstance(module) ?: return null
        val mcpModule = instance.getModuleOfType(McpModuleType) ?: return null

        return if (instance.usesSrgMemberNames() != true) {
            val memberReference = MemberReference.parse(element.text) ?: return null
            entryClass.findMethods(memberReference).firstOrNull()
        } else {
            val srgMap = mcpModule.mappingsManager?.mappingsNow ?: return null
            val reference = srgMap.getMappedMethod(AtMemberReference.get(entry, element) ?: return null)
            reference.resolveMember(module.project)
        }
    }
}

class AtClassValueReference(val element: AtFunction, val argument: AtElement) :
    PsiReferenceBase<AtFunction>(element, argument.textRangeInParent, false) {

    override fun resolve(): PsiElement? {
        val text = argument.text.substringAfterLast('[')
        return when (val c = text[0]) {
            'L' -> if (!text.contains('.')) {
                findQualifiedClass(element.project, text.substring(1, text.length - 1).replace('/', '.'))
            } else {
                null
            }

            else -> getPrimitiveWrapperClass(c, element.project)
        }
    }
}

private class AtClassMemberInsertionHandler(val memberName: String?) : InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val line = context.document.getLineNumber(context.tailOffset)
        context.document.deleteString(context.tailOffset, context.document.getLineEndOffset(line))

        if (memberName != null) {
            val comment = " # $memberName"
            context.document.insertString(context.editor.caretModel.offset, comment)
            context.editor.caretModel.moveCaretRelatively(comment.length, 0, false, false, false)
        }
    }
}
