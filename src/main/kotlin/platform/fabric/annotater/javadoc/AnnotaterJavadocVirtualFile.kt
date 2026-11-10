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
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiUtil
import com.intellij.testFramework.LightVirtualFile

/**
 * The copy of a Minecraft source file that an annotater Javadoc is edited in. It's never opened as a tab, but shown
 * through an [AnnotaterJavadocInlineEditor]'s inline editor, which only shows the Javadoc of [target].
 */
class AnnotaterJavadocVirtualFile(
    val checkout: AnnotaterCheckout,
    val copy: AnnotaterJavadocSourceCopy,
    val target: AnnotaterJavadocTarget,
) : LightVirtualFile(copy.fileName, JavaFileType.INSTANCE, copy.text) {
    /**
     * The reason the last save failed, shown in the editor banner.
     */
    @Volatile
    var error: String? = null

    val outerClass: String
        get() = copy.outerClass

    init {
        putUserData(PsiUtil.FILE_LANGUAGE_LEVEL_KEY, LanguageLevel.HIGHEST)
    }

    companion object {
        fun of(file: PsiFile?): AnnotaterJavadocVirtualFile? =
            file?.viewProvider?.virtualFile as? AnnotaterJavadocVirtualFile
    }
}
