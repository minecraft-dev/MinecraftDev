/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.maven

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.platform.bukkit.BukkitTemplate
import com.demonwav.mcdev.platform.bungeecord.BungeeCordTemplate
import com.demonwav.mcdev.platform.sponge.SpongeTemplate
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.RunManager
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.util.xml.DomManager
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.project.MavenProjectsManager

class MavenBuildSystem(
    artifactId: String,
    groupId: String,
    version: String
) : BuildSystem(artifactId, groupId, version) {

    override fun create(
        project: Project,
        rootDirectory: VirtualFile,
        configuration: ProjectConfiguration,
        indicator: ProgressIndicator,
        pluginName: String
    ) {
        if (project.isDisposed) {
            return
        }
        rootDirectory.refresh(false, true)

        runWriteTask {
            directories = createDirectories(rootDirectory)

            val text = when (configuration.type) {
                PlatformType.BUKKIT, PlatformType.SPIGOT, PlatformType.PAPER ->
                    BukkitTemplate.applyPomTemplate(project)
                PlatformType.BUNGEECORD, PlatformType.WATERFALL ->
                    BungeeCordTemplate.applyPomTemplate(project)
                PlatformType.SPONGE ->
                    SpongeTemplate.applyPomTemplate(project)
                else -> return@runWriteTask
            }

            val pomPsi = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, text)
                ?: return@runWriteTask

            pomPsi.name = "pom.xml"

            val pomXmlPsi = pomPsi as XmlFile
            pomPsi.runWriteAction {
                val manager = DomManager.getDomManager(project)
                val mavenProjectXml = manager.getFileElement(pomXmlPsi, MavenDomProjectModel::class.java)?.rootElement
                    ?: return@runWriteAction

                mavenProjectXml.groupId.value = groupId
                mavenProjectXml.artifactId.value = artifactId
                mavenProjectXml.version.value = version
                mavenProjectXml.name.value = pluginName

                val root = pomXmlPsi.rootTag ?: return@runWriteAction

                val properties = root.findFirstSubTag("properties") ?: return@runWriteAction

                val base = configuration.base ?: return@runWriteAction

                if (!base.website.isNullOrEmpty()) {
                    val url = root.createChildTag("url", null, base.website, false)
                    root.addAfter(url, properties)
                }

                if (!base.description.isNullOrEmpty()) {
                    val description = root.createChildTag("description", null, base.description, false)
                    root.addBefore(description, properties)
                }

                for ((id, url) in repositories) {
                    val repository = mavenProjectXml.repositories.addRepository()
                    repository.id.value = id
                    repository.url.value = url
                }

                for ((depArtifactId, depGroupId, depVersion, scope) in dependencies) {
                    val dependency = mavenProjectXml.dependencies.addDependency()
                    dependency.groupId.value = depGroupId
                    dependency.artifactId.value = depArtifactId
                    dependency.version.value = depVersion
                    dependency.scope.value = scope
                }

                val dir = PsiManager.getInstance(project).findDirectory(rootDirectory) ?: return@runWriteAction
                dir.findFile(pomPsi.name)?.delete()
                dir.add(pomPsi)

                val pomFile = rootDirectory.findChild(pomPsi.name) ?: return@runWriteAction

                // Reformat the code to match their code style
                PsiManager.getInstance(project).findFile(pomFile)?.let {
                    ReformatCodeProcessor(it, false).run()
                }
            }
        }
    }

    override fun finishSetup(
        rootModule: Module,
        rootDirectory: VirtualFile,
        configurations: Collection<ProjectConfiguration>,
        indicator: ProgressIndicator
    ) {
        if (rootModule.isDisposed || rootModule.project.isDisposed) {
            return
        }
        runWriteTask {
            val project = rootModule.project

            val pomFile = rootDirectory.findChild("pom.xml") ?: return@runWriteTask

            // Force Maven to setup the project
            val manager = MavenProjectsManager.getInstance(project)
            manager.addManagedFilesOrUnignore(listOf(pomFile))
            manager.importingSettings.isDownloadDocsAutomatically = true
            manager.importingSettings.isDownloadSourcesAutomatically = true

            // Setup the default Maven run config
            val params = MavenRunnerParameters()
            params.workingDirPath = rootDirectory.canonicalPath ?: return@runWriteTask
            params.goals = listOf("clean", "package")
            val runnerSettings = MavenRunConfigurationType
                .createRunnerAndConfigurationSettings(null, null, params, project)
            runnerSettings.name = rootModule.name + " build"
            RunManager.getInstance(project).addConfiguration(runnerSettings, false)
        }
    }
}
