/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.util.createLiteralExpression
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.ide.nls.NlsMessages
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationMemberValue
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager

open class AnnotationAttributeFix(
    annotation: PsiAnnotation,
    vararg attributes: Pair<String, Any?>,
) : LocalQuickFixOnPsiElement(annotation) {
    @SafeFieldForPreview
    private val attributes = attributes.map { (key, value) ->
        key to value?.let {
            it as? PsiAnnotationMemberValue
                ?: JavaPsiFacade.getElementFactory(annotation.project).createLiteralExpression(it)
        }
    }

    private val description = run {
        val added = this.attributes
            .filter { (_, value) -> value != null }
            .map { (key, value) -> "$key = ${value!!.text}" }
        val removed = this.attributes
            .filter { (_, value) -> value == null }
            .map { (key, _) -> key }

        when {
            added.isNotEmpty() && removed.isNotEmpty() -> MCDevBundle(
                "mixin.fix.annotation_attribute.add_and_remove",
                NlsMessages.formatAndList(added),
                NlsMessages.formatAndList(removed),
            )
            added.isNotEmpty() -> MCDevBundle("mixin.fix.annotation_attribute.add", NlsMessages.formatAndList(added))
            else -> MCDevBundle("mixin.fix.annotation_attribute.remove", NlsMessages.formatAndList(removed))
        }
    }

    override fun getFamilyName() = description
    override fun getText() = description

    override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
        val annotation = startElement as? PsiAnnotation ?: return
        var needsShortenClassReferences = false

        for ((key, value) in attributes) {
            annotation.setDeclaredAttributeValue(key, value)

            if (value != null && value !is PsiLiteralExpression) {
                needsShortenClassReferences = true
            }
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

        if (needsShortenClassReferences) {
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(annotation)
        }
    }
}
