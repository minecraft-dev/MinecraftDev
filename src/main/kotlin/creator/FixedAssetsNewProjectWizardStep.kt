/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.util.invokeLater
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectWizard.generators.AssetsNewProjectWizardStep
import com.intellij.ide.starters.local.GeneratorAsset
import com.intellij.ide.starters.local.GeneratorEmptyDirectory
import com.intellij.ide.starters.local.GeneratorResourceFile
import com.intellij.ide.starters.local.GeneratorTemplateFile
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.NioFiles
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Fixed version of [AssetsNewProjectWizardStep], to be removed in 2022.3 when
 * [IDEA-297489 is fixed](https://github.com/JetBrains/intellij-community/commit/fefae70bf621f3181ee9f2d0815c43d0325cd6c4),
 */
abstract class FixedAssetsNewProjectWizardStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    lateinit var outputDirectory: String
    private val assets = arrayListOf<GeneratorAsset>()
    private val templateProperties = hashMapOf<String, Any>()
    private val filesToOpen = hashSetOf<String>()

    fun addAssets(vararg assets: GeneratorAsset) = addAssets(assets.toList())

    fun addAssets(assets: Iterable<GeneratorAsset>) {
        this.assets.addAll(assets)
    }

    fun addTemplateProperties(vararg properties: Pair<String, Any>) = addTemplateProperties(properties.toMap())

    fun addTemplateProperties(properties: Map<String, Any>) = templateProperties.putAll(properties)

    fun addFilesToOpen(vararg relativeCanonicalPaths: String) = addFilesToOpen(relativeCanonicalPaths.toList())

    fun addFilesToOpen(relativeCanonicalPaths: Iterable<String>) {
        relativeCanonicalPaths.mapTo(filesToOpen) { "$outputDirectory/$it" }
    }

    abstract fun setupAssets(project: Project)

    override fun setupProject(project: Project) {
        setupAssets(project)

        WriteAction.runAndWait<Throwable> {
            val generatedFiles = mutableSetOf<VirtualFile>()
            for (asset in assets) {
                generateFile(asset)?.let { generatedFiles += it }
            }

            runWhenCreated(project) {
                fixupFiles(project, generatedFiles)
            }
        }
    }

    protected fun runWhenCreated(project: Project, action: () -> Unit) {
        if (ApplicationManager.getApplication().isUnitTestMode) {
            action()
        } else {
            StartupManager.getInstance(project).runAfterOpened {
                ApplicationManager.getApplication().invokeLater(action, project.disposed)
            }
        }
    }

    private fun generateFile(asset: GeneratorAsset): VirtualFile? {
        return when (asset) {
            is GeneratorTemplateFile -> generateFile(asset)
            is GeneratorResourceFile -> generateFile(asset)
            is GeneratorEmptyDirectory -> generateFile(asset)
        }
    }

    private fun generateFile(asset: GeneratorTemplateFile): VirtualFile? {
        val code = try {
            asset.template.getText(templateProperties)
        } catch (e: Exception) {
            throw IOException("Unable to process template", e)
        }

        val pathStr = "$outputDirectory/${asset.targetFileName}"
        val path = Path.of(pathStr)
        path.parent?.let(NioFiles::createDirectories)
        Files.writeString(path, code)

        return VfsUtil.findFile(path, true)
    }

    private fun generateFile(asset: GeneratorResourceFile): VirtualFile? {
        val content = asset.resource.openStream().use { it.readAllBytes() }

        val pathStr = "$outputDirectory/${asset.targetFileName}"
        val path = Path.of(pathStr)
        path.parent?.let(NioFiles::createDirectories)
        Files.write(path, content)

        return VfsUtil.findFile(path, true)
    }

    private fun generateFile(asset: GeneratorEmptyDirectory): VirtualFile? {
        val pathStr = "$outputDirectory/${asset.targetFileName}"
        val path = Path.of(pathStr)
        NioFiles.createDirectories(path)
        return VfsUtil.findFile(path, true)
    }

    private fun fixupFiles(project: Project, generatedFiles: Iterable<VirtualFile>) {
        val psiManager = PsiManager.getInstance(project)
        val psiFiles = generatedFiles.mapNotNull { psiManager.findFile(it) }

        ReformatCodeProcessor(project, psiFiles.toTypedArray(), null, false).run()

        val fileEditorManager = FileEditorManager.getInstance(project)
        val projectView = ProjectView.getInstance(project)
        for (file in generatedFiles) {
            if (file.path in filesToOpen) {
                fileEditorManager.openFile(file, true)
                projectView.select(null, file, false)
            }
        }
    }
}