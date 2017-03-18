/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("UtilKt")
package com.demonwav.mcdev.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import org.jetbrains.annotations.Contract

// Kotlin functions
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

inline fun <T, reified R> List<T>.mapToArray(transform: (T) -> R): Array<R> {
    return Array(size) { i -> transform(this[i]) }
}

object Util {
    // Java static methods
    // Can't call inlined stuff from java
    @JvmStatic
    fun runWriteTask(func: Runnable) {
        runWriteTask { func.run() }
    }

    @JvmStatic
    fun runWriteTaskLater(func: Runnable) {
        runWriteTaskLater { func.run() }
    }

    @JvmStatic
    fun invokeAndWait(func: Runnable) {
        invokeAndWait { func.run() }
    }

    @JvmStatic
    fun invokeLater(func: Runnable) {
        invokeLater { func.run() }
    }
}
