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

package com.demonwav.mcdev.creator.buildsystem

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.getVersionJson
import com.demonwav.mcdev.creator.notifyCreatedProjectNotOpened
import com.demonwav.mcdev.creator.step.AbstractLongRunningStep
import com.demonwav.mcdev.creator.step.AbstractModNameStep
import com.demonwav.mcdev.creator.step.AbstractReformatFilesStep
import com.demonwav.mcdev.creator.step.DescriptionStep
import com.demonwav.mcdev.creator.step.FixedAssetsNewProjectWizardStep
import com.demonwav.mcdev.creator.step.WebsiteStep
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.execution.RunManager
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.util.xml.DomManager
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.jetbrains.idea.maven.dom.model.MavenDomProjectModel
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.project.importing.MavenImportingManager

private val pluginVersions by lazy {
    runBlocking {
        getVersionJson<Map<String, String>>("maven.json")
            .mapKeys { (k, _) -> k.replace('-', '_') }
    }
}

fun FixedAssetsNewProjectWizardStep.addDefaultMavenProperties() {
    addTemplateProperties(pluginVersions)
}

abstract class AbstractPatchPomStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description
        get() = MCDevBundle("creator.step.maven.patch_pom.description")

    open fun patchPom(model: MavenDomProjectModel, root: XmlTag) {
        setupCore(model)
        setupName(model)
        setupInfo(root)
    }

    protected fun setupCore(model: MavenDomProjectModel) {
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        model.groupId.value = buildSystemProps.groupId
        model.artifactId.value = buildSystemProps.artifactId
        model.version.value = buildSystemProps.version
    }

    protected fun setupName(model: MavenDomProjectModel) {
        val name = data.getUserData(AbstractModNameStep.KEY) ?: return
        model.name.value = name
    }

    protected fun setupInfo(root: XmlTag) {
        val website = data.getUserData(WebsiteStep.KEY)
        val description = data.getUserData(DescriptionStep.KEY)

        val properties = root.findFirstSubTag("properties")
        if (!website.isNullOrBlank()) {
            val url = root.createChildTag("url", null, website, false)
            root.addAfter(url, properties)
        }

        if (!description.isNullOrBlank()) {
            val descriptionTag = root.createChildTag("description", null, description, false)
            root.addBefore(descriptionTag, properties)
        }
    }

    protected fun setupDependencies(
        model: MavenDomProjectModel,
        repositories: List<BuildRepository>,
        dependencies: List<BuildDependency>,
    ) {
        for ((id, url, types) in repositories) {
            if (!types.contains(BuildSystemType.MAVEN)) {
                continue
            }
            val repository = model.repositories.addRepository()
            repository.id.value = id
            repository.url.value = url
        }

        for ((depGroupId, depArtifactId, depVersion, scope) in dependencies) {
            if (scope == null) {
                continue
            }
            val dependency = model.dependencies.addDependency()
            dependency.groupId.value = depGroupId
            dependency.artifactId.value = depArtifactId
            dependency.version.value = depVersion
            dependency.scope.value = scope
        }
    }

    override fun perform(project: Project) {
        invokeAndWait {
            if (project.isDisposed || !project.isInitialized) {
                notifyCreatedProjectNotOpened()
                return@invokeAndWait
            }

            runWriteTask {
                val pomFile = VfsUtil.findFile(Path.of(context.projectFileDirectory, "pom.xml"), true)
                    ?: return@runWriteTask
                val pomPsi = PsiManager.getInstance(project).findFile(pomFile) as? XmlFile ?: return@runWriteTask

                pomPsi.name = "pom.xml"

                NonProjectFileWritingAccessProvider.disableChecksDuring {
                    pomPsi.runWriteAction {
                        val manager = DomManager.getDomManager(project)
                        val mavenProjectXml =
                            manager.getFileElement(pomPsi, MavenDomProjectModel::class.java)?.rootElement
                                ?: return@runWriteAction

                        val root = pomPsi.rootTag ?: return@runWriteAction

                        patchPom(mavenProjectXml, root)

                        // The maven importer requires that the document is saved to disk
                        val document = PsiDocumentManager.getInstance(project).getDocument(pomPsi)
                            ?: return@runWriteAction
                        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
                        FileDocumentManager.getInstance().saveDocument(document)
                    }
                }
            }
        }
    }
}

class ReformatPomStep(parent: NewProjectWizardStep) : AbstractReformatFilesStep(parent) {
    override fun addFilesToReformat() {
        addFileToReformat("pom.xml")
    }
}

class MavenImportStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description
        get() = MCDevBundle("creator.step.maven.import_maven.description")

    override fun perform(project: Project) {
        val pomFile = VfsUtil.findFile(Path.of(context.projectFileDirectory).resolve("pom.xml"), true)
            ?: return
        val promise = invokeAndWait {
            if (project.isDisposed || !project.isInitialized) {
                notifyCreatedProjectNotOpened()
                return@invokeAndWait null
            }
            MavenImportingManager.getInstance(project).linkAndImportFile(pomFile)
        } ?: return

        promise.finishPromise.blockingGet(Int.MAX_VALUE, TimeUnit.SECONDS)

        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()
        addRunTaskConfiguration(project, buildSystemProps, "package")
    }

    private fun addRunTaskConfiguration(
        project: Project,
        buildSystemProps: BuildSystemPropertiesStep<*>,
        task: String,
    ) {
        val mavenConfigFactory = MavenRunConfigurationType.getInstance().configurationFactories.first()

        val runManager = RunManager.getInstance(project)
        val runConfigName = buildSystemProps.artifactId + ' ' + task

        val templateConfig = mavenConfigFactory.createTemplateConfiguration(project)
        val runConfiguration = mavenConfigFactory.createConfiguration(runConfigName, templateConfig)
            as MavenRunConfiguration
        runConfiguration.runnerParameters.goals.add(task)

        runConfiguration.isAllowRunningInParallel = false

        val settings = runManager.createConfiguration(
            runConfiguration,
            mavenConfigFactory,
        )

        settings.isActivateToolWindowBeforeRun = true
        settings.storeInLocalWorkspace()

        runManager.addConfiguration(settings)
        if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = settings
        }
    }
}
