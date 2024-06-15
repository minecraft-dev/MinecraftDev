/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.custom.BuiltinValidations
import com.demonwav.mcdev.creator.modalityState
import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.refreshSync
import com.demonwav.mcdev.util.virtualFile
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.getOrNull
import com.github.kittinunf.result.onError
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.observable.util.trim
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import com.intellij.util.io.ZipUtil
import com.intellij.util.io.createDirectories
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.writeBytes

open class RemoteTemplateProvider : TemplateProvider {

    private var updatedTemplates = mutableSetOf<String>()

    override val label: String = MCDevBundle("template.provider.remote.label")

    override val hasConfig: Boolean = true

    override fun init(indicator: ProgressIndicator, repos: List<MinecraftSettings.TemplateRepo>) {
        for (repo in repos) {
            ProgressManager.checkCanceled()
            val remote = RemoteTemplateRepo.deserialize(repo.data)
                ?: continue
            if (!remote.autoUpdate || remote.url in updatedTemplates) {
                continue
            }

            if (doUpdateRepo(indicator, repo.name, remote.url, remote.getDestination(repo.name))) {
                updatedTemplates.add(remote.url)
            }
        }
    }

    protected fun doUpdateRepo(
        indicator: ProgressIndicator,
        repoName: String,
        repoUrl: String,
        destination: Path
    ): Boolean {
        indicator.text2 = "Updating remote repository $repoName"

        val manager = FuelManager()
        manager.proxy = selectProxy(repoUrl)
        val (_, _, result) = manager.get(repoUrl)
            .header("User-Agent", "github_org/minecraft-dev/${PluginUtil.pluginVersion}")
            .header("Accepts", "application/json")
            .timeout(10000)
            .response()

        val data = result.onError {
            thisLogger().warn("Could not fetch remote templates repository update at $repoUrl", it)
        }.getOrNull() ?: return false

        try {
            val remoteTemplatesDir = destination
            remoteTemplatesDir.createDirectories()
            val zipPath = remoteTemplatesDir.resolveSibling("$repoName.zip")
            zipPath.writeBytes(data)
            FileUtil.deleteRecursively(remoteTemplatesDir)
            ZipUtil.extract(zipPath, remoteTemplatesDir, null)

            // Loose way to find out if the url is a github repo archive
            // In such cases there is a single directory in the root directory of the zip
            // We simply move all its children to the base directory so the rest of the system uses the correct
            // root directory for this repository
            val githubRepoArchiveRegex = "https://github\\.com/(.*?)/(.*?)/archive/refs/heads/(.*?).zip".toRegex()
            val githubRepoArchiveMatcher = githubRepoArchiveRegex.matchEntire(repoUrl)
            if (githubRepoArchiveMatcher != null) {
                val githubRepoName = githubRepoArchiveMatcher.groupValues[2]
                val branchName = githubRepoArchiveMatcher.groupValues[3]
                for (child in remoteTemplatesDir.resolve("$githubRepoName-$branchName").listDirectoryEntries()) {
                    child.moveTo(remoteTemplatesDir.resolve(child.fileName))
                }
            }

            thisLogger().info("Remote templates repository update applied successfully")
            return true
        } catch (t: Throwable) {
            if (t is ControlFlowException) {
                throw t
            }
            thisLogger().error("Failed to apply remote templates repository update of $repoName", t)
        }
        return false
    }

    override fun loadTemplates(
        context: WizardContext,
        repo: MinecraftSettings.TemplateRepo
    ): Collection<LoadedTemplate> {
        val remote = RemoteTemplateRepo.deserialize(repo.data)
            ?: return emptyList()
        val repoRoot = remote.getDestination(repo.name).virtualFile
            ?: return emptyList()
        val modalityState = context.modalityState
        repoRoot.refreshSync(modalityState)
        return TemplateProvider.findTemplates(modalityState, repoRoot)
    }

    override fun setupConfigUi(
        data: String,
        dataSetter: (String) -> Unit
    ): JComponent? {
        val propertyGraph = PropertyGraph("RemoteTemplateProvider config")
        val defaultRepo = RemoteTemplateRepo.deserialize(data)
        val urlProperty = propertyGraph.property(defaultRepo?.url ?: "").trim()
        val autoUpdateProperty = propertyGraph.property(defaultRepo?.autoUpdate != false)

        return panel {
            row(MCDevBundle("creator.ui.custom.remote.url.label")) {
                textField()
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(urlProperty)
                    .textValidation(BuiltinValidations.nonBlank)
            }

            row {
                checkBox(MCDevBundle("creator.ui.custom.remote.auto_update.label"))
                    .bindSelected(autoUpdateProperty)
            }

            onApply {
                val repo = RemoteTemplateRepo(urlProperty.get(), autoUpdateProperty.get())
                dataSetter(repo.serialize())
            }
        }
    }

    data class RemoteTemplateRepo(val url: String, val autoUpdate: Boolean) {

        fun getDestination(repoName: String): Path {
            return PathManager.getSystemDir().resolve("mcdev-templates").resolve(repoName)
        }

        fun serialize(): String = "$url\n$autoUpdate"

        companion object {
            fun deserialize(data: String): RemoteTemplateRepo? {
                val lines = data.lines()
                return when (lines.size) {
                    0 -> null
                    1 -> RemoteTemplateRepo(lines[0], true)
                    else -> {
                        val (url, autoUpdate) = lines
                        RemoteTemplateRepo(url, autoUpdate.toBoolean())
                    }
                }
            }
        }
    }
}
