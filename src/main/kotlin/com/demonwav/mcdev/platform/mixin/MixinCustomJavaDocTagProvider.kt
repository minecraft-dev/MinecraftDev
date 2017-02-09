/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.platform.mixin.inspections.AuthorInspection

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.javadoc.PsiDocTagValue

class MixinCustomJavaDocTagProvider : CustomJavadocTagProvider {

    override fun getSupportedTags(): List<JavadocTagInfo> = listOf(OverwriteAuthorTag, OverwriteReasonTag)

    private abstract class OverwriteTag : JavadocTagInfo {

        override fun isInline() = false

        override fun isValidInContext(element: PsiElement): Boolean {
            return element is PsiMethod && AuthorInspection.shouldHaveAuthorTag(element)
        }

        override fun checkTagValue(value: PsiDocTagValue): String? = null
        override fun getReference(value: PsiDocTagValue) = null

    }

    private object OverwriteAuthorTag : OverwriteTag() {

        override fun getName() = "author"

        override fun checkTagValue(value: PsiDocTagValue): String? {
            return "The @author JavaDoc tag on @Overwrite methods must be filled in.".takeIf { value.text.trim().isEmpty() }
        }

    }

    private object OverwriteReasonTag : OverwriteTag() {
        override fun getName() = "reason"
    }

}
