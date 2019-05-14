/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.i18n

import com.demonwav.mcdev.util.mcDomain
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

fun Project.findDefaultLangFile(domain: String? = null) =
    FilenameIndex.getVirtualFilesByName(
        this,
        I18nConstants.DEFAULT_LOCALE_FILE,
        false,
        GlobalSearchScope.projectScope(this)
    ).firstOrNull { domain == null || it.mcDomain == domain }
