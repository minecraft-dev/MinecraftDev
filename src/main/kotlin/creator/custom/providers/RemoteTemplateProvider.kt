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
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.modalityState
import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.refreshSync
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
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.textValidation
import com.intellij.util.io.createDirectories
import java.nio.file.Path
import javax.swing.JComponent
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
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

            if (doUpdateRepo(indicator, repo.name, remote.url)) {
                updatedTemplates.add(remote.url)
            }
        }
    }

    protected fun doUpdateRepo(
        indicator: ProgressIndicator,
        repoName: String,
        originalRepoUrl: String
    ): Boolean {
        indicator.text2 = "Updating remote repository $repoName"

        val repoUrl = replaceVariables(originalRepoUrl)

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
            val zipPath = RemoteTemplateRepo.getDestinationZip(repoName)
            zipPath.parent.createDirectories()
            zipPath.writeBytes(data)

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
        val remoteRepo = RemoteTemplateRepo.deserialize(repo.data)
            ?: return emptyList()
        return doLoadTemplates(context, repo, remoteRepo.innerPath)
    }

    protected fun doLoadTemplates(
        context: WizardContext,
        repo: MinecraftSettings.TemplateRepo,
        rawInnerPath: String
    ): List<LoadedTemplate> {
        val remoteRootPath = RemoteTemplateRepo.getDestinationZip(repo.name)
        if (!remoteRootPath.exists()) {
            return emptyList()
        }

        val archiveRoot = remoteRootPath.absolutePathString() + JarFileSystem.JAR_SEPARATOR

        val fs = JarFileSystem.getInstance()
        val rootFile = fs.refreshAndFindFileByPath(archiveRoot)
            ?: return emptyList()
        val modalityState = context.modalityState
        rootFile.refreshSync(modalityState)

        val innerPath = replaceVariables(rawInnerPath)
        val repoRoot = if (innerPath.isNotBlank()) {
            rootFile.findFileByRelativePath(innerPath)
        } else {
            rootFile
        }

        if (repoRoot == null) {
            return emptyList()
        }

        return TemplateProvider.findTemplates(modalityState, repoRoot)
    }

    private fun replaceVariables(originalRepoUrl: String): String =
        originalRepoUrl.replace("\$version", TemplateDescriptor.FORMAT_VERSION.toString())

    override fun setupConfigUi(
        data: String,
        dataSetter: (String) -> Unit
    ): JComponent? {
        val propertyGraph = PropertyGraph("RemoteTemplateProvider config")
        val defaultRepo = RemoteTemplateRepo.deserialize(data)
        val urlProperty = propertyGraph.property(defaultRepo?.url ?: "").trim()
        val autoUpdateProperty = propertyGraph.property(defaultRepo?.autoUpdate != false)
        val innerPathProperty = propertyGraph.property(defaultRepo?.innerPath ?: "").trim()

        return panel {
            row(MCDevBundle("creator.ui.custom.remote.url.label")) {
                textField()
                    .comment(MCDevBundle("creator.ui.custom.remote.url.comment"))
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(urlProperty)
                    .textValidation(BuiltinValidations.nonBlank)
            }

            row(MCDevBundle("creator.ui.custom.remote.inner_path.label")) {
                textField()
                    .comment(MCDevBundle("creator.ui.custom.remote.inner_path.comment"))
                    .align(AlignX.FILL)
                    .columns(COLUMNS_LARGE)
                    .bindText(innerPathProperty)
            }

            row {
                checkBox(MCDevBundle("creator.ui.custom.remote.auto_update.label"))
                    .bindSelected(autoUpdateProperty)
            }

            onApply {
                val repo = RemoteTemplateRepo(urlProperty.get(), autoUpdateProperty.get(), innerPathProperty.get())
                dataSetter(repo.serialize())
            }
        }
    }

    data class RemoteTemplateRepo(val url: String, val autoUpdate: Boolean, val innerPath: String) {

        fun serialize(): String = "$url\n$autoUpdate\n$innerPath"

        companion object {

            val templatesBaseDir: Path
                get() = PathManager.getSystemDir().resolve("mcdev-templates")

            fun getDestinationZip(repoName: String): Path {
                return templatesBaseDir.resolve("$repoName.zip")
            }

            fun deserialize(data: String): RemoteTemplateRepo? {
                if (data.isBlank()) {
                    return null
                }

                val lines = data.lines()
                return RemoteTemplateRepo(
                    lines[0],
                    lines.getOrNull(1).toBoolean(),
                    lines.getOrNull(2) ?: "",
                )
            }
        }
    }
}
