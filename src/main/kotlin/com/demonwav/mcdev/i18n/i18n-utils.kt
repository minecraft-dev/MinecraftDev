/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.i18n.lang.I18nFile
import com.demonwav.mcdev.i18n.lang.I18nFileType
import com.demonwav.mcdev.i18n.lang.gen.psi.I18nEntry
import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import java.util.Locale

enum class Scope {
    GLOBAL, PROJECT
}

private fun Project.files(scope: Scope): Sequence<I18nFile> {
    val searchScope =
        if (scope == Scope.GLOBAL) GlobalSearchScope.allScope(this) else GlobalSearchScope.projectScope(this)
    return FileTypeIndex.getFiles(I18nFileType, searchScope)
        .asSequence()
        .mapNotNull { PsiManager.getInstance(this).findFile(it) as? I18nFile }
}

private fun Project.findEntriesImpl(
    scope: Scope,
    fileFilter: (I18nFile) -> Boolean = { true },
    entryFilter: (I18nEntry) -> Boolean = { true }
) =
    files(scope)
        .filter(fileFilter)
        .flatMap { PsiTreeUtil.getChildrenOfType(it, I18nEntry::class.java)?.asSequence() ?: emptySequence() }
        .filter(entryFilter)
        .toList()

fun Project.findLangEntries(
    scope: Scope = Scope.GLOBAL,
    key: String? = null,
    file: VirtualFile? = null,
    domain: String? = null
) =
    findEntriesImpl(
        scope,
        {
            it.virtualFile != null &&
                (file == null || it.virtualFile.path == file.path) &&
                (domain == null || it.virtualFile.mcDomain == domain)
        },
        { key == null || it.key == key }
    )

fun Project.findDefaultLangEntries(
    scope: Scope = Scope.GLOBAL,
    key: String? = null,
    file: VirtualFile? = null,
    domain: String? = null
) =
    findEntriesImpl(
        scope,
        {
            it.virtualFile != null &&
                it.virtualFile.nameWithoutExtension.toLowerCase(Locale.ROOT) == I18nConstants.DEFAULT_LOCALE &&
                (file == null || it.virtualFile.path == file.path) &&
                (domain == null || it.virtualFile.mcDomain == domain)
        },
        { key == null || it.key == key }
    )

fun Project.findDefaultLangFile(domain: String? = null) =
    FilenameIndex.getVirtualFilesByName(
        this,
        I18nConstants.DEFAULT_LOCALE_FILE,
        false,
        GlobalSearchScope.projectScope(this)
    ).firstOrNull { domain == null || it.mcDomain == domain }
