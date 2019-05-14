/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n.actions

import com.demonwav.mcdev.i18n.index.TranslationIndex
import com.demonwav.mcdev.i18n.index.TranslationInverseIndex
import com.demonwav.mcdev.i18n.index.merge
import com.demonwav.mcdev.i18n.intentions.ConvertToTranslationIntention
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.psi.PsiLiteral
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.evaluation.uValueOf
import org.jetbrains.uast.toUElement

class ConvertToTranslationAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE) ?: return
        val editor = e.getData(PlatformDataKeys.EDITOR) ?: return
        val element = file.findElementAt(editor.caretModel.offset) ?: return
        val values = FileBasedIndex.getInstance().getValues(TranslationIndex.NAME, "en_us", GlobalSearchScope.projectScope(e.project
            ?: return))
        println(values.merge(""))
        println((element.parent.parent.toUElement() as? UExpression)?.uValueOf())
        println(TranslationInverseIndex.findElements("test", GlobalSearchScope.projectScope(element.project)))
        return
        ConvertToTranslationIntention().invoke(editor.project ?: return, editor, element)
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(LangDataKeys.PSI_FILE)
        val editor = e.getData(PlatformDataKeys.EDITOR)
        if (file == null || editor == null) {
            e.presentation.isEnabled = false
            return
        }
        val element = file.findElementAt(editor.caretModel.offset)
        e.presentation.isEnabled = (element?.parent as? PsiLiteral)?.value is String
    }
}
