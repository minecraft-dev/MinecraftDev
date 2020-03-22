/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

import com.intellij.codeInsight.daemon.impl.quickfix.AddMethodFix
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager

/**
 * Fixes an issue with [AddMethodFix] where static method bodies are removed in interfaces
 */
class AccessorAddMethodFix(methodPrototype: PsiMethod, implClass: PsiClass) : AddMethodFix(methodPrototype, implClass) {

    private val myMethodPrototype = SmartPointerManager.createPointer(methodPrototype)

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val methodPrototype = myMethodPrototype.element ?: return

        val myClass = startElement as PsiClass

        val body = methodPrototype.body
        val deleteBody = myClass.isInterface && !methodPrototype.modifierList.hasExplicitModifier(PsiModifier.STATIC)
        if (deleteBody && body != null) {
            body.delete()
        }
        var method = myClass.add(methodPrototype) as PsiMethod
        method = reformat(project, method)
        postAddAction(file, editor, method)
    }

    private fun reformat(project: Project, method: PsiMethod): PsiMethod {
        var result = method
        val codeStyleManager = CodeStyleManager.getInstance(project)
        result = codeStyleManager.reformat(result) as PsiMethod

        val javaCodeStyleManager = JavaCodeStyleManager.getInstance(project)
        result = javaCodeStyleManager.shortenClassReferences(result) as PsiMethod
        return result
    }
}
