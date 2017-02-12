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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.OVERWRITE

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.javadoc.CustomJavadocTagProvider
import com.intellij.psi.javadoc.JavadocTagInfo
import com.intellij.psi.javadoc.PsiDocTagValue

class MixinCustomJavaDocTagProvider : CustomJavadocTagProvider {

    override fun getSupportedTags(): List<JavadocTagInfo> = listOf(OverwriteTag.Author, OverwriteTag.Reason)

    private sealed class OverwriteTag : JavadocTagInfo {

        override fun isInline() = false

        override fun isValidInContext(element: PsiElement): Boolean {
            return element is PsiMethod && element.modifierList.findAnnotation(OVERWRITE) != null
        }

        override fun checkTagValue(value: PsiDocTagValue): String? = null
        override fun getReference(value: PsiDocTagValue) = null

        object Author : OverwriteTag() {

            override fun getName() = "author"

            override fun checkTagValue(value: PsiDocTagValue): String? {
                return "The @author JavaDoc tag on @Overwrite methods must be filled in.".takeIf { value.text.trim().isEmpty() }
            }

        }

        object Reason : OverwriteTag() {
            override fun getName() = "reason"
        }

    }

}
