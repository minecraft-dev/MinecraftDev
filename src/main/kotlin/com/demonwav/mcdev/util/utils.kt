/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.Result
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.Contract

inline fun <T> runInlineReadAction(func: () -> T): T {
    return ApplicationManager.getApplication().acquireReadActionLock().use { func() }
}

inline fun runWriteTask(crossinline func: () -> Unit) {
    if (ApplicationManager.getApplication().isWriteAccessAllowed) {
        func()
    } else {
        invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                func()
            }
        }
    }
}

inline fun runWriteTaskLater(crossinline func: () -> Unit) {
    if (ApplicationManager.getApplication().isWriteAccessAllowed) {
        func()
    } else {
        invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                func()
            }
        }
    }
}

inline fun invokeAndWait(crossinline func: () -> Unit) {
    if (ApplicationManager.getApplication().isDispatchThread) {
        func()
    } else {
        ApplicationManager.getApplication().invokeAndWait({ func() }, ModalityState.defaultModalityState())
    }
}

inline fun invokeLater(crossinline func: () -> Unit) {
    if (ApplicationManager.getApplication().isDispatchThread) {
        func()
    } else {
        ApplicationManager.getApplication().invokeLater({ func() }, ModalityState.defaultModalityState())
    }
}

inline fun invokeLaterAny(crossinline func: () -> Unit) {
    if (ApplicationManager.getApplication().isDispatchThread) {
        func()
    } else {
        ApplicationManager.getApplication().invokeLater({ func() }, ModalityState.any())
    }
}

inline fun <T : Any?> PsiFile.runWriteAction(crossinline func: () -> T) =
    applyWriteAction { func() }

inline fun <T : Any?> PsiFile.applyWriteAction(crossinline func: PsiFile.() -> T): T {
    val result = object : WriteCommandAction<T>(project) {
        override fun run(result: Result<T>) {
            result.setResult(func())
        }
    }.execute().resultObject
    PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(FileDocumentManager.getInstance().getDocument(this.virtualFile) ?: return result)
    return result
}

/**
 * Returns an untyped array for the specified [Collection].
 */
@Contract(pure = true)
fun Collection<*>.toArray(): Array<Any?> {
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    return (this as java.util.Collection<*>).toArray()
}

inline fun <T : Collection<*>> T.ifEmpty(func: () -> Unit): T {
    if (isEmpty()) func()
    return this
}

@Contract(pure = true)
inline fun <T, R> Iterable<T>.mapFirstNotNull(transform: (T) -> R?): R? {
    forEach { transform(it)?.let { return it } }
    return null
}

@Contract(pure = true)
inline fun <T, R> Array<T>.mapFirstNotNull(transform: (T) -> R?): R? {
    forEach { transform(it)?.let { return it } }
    return null
}

inline fun <T, reified R> Array<T>.mapToArray(transform: (T) -> R) = Array(size) { i -> transform(this[i]) }
inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R) = Array(size) { i -> transform(this[i]) }

fun <T : Any> Array<T?>.castNotNull(): Array<T> {
    @Suppress("UNCHECKED_CAST")
    return this as Array<T>
}

fun Module.findChildren(): Set<Module> {
    runInlineReadAction {
        val manager = ModuleManager.getInstance(project)
        val result = mutableSetOf<Module>()

        for (m in manager.modules) {
            if (m === this) {
                continue
            }

            val path = manager.getModuleGroupPath(m) ?: continue
            val namedModule = manager.findModuleByName(path.last()) ?: continue

            if (namedModule != this) {
                continue
            }

            result.add(m)
        }

        return result
    }
}

// Keep a single gson constant around rather than initializing it everywhere
val gson = Gson()

// Using the ugly TypeToken approach we can any complex generic signature, including
// nested generics
inline fun <reified T : Any> Gson.fromJson(text: String): T = fromJson(text, object : TypeToken<T>() {}.type)

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
        return 1_000_000 + bonus// exact match
    }

    val lowerCaseThis = this.toLowerCase()
    val lowerCaseText = text.toLowerCase()

    if (lowerCaseThis == lowerCaseText) {
        return 100_000 + bonus // lowercase exact match
    }

    val distance = Math.min(lowerCaseThis.length, lowerCaseText.length)
    for (i in 0 until distance) {
        if (lowerCaseThis[i] != lowerCaseText[i]) {
            return i + bonus
        }
    }
    return distance + bonus
}
