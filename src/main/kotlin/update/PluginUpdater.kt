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

package com.demonwav.mcdev.update

import com.demonwav.mcdev.util.McdevReflection
import com.demonwav.mcdev.util.invokeLater
import com.intellij.ide.plugins.PluginManagerMain
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.updateSettings.PluginUpdateCheckService
import com.intellij.openapi.updateSettings.PluginUpdateInfo
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import java.io.IOException

object PluginUpdater {
    fun runUpdateCheck(callback: (Result<PluginUpdateInfo>) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val result = runCatching { PluginUpdateCheckService.getInstance().getPluginUpdate(PluginUtil.PLUGIN_ID) }
            ApplicationManager.getApplication().invokeLater({ callback(result) }, ModalityState.any())
        }
    }

    fun installPluginUpdate(downloader: PluginDownloader) {
        ProgressManager.getInstance().run(
            object : Task.Backgroundable(null, "Downloading plugin", true) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        val status = downloader.prepareToInstall(indicator)
                        // If the download failed, quit
                        // But otherwise force the install
                        if (!status && McdevReflection.FIELD_PLUGIN_DOWNLOADER_MY_FILE.get(downloader) == null) {
                            return
                        }

                        downloader.install()

                        invokeLater {
                            PluginManagerMain.notifyPluginsUpdated(null)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            },
        )
    }
}
