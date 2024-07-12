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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.mcp.McpModule
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.impl.OrderEntryUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementManipulator
import com.intellij.psi.ElementManipulators
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiEllipsisType
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiKeyword
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifier.ModifierConstant
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiType
import com.intellij.psi.ResolveResult
import com.intellij.psi.filters.ElementFilter
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.psi.util.parentOfType
import com.intellij.refactoring.changeSignature.ChangeSignatureUtil
import com.intellij.util.IncorrectOperationException
import com.siyeh.ig.psiutils.ImportUtils
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

// Parent
fun PsiElement.findModule(): Module? = ModuleUtilCore.findModuleForPsiElement(this)

fun PsiElement.findContainingClass(): PsiClass? = findParent(resolveReferences = false)

fun PsiElement.findReferencedClass(): PsiClass? = findParent(resolveReferences = true)

fun PsiElement.findReferencedMember(): PsiMember? = findParent(resolveReferences = true) { it is PsiClass }

fun PsiElement.findContainingMember(): PsiMember? = findParent(resolveReferences = false) { it is PsiClass }

fun PsiElement.findContainingMethod(): PsiMethod? = findParent(resolveReferences = false) { it is PsiClass }

fun PsiElement.findContainingModifierList(): PsiModifierList? = findParent(resolveReferences = false) { it is PsiClass }

fun PsiElement.findContainingNameValuePair(): PsiNameValuePair? = findParent(resolveReferences = false) {
    it is PsiClass || it is PsiMethod || it is PsiAnnotation
}

private val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

fun PsiElement.isAncestorOf(child: PsiElement): Boolean = child.ancestors.contains(this)

private inline fun <reified T : PsiElement> PsiElement.findParent(resolveReferences: Boolean): T? {
    return findParent(resolveReferences) { false }
}

private inline fun <reified T : PsiElement> PsiElement.findParent(
    resolveReferences: Boolean,
    stop: (PsiElement) -> Boolean,
): T? {
    var el: PsiElement = this

    while (true) {
        if (resolveReferences && el is PsiReference) {
            el = el.resolve() ?: return null
        }

        if (el is T) {
            return el
        }

        if (el is PsiFile || el is PsiDirectory || stop(el)) {
            return null
        }

        el = el.parent ?: return null
    }
}

// Children
fun PsiClass.findFirstMember(): PsiMember? = findChild()

fun PsiElement.findNextMember(): PsiMember? = findSibling(true)

private inline fun <reified T : PsiElement> PsiElement.findChild(): T? {
    return firstChild?.findSibling(strict = false)
}

private inline fun <reified T : PsiElement> PsiElement.findSibling(strict: Boolean): T? {
    var sibling = if (strict) nextSibling ?: return null else this
    while (true) {
        if (sibling is T) {
            return sibling
        }

        sibling = sibling.nextSibling ?: return null
    }
}

fun PsiElement.findKeyword(name: String): PsiKeyword? {
    forEachChild {
        if (it is PsiKeyword && it.text == name) {
            return it
        }
    }
    return null
}

private inline fun PsiElement.forEachChild(func: (PsiElement) -> Unit) {
    firstChild?.forEachSibling(func, strict = false)
}

private inline fun PsiElement.forEachSibling(func: (PsiElement) -> Unit, strict: Boolean) {
    var sibling = if (strict) nextSibling ?: return else this
    while (true) {
        func(sibling)
        sibling = sibling.nextSibling ?: return
    }
}

inline fun PsiElement.findLastChild(condition: (PsiElement) -> Boolean): PsiElement? {
    var child = firstChild ?: return null
    var lastChild: PsiElement? = null

    while (true) {
        if (condition(child)) {
            lastChild = child
        }

        child = child.nextSibling ?: return lastChild
    }
}

inline fun <reified T : PsiElement> PsiElement.childrenOfType(): Collection<T> =
    PsiTreeUtil.findChildrenOfType(this, T::class.java)

inline fun <reified T : PsiElement> PsiElement.childOfType(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java)

/**
 * [InjectedLanguageManager.getInjectionHost] returns the first host of a multi-host injection for some reason.
 * Use this method as a workaround.
 */
fun PsiElement.findMultiInjectionHost(): PsiLanguageInjectionHost? {
    val injectedLanguageManager = InjectedLanguageManager.getInstance(project)
    val hostFile = injectedLanguageManager.getInjectionHost(this)?.containingFile ?: return null
    val hostOffset = injectedLanguageManager.injectedToHost(this, textRange.startOffset)
    val hostElement = hostFile.findElementAt(hostOffset) ?: return null
    return hostElement.parentOfType<PsiLanguageInjectionHost>(withSelf = true)
}

fun <T : Any> Sequence<T>.filter(filter: ElementFilter?, context: PsiElement): Sequence<T> {
    filter ?: return this
    return filter { filter.isAcceptable(it, context) }
}

fun Sequence<PsiElement>.toResolveResults(): Array<ResolveResult> = map(::PsiElementResolveResult).toTypedArray()

fun PsiParameterList.synchronize(newParams: List<PsiParameter>) {
    ChangeSignatureUtil.synchronizeList(this, newParams, { it.parameters.asList() }, BooleanArray(newParams.size))
}

val PsiElement.constantValue: Any?
    get() = JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(this)

val PsiElement.constantStringValue: String?
    get() = constantValue as? String

private val ACCESS_MODIFIERS =
    listOf(PsiModifier.PUBLIC, PsiModifier.PROTECTED, PsiModifier.PRIVATE, PsiModifier.PACKAGE_LOCAL)

fun isAccessModifier(@ModifierConstant modifier: String): Boolean {
    return modifier in ACCESS_MODIFIERS
}

infix fun PsiElement.equivalentTo(other: PsiElement?): Boolean {
    return manager.areElementsEquivalent(this, other)
}

fun PsiType?.isErasureEquivalentTo(other: PsiType?): Boolean {
    return this?.normalize() == other?.normalize()
}

fun PsiType.normalize(): PsiType {
    var normalized = TypeConversionUtil.erasure(this)
    if (normalized is PsiEllipsisType) {
        normalized = normalized.toArrayType()
    }
    return normalized
}

val PsiMethod.nameAndParameterTypes: String
    get() = "$name(${parameterList.parameters.joinToString(", ") { it.type.presentableText }})"

val <T : PsiElement> T.manipulator: ElementManipulator<T>?
    get() = ElementManipulators.getManipulator(this)

inline fun <T> PsiElement.cached(vararg dependencies: Any, crossinline compute: () -> T): T {
    return CachedValuesManager.getCachedValue(this) {
        CachedValueProvider.Result.create(compute(), *(dependencies.toList() + this).toTypedArray())
    }
}

@PublishedApi
internal val CACHE_LOCKS_KEY = Key.create<ConcurrentMap<Key<*>, ReentrantReadWriteLock>>("mcdev.cacheLock")

inline fun <T> PsiElement.lockedCached(
    key: Key<CachedValue<T>>,
    vararg dependencies: Any,
    crossinline compute: () -> T,
): T {
    val cacheLocks = (this as UserDataHolderEx).putUserDataIfAbsent(CACHE_LOCKS_KEY, ConcurrentHashMap())
    val cacheLock = cacheLocks.computeIfAbsent(key) { ReentrantReadWriteLock() }

    cacheLock.read {
        val value = getUserData(key)?.upToDateOrNull
        if (value != null) {
            return value.get()
        }
    }

    cacheLock.write {
        val value = getUserData(key)?.upToDateOrNull
        if (value != null) {
            return value.get()
        }

        return CachedValuesManager.getCachedValue(this, key) {
            CachedValueProvider.Result.create(compute(), *(dependencies.toList() + this).toTypedArray())
        }
    }
}

fun LookupElementBuilder.withImportInsertion(toImport: List<PsiClass>): LookupElementBuilder =
    this.withInsertHandler { insertionContext, _ ->
        toImport.forEach { ImportUtils.addImportIfNeeded(it, insertionContext.file) }
    }

fun PsiElement.findMcpModule() = this.cached {
    val file = containingFile?.virtualFile ?: return@cached null
    val index = ProjectFileIndex.getInstance(project)
    val modules = if (index.isInLibrary(file)) {
        val library = index.getOrderEntriesForFile(file)
            .asSequence()
            .mapNotNull { it as? LibraryOrderEntry }
            .firstOrNull()
            ?.library
            ?: return@cached null
        ModuleManager.getInstance(project).modules.asSequence()
            .filter { OrderEntryUtil.findLibraryOrderEntry(ModuleRootManager.getInstance(it), library) != null }
    } else {
        sequenceOf(this.findModule())
    }

    modules.mapNotNull { it?.findMcpModule() }.firstOrNull()
}

private fun Module.findMcpModule(): McpModule? {
    var result: McpModule? = null
    ModuleUtilCore.visitMeAndDependentModules(this) {
        result = MinecraftFacet.getInstance(it, McpModuleType)
        result == null
    }
    return result
}

val PsiElement.mcVersion: SemanticVersion?
    get() = this.cached {
        findMcpModule()?.let {
            SemanticVersion.parse(it.getSettings().minecraftVersion ?: return@let null)
        }
    }

@Suppress("PrivatePropertyName")
private val REAL_NAME_KEY = Key<String>("mcdev.real_name")

var PsiMember.realName: String?
    get() = getUserData(REAL_NAME_KEY)
    set(value) = putUserData(REAL_NAME_KEY, value)

val PsiMethodReferenceExpression.hasSyntheticMethod: Boolean
    get() {
        // the only method reference that doesn't have a synthetic method is a direct reference to a method
        if (referenceName == "new") return true
        val qualifier = this.qualifier
        if (qualifier !is PsiReferenceExpression) return true
        return qualifier.resolve() !is PsiClass
    }

val PsiClass.psiType: PsiType
    get() = PsiTypesUtil.getClassType(this)

fun PsiElementFactory.createLiteralExpression(constant: Any?): PsiExpression {
    return when (constant) {
        null -> createExpressionFromText("null", null)
        is Boolean, is Double, is Int -> createExpressionFromText(constant.toString(), null)
        is Char -> createExpressionFromText("'${StringUtil.escapeCharCharacters(constant.toString())}'", null)
        is Float -> createExpressionFromText("${constant}F", null)
        is Long -> createExpressionFromText("${constant}L", null)
        is String -> createExpressionFromText("\"${StringUtil.escapeStringCharacters(constant)}\"", null)

        else -> throw IncorrectOperationException("Unsupported literal type: ${constant.javaClass.name}")
    }
}
