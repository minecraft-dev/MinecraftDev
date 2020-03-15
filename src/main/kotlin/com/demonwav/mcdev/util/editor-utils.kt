/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.ide.util.EditSourceUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

fun gotoTargetElement(element: PsiElement, currentEditor: Editor, currentFile: PsiFile) {
    if (element.containingFile === currentFile) {
        val offset = element.textOffset
        val leaf = currentFile.findElementAt(offset)
        // check that element is really physically inside the file
        // there are fake elements with custom navigation (e.g. opening URL in browser) that override getContainingFile for various reasons
        if (leaf != null && PsiTreeUtil.isAncestor(element, leaf, false)) {
            val project = element.project
            IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation()
            OpenFileDescriptor(project, currentFile.viewProvider.virtualFile, offset).navigateIn(currentEditor)
            return
        }
    }

    val navigatable = if (element is Navigatable) element else EditSourceUtil.getDescriptor(element)
    if (navigatable != null && navigatable.canNavigate()) {
        navigatable.navigate(true)
    }
}
