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
import com.demonwav.mcdev.platform.canary.CanaryTemplate
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

class MavenBuildSystem : BuildSystem() {

    private lateinit var pomFile: VirtualFile

    override fun create(project: Project, configuration: ProjectConfiguration, indicator: ProgressIndicator) {
        rootDirectory.refresh(false, true)

        runWriteTask {
            createDirectories()

            val text = when (configuration.type) {
                PlatformType.BUKKIT, PlatformType.SPIGOT, PlatformType.PAPER ->
                    BukkitTemplate.applyPomTemplate(project, buildVersion)
                PlatformType.BUNGEECORD, PlatformType.WATERFALL -> BungeeCordTemplate.applyPomTemplate(project, buildVersion)
                PlatformType.SPONGE -> SpongeTemplate.applyPomTemplate(project, buildVersion)
                PlatformType.CANARY, PlatformType.NEPTUNE ->
                    CanaryTemplate.applyPomTemplate(project, buildVersion)
                else -> return@runWriteTask
            }

            val pomPsi = PsiFileFactory.getInstance(project).createFileFromText(XMLLanguage.INSTANCE, text) ?: return@runWriteTask

            pomPsi.name = "pom.xml"

            val pomXmlPsi = pomPsi as XmlFile
            pomPsi.runWriteAction {
                val manager = DomManager.getDomManager(project)
                val mavenProjectXml = manager.getFileElement(pomXmlPsi, MavenDomProjectModel::class.java)!!.rootElement

                mavenProjectXml.groupId.value = groupId
                mavenProjectXml.artifactId.value = artifactId
                mavenProjectXml.version.value = version
                mavenProjectXml.name.value = pluginName

                val root = pomXmlPsi.rootTag ?: return@runWriteAction

                val properties = root.findFirstSubTag("properties") ?: return@runWriteAction

                if (!configuration.website.isNullOrEmpty()) {
                    val url = root.createChildTag("url", null, configuration.website, false)
                    root.addAfter(url, properties)
                }

                if (configuration.description.isNotEmpty()) {
                    val description = root.createChildTag("description", null, configuration.description, false)
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

                pomFile = rootDirectory.findChild("pom.xml") ?: return@runWriteAction
                // Reformat the code to match their code style
                PsiManager.getInstance(project).findFile(pomFile)?.let {
                    ReformatCodeProcessor(it, false).run()
                }
            }
        }
    }

    override fun finishSetup(rootModule: Module, configurations: Collection<ProjectConfiguration>, indicator: ProgressIndicator) {
        runWriteTask {
            val project = rootModule.project

            // Force Maven to setup the project
            val manager = MavenProjectsManager.getInstance(project)
            manager.addManagedFilesOrUnignore(listOf(pomFile))
            manager.importingSettings.isDownloadDocsAutomatically = true
            manager.importingSettings.isDownloadSourcesAutomatically = true

            // Setup the default Maven run config
            val params = MavenRunnerParameters()
            params.workingDirPath = rootDirectory.canonicalPath!!
            params.goals = listOf("clean", "package")
            val runnerSettings = MavenRunConfigurationType
                .createRunnerAndConfigurationSettings(null, null, params, project)
            runnerSettings.name = rootModule.name + " build"
            RunManager.getInstance(project).addConfiguration(runnerSettings, false)
        }
    }
}
