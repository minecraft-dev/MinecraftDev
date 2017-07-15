/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.lang.I18nFile
import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nProperty
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory

object I18nElementFactory {
    fun createFile(project: Project, text: String): I18nFile {
        return PsiFileFactory.getInstance(project).createFileFromText("name", I18nFileType, text) as I18nFile
    }

    fun createProperty(project: Project, key: String, value: String = ""): I18nProperty {
        val file = createFile(project, "$key=$value")
        return file.firstChild as I18nProperty
    }

    fun createLineEnding(project: Project): PsiElement {
        val file = createFile(project, "\n")
        return file.firstChild
    }
}