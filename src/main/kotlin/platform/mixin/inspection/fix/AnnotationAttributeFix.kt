/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mixin.inspection.fix

import com.demonwav.mcdev.util.createLiteralExpression
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager

open class AnnotationAttributeFix(
    annotation: PsiAnnotation,
    vararg attributes: Pair<String, Any?>,
) : LocalQuickFixOnPsiElement(annotation) {
    @SafeFieldForPreview
    private val attributes = attributes.map { (key, value) ->
        key to value?.let {
            if (it !is PsiAnnotationMemberValue) {
                JavaPsiFacade.getElementFactory(annotation.project).createLiteralExpression(it)
            } else {
                it
            }
        }
    }

    private val description = run {
        val added = this.attributes
            .filter { (_, value) -> value != null }
            .map { (key, value) -> "$key = ${value!!.text}" }
        val removed = this.attributes
            .filter { (_, value) -> value == null }
            .map { (key, _) -> key }

        buildString {
            if (added.isNotEmpty()) {
                append("Add ")
                for ((i, add) in added.withIndex()) {
                    if (i != 0) {
                        if (i == added.size - 1) {
                            append(" and ")
                        } else {
                            append(", ")
                        }
                    }
                    append(add)
                }
                if (removed.isNotEmpty()) {
                    append(", and remove ")
                }
            } else if (removed.isNotEmpty()) {
                append("Remove ")
            }
            if (removed.isNotEmpty()) {
                for ((i, rem) in removed.withIndex()) {
                    if (i != 0) {
                        if (i == removed.size - 1) {
                            append(" and ")
                        } else {
                            append(", ")
                        }
                    }
                    append(rem)
                }
            }
        }
    }

    override fun getFamilyName() = description
    override fun getText() = description

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val annotation = startElement as? PsiAnnotation ?: return
        for ((key, value) in attributes) {
            annotation.setDeclaredAttributeValue(key, value)
        }

        // replace single "value = foo" with "foo"
        val attrs = annotation.parameterList.attributes
        if (attrs.size == 1 && attrs[0].name == "value") {
            attrs[0].value?.let { value ->
                val fakeAnnotation = JavaPsiFacade.getElementFactory(project).createAnnotationFromText("@Foo(0)", null)
                fakeAnnotation.parameterList.attributes[0].value!!.replace(value)
                annotation.parameterList.replace(fakeAnnotation.parameterList)
            }
        }

        CodeStyleManager.getInstance(project).reformat(annotation.parameterList)
    }
}
