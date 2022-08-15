/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.javadoc.PsiDocTagValue

class MixinCustomJavaDocTagProvider : CustomJavadocTagProvider {

    override fun getSupportedTags(): List<JavadocTagInfo> = listOf(InjectorTag.Author, InjectorTag.Reason)

    private sealed class InjectorTag : JavadocTagInfo {

        override fun isInline() = false

        override fun isValidInContext(element: PsiElement?): Boolean {
            if (element !is PsiMethod) {
                return false
            }
            val project = element.project
            for (annotation in element.annotations) {
                val qName = annotation.qualifiedName ?: continue
                val handler = MixinAnnotationHandler.forMixinAnnotation(qName, project)
                if (handler != null && handler.isEntryPoint) {
                    return true
                }
            }
            return false
        }

        override fun checkTagValue(value: PsiDocTagValue?): String? = null
        override fun getReference(value: PsiDocTagValue?): PsiReference? = null

        object Author : InjectorTag() {

            override fun getName() = "author"

            override fun checkTagValue(value: PsiDocTagValue?): String? {
                return "The @author JavaDoc tag must be filled in.".takeIf { value?.text.isNullOrBlank() }
            }
        }

        object Reason : InjectorTag() {
            override fun getName() = "reason"

            override fun checkTagValue(value: PsiDocTagValue?): String? {
                return "The @reason JavaDoc tag must be filled in.".takeIf { value?.text.isNullOrBlank() }
            }
        }
    }
}
