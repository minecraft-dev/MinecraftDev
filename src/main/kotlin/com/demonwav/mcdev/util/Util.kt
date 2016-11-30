/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("Util")
package com.demonwav.mcdev.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil

fun runWriteTask(runnable: Runnable) {
    ApplicationManager.getApplication().invokeAndWait({ ApplicationManager.getApplication().runWriteAction(runnable) }, ModalityState.NON_MODAL)
}

fun runWriteTaskLater(runnable: Runnable) {
    ApplicationManager.getApplication().invokeLater({ ApplicationManager.getApplication().runWriteAction(runnable) }, ModalityState.NON_MODAL)
}

fun invokeLater(runnable: Runnable) {
    ApplicationManager.getApplication().invokeLater(runnable)
}

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
