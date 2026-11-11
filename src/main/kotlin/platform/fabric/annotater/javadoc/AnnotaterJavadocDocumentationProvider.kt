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

package com.demonwav.mcdev.platform.fabric.annotater.javadoc

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement

/**
 * Shows annotater Javadocs instead of the existing Javadoc of Minecraft classes in Quick Documentation.
 */
class AnnotaterJavadocDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?): String? {
        if (element == null || DumbService.isDumb(element.project)) {
            return null
        }
        val member = AnnotaterJavadocEditing.normalize(element) ?: return null
        val checkout = AnnotaterJavadocRendering.checkoutForDisplay(member) ?: return null
        val target = AnnotaterJavadocTarget.fromPsi(member) ?: return null
        return AnnotaterJavadocRendering.generateDoc(member, checkout.javadocStore.getOrNull(target))
    }
}
