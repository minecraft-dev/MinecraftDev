/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.lang.java.lexer.JavaLexer
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.libraries.LibraryKind
import com.intellij.openapi.roots.libraries.LibraryKindRegistry
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import java.lang.invoke.MethodHandles
import java.util.Locale
import kotlin.math.min
import kotlin.reflect.KClass
import org.jetbrains.concurrency.Promise
import org.jetbrains.concurrency.runAsync

inline fun <T : Any?> runWriteTask(crossinline func: () -> T): T {
    return invokeAndWait {
        ApplicationManager.getApplication().runWriteAction(Computable { func() })
    }
}

fun runWriteTaskLater(func: () -> Unit) {
    invokeLater {
        ApplicationManager.getApplication().runWriteAction(func)
    }
}

inline fun Project.runWriteTaskInSmartMode(crossinline func: () -> Unit) {
    val dumbService = DumbService.getInstance(this)
    lateinit var runnable: Runnable
    runnable = Runnable {
        if (isDisposed) {
            throw ProcessCanceledException()
        }
        runWriteTask {
            if (isDisposed) {
                throw ProcessCanceledException()
            }
            if (dumbService.isDumb) {
                dumbService.runWhenSmart(runnable)
            } else {
                func()
            }
        }
    }
    dumbService.runWhenSmart(runnable)
}

fun <T : Any?> invokeAndWait(func: () -> T): T {
    val ref = Ref<T>()
    ApplicationManager.getApplication().invokeAndWait({ ref.set(func()) }, ModalityState.defaultModalityState())
    return ref.get()
}

fun invokeLater(func: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(func, ModalityState.defaultModalityState())
}

fun invokeLater(expired: Condition<*>, func: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(func, ModalityState.defaultModalityState(), expired)
}

fun invokeLaterAny(func: () -> Unit) {
    ApplicationManager.getApplication().invokeLater(func, ModalityState.any())
}

fun <T> invokeEdt(block: () -> T): T {
    return AppUIExecutor.onUiThread().submit(block).get()
}

inline fun <T : Any?> PsiFile.runWriteAction(crossinline func: () -> T) =
    applyWriteAction { func() }

inline fun <T : Any?> PsiFile.applyWriteAction(crossinline func: PsiFile.() -> T): T {
    val result = WriteCommandAction.writeCommandAction(this).withGlobalUndo().compute<T, Throwable> { func() }
    val documentManager = PsiDocumentManager.getInstance(project)
    val document = documentManager.getDocument(this) ?: return result
    documentManager.doPostponedOperationsAndUnblockDocument(document)
    return result
}

fun <T> runReadActionAsync(runnable: () -> T): Promise<T> {
    return runAsync {
        runReadAction(runnable)
    }
}

fun waitForAllSmart() {
    for (project in ProjectManager.getInstance().openProjects) {
        if (!project.isDisposed) {
            DumbService.getInstance(project).waitForSmartMode()
        }
    }
}

/**
 * Returns an untyped array for the specified [Collection].
 */
fun Collection<*>.toArray(): Array<Any?> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    return (this as java.util.Collection<*>).toArray()
}

inline fun <T : Collection<*>> T.ifEmpty(func: () -> Unit): T {
    if (isEmpty()) {
        func()
    }
    return this
}

inline fun <T : Collection<*>?> T.ifNullOrEmpty(func: () -> Unit): T {
    if (isNullOrEmpty()) {
        func()
    }
    return this
}

inline fun <T : Collection<*>> T.ifNotEmpty(func: (T) -> Unit): T {
    if (isNotEmpty()) {
        func(this)
    }
    return this
}

inline fun <T, R> Iterable<T>.mapFirstNotNull(transform: (T) -> R?): R? {
    forEach { element -> transform(element)?.let { return it } }
    return null
}

inline fun <T, R> Array<T>.mapFirstNotNull(transform: (T) -> R?): R? {
    forEach { element -> transform(element)?.let { return it } }
    return null
}

inline fun <T : Any> Iterable<T?>.forEachNotNull(func: (T) -> Unit) {
    forEach { it?.let(func) }
}

inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R) = Array(size) { i -> transform(this[i]) }
inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R) = Array(size) { i -> transform(this[i]) }
inline fun <T, reified R> Collection<T>.mapToArray(transform: (T) -> R): Array<R> {
    val result = arrayOfNulls<R>(size)
    var i = 0
    for (element in this) {
        result[i++] = transform(element)
    }
    return result.castNotNull()
}

fun <T> Array<T?>.castNotNull(): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return this as Array<T>
}

// Same as Collections.rotate but for arrays
fun <T> Array<T>.rotate(amount: Int) {
    val size = size
    if (size == 0) return
    var distance = amount % size
    if (distance < 0) distance += size
    if (distance == 0) return

    var cycleStart = 0
    var nMoved = 0
    while (nMoved != size) {
        var displaced = this[cycleStart]
        var i = cycleStart
        do {
            i += distance
            if (i >= size) i -= size
            val newDisplaced = this[i]
            this[i] = displaced
            displaced = newDisplaced
            nMoved++
        } while (i != cycleStart)
        cycleStart++
    }
}

inline fun <T> Iterable<T>.firstIndexOrNull(predicate: (T) -> Boolean): Int? {
    for ((index, element) in this.withIndex()) {
        if (predicate(element)) {
            return index
        }
    }
    return null
}

fun Module.findChildren(): Set<Module> {
    return runReadAction {
        val manager = ModuleManager.getInstance(project)
        val result = mutableSetOf<Module>()

        for (m in manager.modules) {
            if (m === this) {
                continue
            }

            val path = manager.getModuleGrouper(null).getGroupPath(m)
            if (path.isEmpty()) {
                continue
            }

            val namedModule = manager.findModuleByName(path.last()) ?: continue

            if (namedModule != this) {
                continue
            }

            result.add(m)
        }

        return@runReadAction result
    }
}

// Using the ugly TypeToken approach we can use any complex generic signature, including
// nested generics
inline fun <reified T : Any> Gson.fromJson(text: String): T = fromJson(text, object : TypeToken<T>() {}.type)
fun <T : Any> Gson.fromJson(text: String, type: KClass<T>): T = fromJson(text, type.java)

fun <K> Map<K, *>.containsAllKeys(vararg keys: K) = keys.all { this.containsKey(it) }

/**
 * Splits a string into the longest prefix matching a predicate and the corresponding suffix *not* matching.
 *
 * Note: Name inspired by Scala.
 */
inline fun String.span(predicate: (Char) -> Boolean): Pair<String, String> {
    val prefix = takeWhile(predicate)
    return prefix to drop(prefix.length)
}

fun String.getSimilarity(text: String, bonus: Int = 0): Int {
    if (this == text) {
        return 1_000_000 + bonus // exact match
    }

    val lowerCaseThis = this.lowercase(Locale.ENGLISH)
    val lowerCaseText = text.lowercase(Locale.ENGLISH)

    if (lowerCaseThis == lowerCaseText) {
        return 100_000 + bonus // lowercase exact match
    }

    val distance = min(lowerCaseThis.length, lowerCaseText.length)
    for (i in 0 until distance) {
        if (lowerCaseThis[i] != lowerCaseText[i]) {
            return i + bonus
        }
    }
    return distance + bonus
}

fun String.isJavaKeyword() = JavaLexer.isSoftKeyword(this, LanguageLevel.HIGHEST)

fun String.toJavaIdentifier(allowDollars: Boolean = true): String {
    if (this.isEmpty()) {
        return "_"
    }

    if (this.isJavaKeyword()) {
        return "_$this"
    }

    if (!this[0].isJavaIdentifierStart() && this[0].isJavaIdentifierPart()) {
        return "_$this".toJavaIdentifier(allowDollars)
    }

    return this.asSequence()
        .map {
            if (it.isJavaIdentifierPart() && (allowDollars || it != '$')) {
                it
            } else {
                "_"
            }
        }
        .joinToString("")
}

fun String.toJavaClassName() = StringUtil.capitalizeWords(this, true)
    .replace(" ", "").toJavaIdentifier(allowDollars = false)

fun String.toPackageName(): String {
    if (this.isEmpty()) {
        return "_"
    }

    val firstChar = this.first().let {
        if (it.isJavaIdentifierStart()) {
            "$it"
        } else {
            ""
        }
    }
    val packageName = firstChar + this.asSequence()
        .drop(1)
        .filter { it.isJavaIdentifierPart() || it == '.' }
        .joinToString("")

    return if (packageName.isEmpty()) {
        "_"
    } else {
        packageName.lowercase(Locale.ENGLISH)
    }
}

inline fun <reified T> Iterable<*>.firstOfType(): T? {
    return this.firstOrNull { it is T } as? T
}

fun libraryKind(id: String): LibraryKind = LibraryKindRegistry.getInstance().findKindById(id) ?: LibraryKind.create(id)

fun String.capitalize(): String =
    replaceFirstChar {
        if (it.isLowerCase()) {
            it.titlecase(Locale.ENGLISH)
        } else {
            it.toString()
        }
    }

fun String.decapitalize(): String = replaceFirstChar { it.lowercase(Locale.ENGLISH) }

// Bit of a hack, but this allows us to get the class object for top level declarations without having to
// put the whole class name in as a string (easier to refactor, etc.)
@Suppress("NOTHING_TO_INLINE") // In order for this to work this function must be `inline`
inline fun loggerForTopLevel() = Logger.getInstance(MethodHandles.lookup().lookupClass())

inline fun <T> runCatchingKtIdeaExceptions(action: () -> T): T? = try {
    action()
} catch (e: Exception) {
    when (e.javaClass.name) {
        "org.jetbrains.kotlin.idea.caches.resolve.KotlinIdeaResolutionException",
        "org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments" -> {
            loggerForTopLevel().info("Caught Kotlin plugin exception", e)
            null
        }
        else -> throw e
    }
}

fun <T : Throwable> withSuppressed(original: T?, other: T): T =
    original?.apply { addSuppressed(other) } ?: other

fun <S : CharSequence, R> S.ifNotBlank(block: (S) -> R): R? {
    if (this.isNotBlank()) {
        return block(this)
    }

    return null
}
