/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem.maven

import com.demonwav.mcdev.creator.AbstractLongRunningStep
import com.demonwav.mcdev.creator.AbstractModNameStep
import com.demonwav.mcdev.creator.AbstractReformatFilesStep
import com.demonwav.mcdev.creator.DescriptionStep
import com.demonwav.mcdev.creator.FixedAssetsNewProjectWizardStep
import com.demonwav.mcdev.creator.WebsiteStep
import com.demonwav.mcdev.creator.buildsystem.BuildDependency
import com.demonwav.mcdev.creator.buildsystem.BuildRepository
import com.demonwav.mcdev.creator.buildsystem.BuildSystemPropertiesStep
import com.demonwav.mcdev.creator.buildsystem.BuildSystemType
import com.demonwav.mcdev.creator.findStep
import com.demonwav.mcdev.creator.getVersionJson
import com.demonwav.mcdev.util.invokeAndWait
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
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
import org.jetbrains.idea.maven.project.importing.MavenImportingManager

private val pluginVersions by lazy {
    runBlocking {
        getVersionJson<Map<String, String>>("maven.json")
    }
}

fun FixedAssetsNewProjectWizardStep.addDefaultMavenProperties() {
    addTemplateProperties(pluginVersions)
}

abstract class AbstractPatchPomStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description = "Patching pom.xml"

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
        dependencies: List<BuildDependency>
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
            if (project.isDisposed) {
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
    override val description = "Importing Maven project"

    override fun perform(project: Project) {
        val pomFile = VfsUtil.findFile(Path.of(context.projectFileDirectory).resolve("pom.xml"), true)
            ?: return
        val promise = invokeAndWait {
            if (project.isDisposed) {
                return@invokeAndWait null
            }
            MavenImportingManager.getInstance(project).linkAndImportFile(pomFile)
        } ?: return

        promise.blockingGet(Int.MAX_VALUE, TimeUnit.SECONDS)
    }
}
