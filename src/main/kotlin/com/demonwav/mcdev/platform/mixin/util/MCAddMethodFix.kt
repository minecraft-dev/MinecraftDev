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

import com.demonwav.mcdev.util.invokeStatic
import com.intellij.codeInsight.daemon.impl.quickfix.AddMethodFix
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*

/**
 * Fixes an issue with [AddMethodFix] where static method bodies are removed in interfaces
 */
class MCAddMethodFix(methodPrototype: PsiMethod, implClass: PsiClass) : AddMethodFix(methodPrototype, implClass) {

    private val myMethodPrototype = SmartPointerManager.createPointer(methodPrototype)

    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        val methodPrototype = myMethodPrototype.element ?: return

        val myClass = startElement as PsiClass

        val body = methodPrototype.body
        if (myClass.isInterface && !methodPrototype.modifierList.hasExplicitModifier(PsiModifier.STATIC) && body != null) body.delete()
        var method = myClass.add(methodPrototype) as PsiMethod
        method = method.replace(AddMethodFix::class.java.invokeStatic("reformat", arrayOf(Project::class.java, PsiMethod::class.java), project, method) as PsiMethod) as PsiMethod
        postAddAction(file, editor, method)
    }

}
