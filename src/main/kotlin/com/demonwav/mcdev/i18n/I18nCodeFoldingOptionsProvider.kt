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

import com.intellij.application.options.editor.CodeFoldingOptionsProvider
import com.intellij.openapi.options.BeanConfigurable

class I18nCodeFoldingOptionsProvider : BeanConfigurable<I18nFoldingSettings>(I18nFoldingSettings.instance), CodeFoldingOptionsProvider {
    init {
        checkBox("Minecraft Translations", instance::shouldFoldTranslations, { instance.shouldFoldTranslations = it })
    }
}
