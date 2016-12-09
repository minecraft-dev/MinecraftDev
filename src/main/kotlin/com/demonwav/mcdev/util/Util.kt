/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("UtilKt")
package com.demonwav.mcdev.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil

// Kotlin functions
// can't inline them due to a bug in the compiler
fun runWriteTask(func: () -> Unit) {
    invokeAndWait { runWriteAction(func) }
}

fun runWriteTaskLater(func: () -> Unit) {
    invokeLater { runWriteAction(func) }
}

fun invokeAndWait(func: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait({ func() }, ModalityState.current())
}

fun invokeLater(func: () -> Unit) {
    ApplicationManager.getApplication().invokeLater({ func() }, ModalityState.current())
}

object Util {
    // Java static methods
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

    @JvmStatic
    fun defaultNameForSubClassEvents(psiClass: PsiClass): String {
        val isInnerClass = psiClass.parent !is PsiFile

        val name = StringBuilder()
        if (isInnerClass) {
            val containingClass = PsiUtil.getContainingNotInnerClass(psiClass)
            if (containingClass != null) {
                if (containingClass.name != null) {
                    name.append(containingClass.name!!.replace("Event".toRegex(), ""))
                }
            }
        }

        var className = psiClass.name!!
        if (className.startsWith(name.toString())) {
            className = className.substring(name.length)
        }
        name.append(className.replace("Event".toRegex(), ""))

        name.insert(0, "on")
        return name.toString()
    }
}
