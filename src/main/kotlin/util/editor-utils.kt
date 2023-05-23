/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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
