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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.FileBasedIndex

enum class Scope {
    GLOBAL, PROJECT
}

private fun Project.files(scope: Scope) =
    FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, I18nFileType, if (scope == Scope.GLOBAL) GlobalSearchScope.allScope(this) else GlobalSearchScope.projectScope(this))
        .map { PsiManager.getInstance(this).findFile(it) as I18nFile? }
        .filter { it != null }
        .map { it as I18nFile }

private fun Project.findPropertiesImpl(scope: Scope, fileFilter: (I18nFile) -> Boolean = { true }, propertyFilter: (I18nProperty) -> Boolean = { true }) =
    files(scope)
        .filter(fileFilter)
        .flatMap { (PsiTreeUtil.getChildrenOfType(it, I18nProperty::class.java) ?: emptyArray()).asIterable() }
        .filter(propertyFilter)
        .toList()

fun Project.findProperties(scope: Scope = Scope.GLOBAL, key: String? = null, file: VirtualFile? = null, domain: String? = null) =
    findPropertiesImpl(scope,
        {
            it.virtualFile != null
                && (if (file != null) it.virtualFile.path == file.path else true)
                && (if (domain != null) I18nElementFactory.getResourceDomain(it.virtualFile) == domain else true)
        },
        { if (key != null) it.key == key else true })

fun Project.findDefaultProperties(scope: Scope = Scope.GLOBAL, key: String? = null, file: VirtualFile? = null, domain: String? = null) =
    findPropertiesImpl(scope,
        {
            it.virtualFile != null && it.virtualFile.nameWithoutExtension.toLowerCase() == I18nConstants.DEFAULT_LOCALE
                && (if (file != null) it.virtualFile.path == file.path else true)
                && (if (domain != null) I18nElementFactory.getResourceDomain(it.virtualFile) == domain else true)
        },
        { if (key != null) it.key == key else true })
