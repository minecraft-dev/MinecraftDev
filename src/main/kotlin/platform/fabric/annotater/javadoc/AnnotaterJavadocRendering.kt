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

import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckout
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterCheckoutService
import com.demonwav.mcdev.platform.fabric.annotater.AnnotaterSettings
import com.intellij.lang.java.JavaDocumentationProvider
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiCompiledElement
import com.intellij.psi.PsiJavaDocumentedElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import com.intellij.psi.util.PsiTreeUtil
import java.io.IOException

/**
 * Renders annotater Javadocs with the IDE's own Java documentation renderer, by rendering the declaration in a copy of
 * its source file that has the annotater Javadoc as its doc comment.
 */
object AnnotaterJavadocRendering {
    /**
     * Finds the checkout whose Javadocs should be displayed instead of the existing Javadoc of [member], or null if
     * the existing Javadoc should be shown.
     */
    fun checkoutForDisplay(member: PsiMember): AnnotaterCheckout? {
        if (!AnnotaterSettings.instance.showAnnotaterJavadocs) {
            return null
        }
        return AnnotaterCheckoutService.instance.checkoutFor(member)
    }

    /**
     * All classes, fields and methods declared in [file] and its inner classes.
     */
    fun displayedMembers(file: PsiJavaFile): Sequence<PsiMember> = members(file.classes)

    private fun members(classes: Array<PsiClass>): Sequence<PsiMember> = classes.asSequence().flatMap { clazz ->
        sequenceOf(clazz) + clazz.fields + clazz.methods + members(clazz.innerClasses)
    }

    /**
     * The offset that the annotater Javadoc of [member] is displayed above: its declaration, after any existing
     * doc comment.
     */
    fun displayOffset(member: PsiMember): Int {
        val existing = if (member is PsiCompiledElement) null else (member as? PsiJavaDocumentedElement)?.docComment
        return existing?.let { PsiTreeUtil.skipWhitespacesAndCommentsForward(it) }?.textRange?.startOffset
            ?: member.textRange.startOffset
    }

    fun getJavadoc(checkout: AnnotaterCheckout, target: AnnotaterJavadocTarget): String? {
        return try {
            if (checkout.javadocStore.hasFile(target.outerClass)) checkout.javadocStore.get(target) else null
        } catch (e: IOException) {
            thisLogger().info("Failed to read annotater Javadoc of $target", e)
            null
        }
    }

    /**
     * Finds the declaration of [member] in a copy of its source file with [javadoc] as its doc comment.
     */
    private fun copiedDeclaration(member: PsiMember, javadoc: String?): PsiJavaDocumentedElement? {
        val target = AnnotaterJavadocTarget.fromPsi(member) ?: return null
        val copy = AnnotaterJavadocSourceCopy.create(member) { if (it == target) javadoc else null } ?: return null
        val file = copy.createPsiFile(member.project) ?: return null
        return copy.memberOf(file, target) as? PsiJavaDocumentedElement
    }

    /**
     * Generates the Quick Documentation of [member] using [javadoc] as its description.
     */
    fun generateDoc(member: PsiMember, javadoc: String?): String? {
        val declaration = copiedDeclaration(member, javadoc) ?: return null
        return JavaDocumentationProvider.generateDocStatic(declaration, null)
    }
}
