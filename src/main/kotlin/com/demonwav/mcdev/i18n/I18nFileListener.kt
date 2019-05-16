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

import com.demonwav.mcdev.i18n.translations.TranslationFiles
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.EditorNotifications

object I18nFileListener : BulkFileListener {
    override fun before(events: List<VFileEvent>) {}

    override fun after(events: List<VFileEvent>) {
        if (events.any { TranslationFiles.isTranslationFile(it.file ?: return@any false) }) {
            EditorNotifications.updateAll()
        }
    }
}
