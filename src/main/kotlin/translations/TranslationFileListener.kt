/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations

import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.EditorNotifications

object TranslationFileListener : BulkFileListener {
    override fun before(events: List<VFileEvent>) {}

    override fun after(events: List<VFileEvent>) {
        if (events.any { TranslationFiles.isTranslationFile(it.file ?: return@any false) }) {
            EditorNotifications.updateAll()
        }
    }
}
