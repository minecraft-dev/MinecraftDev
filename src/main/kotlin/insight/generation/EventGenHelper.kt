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

package com.demonwav.mcdev.insight.generation

import com.demonwav.mcdev.util.addImplements
import com.intellij.core.CoreJavaCodeStyleManager
import com.intellij.lang.LanguageExtension
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.parentOfType
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.idea.base.analysis.api.utils.shortenReferences
import org.jetbrains.kotlin.idea.base.analysis.api.utils.shortenReferencesInRange
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

interface EventGenHelper {

    fun addImplements(context: PsiElement, fqn: String)

    fun reformatAndShortenRefs(file: PsiFile, startOffset: Int, endOffset: Int)

    companion object {

        val EP_NAME = ExtensionPointName.create<LanguageExtensionPoint<EventGenHelper>>(
            "com.demonwav.minecraft-dev.eventGenHelper"
        )
        val COLLECTOR = LanguageExtension(EP_NAME, JvmEventGenHelper())
    }
}

open class JvmEventGenHelper : EventGenHelper {

    override fun addImplements(context: PsiElement, fqn: String) {}

    override fun reformatAndShortenRefs(file: PsiFile, startOffset: Int, endOffset: Int) {
        val project = file.project

        val marker = doReformat(project, file, startOffset, endOffset) ?: return

        CoreJavaCodeStyleManager.getInstance(project).shortenClassReferences(file, marker.startOffset, marker.endOffset)
    }

    companion object {

        fun doReformat(project: Project, file: PsiFile, startOffset: Int, endOffset: Int): RangeMarker? {
            val documentManager = PsiDocumentManager.getInstance(project)
            val document = documentManager.getDocument(file) ?: return null

            val marker = document.createRangeMarker(startOffset, endOffset).apply {
                isGreedyToLeft = true
                isGreedyToRight = true
            }

            CodeStyleManager.getInstance(project).reformatText(file, startOffset, endOffset)
            documentManager.commitDocument(document)

            return marker
        }
    }
}

class JavaEventGenHelper : JvmEventGenHelper() {

    override fun addImplements(context: PsiElement, fqn: String) {
        val psiClass = context.parentOfType<PsiClass>(true) ?: return
        psiClass.addImplements(fqn)
    }
}

class KotlinEventGenHelper : EventGenHelper {

    private fun hasSuperType(ktClass: KtClassOrObject, fqn: String): Boolean {
        val names = setOf(fqn, fqn.substringAfterLast('.'))
        return ktClass.superTypeListEntries.any { it.text in names }
    }

    override fun addImplements(context: PsiElement, fqn: String) {
        val ktClass = context.parentOfType<KtClassOrObject>(true) ?: return
        if (hasSuperType(ktClass, fqn)) {
            return
        }

        val factory = KtPsiFactory.contextual(context)
        val entry = factory.createSuperTypeEntry(fqn)
        val insertedEntry = ktClass.addSuperTypeListEntry(entry)
        when (KotlinPluginModeProvider.currentPluginMode) {
            KotlinPluginMode.K1 -> @OptIn(K1Deprecation::class) ShortenReferences.DEFAULT.process(insertedEntry)
            // TODO find a non-internal alternative to this...
            KotlinPluginMode.K2 -> @OptIn(KaIdeApi::class) shortenReferences(insertedEntry)
        }
    }

    override fun reformatAndShortenRefs(file: PsiFile, startOffset: Int, endOffset: Int) {
        if (file !is KtFile) {
            return
        }
        val project = file.project

        val marker = JvmEventGenHelper.doReformat(project, file, startOffset, endOffset) ?: return
        when (KotlinPluginModeProvider.currentPluginMode) {
            KotlinPluginMode.K1 -> @OptIn(K1Deprecation::class) ShortenReferences.DEFAULT.process(file, marker.startOffset, marker.endOffset)
            // TODO find a non-internal alternative to this...
            KotlinPluginMode.K2 -> @OptIn(KaIdeApi::class) shortenReferencesInRange(file, marker.textRange)
        }
    }
}
