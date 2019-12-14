/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.util.MixinConstants
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
            val modifierList = (element as? PsiMethod)?.modifierList ?: return false
            return MixinConstants.Annotations.ENTRY_POINTS.any {
                modifierList.findAnnotation(it) != null
            }
        }

        override fun checkTagValue(value: PsiDocTagValue?): String? = null
        override fun getReference(value: PsiDocTagValue?): PsiReference? = null

        object Author : InjectorTag() {

            override fun getName() = "author"

            override fun checkTagValue(value: PsiDocTagValue?): String? {
                return "The @author JavaDoc tag must be filled in.".takeIf { value?.text?.trim().isNullOrEmpty() }
            }
        }

        object Reason : InjectorTag() {
            override fun getName() = "reason"
        }
    }
}
