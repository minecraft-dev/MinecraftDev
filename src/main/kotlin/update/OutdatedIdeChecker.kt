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

import com.demonwav.mcdev.asset.MCDevBundle
import com.google.gson.JsonParser
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.HttpRequests
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Warns the user once per IDE session when their IDE is so far behind the latest stable release that this plugin no
 * longer publishes updates for it (e.g. running 2025.3 once 2026.3 is released).
 */
class OutdatedIdeChecker : ProjectActivity {
    override suspend fun execute(project: Project) {
        if (!Registry.`is`("mcdev.outdated.ide.warning") || !checked.compareAndSet(false, true)) {
            return
        }

        val currentBuild = ApplicationInfo.getInstance().build
        if (currentBuild.isSnapshot) {
            return
        }

        val latestBuild = try {
            withContext(Dispatchers.IO) { fetchLatestReleaseBuild() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LOGGER.info("Failed to check for the latest IDE release", e)
            return
        } ?: return

        if (releaseIndex(latestBuild) - releaseIndex(currentBuild) < UNSUPPORTED_RELEASE_DISTANCE) {
            return
        }

        NotificationGroupManager.getInstance().getNotificationGroup("Minecraft Development updates")
            .createNotification(
                MCDevBundle("update.outdated_ide.title"),
                MCDevBundle("update.outdated_ide.content", ApplicationInfo.getInstance().fullApplicationName),
                NotificationType.WARNING,
            )
            .setImportant(true)
            .notify(project)
    }

    private fun fetchLatestReleaseBuild(): BuildNumber? {
        val text = HttpRequests.request(RELEASES_URL)
            .userAgent(PluginUtil.useragent)
            .readString()
        val build = JsonParser.parseString(text).asJsonObject
            .getAsJsonArray(PRODUCT_CODE)
            ?.firstOrNull()
            ?.asJsonObject
            ?.get("build")
            ?.asString
            ?: return null
        return BuildNumber.fromStringOrNull(build)
    }

    /**
     * Maps a build baseline (e.g. 253 for 2025.3) to a sequential release index, so that consecutive major releases
     * differ by exactly 1 (253 -> 261 -> 262 -> 263).
     */
    private fun releaseIndex(build: BuildNumber): Int {
        val baseline = build.baselineVersion
        return (baseline / 10) * 3 + (baseline % 10 - 1)
    }

    private companion object {
        val LOGGER = logger<OutdatedIdeChecker>()

        // All JetBrains IDEs share the same platform build numbers, so IntelliJ IDEA's releases are representative
        const val PRODUCT_CODE = "IIU"
        const val RELEASES_URL =
            "https://data.services.jetbrains.com/products/releases?code=$PRODUCT_CODE&latest=true&type=release"

        // e.g. 2025.3 -> 2026.3 is 3 releases apart (2026.1, 2026.2, 2026.3)
        const val UNSUPPORTED_RELEASE_DISTANCE = 3

        val checked = AtomicBoolean(false)
    }
}
