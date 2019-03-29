/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager

class SpongeProjectConfiguration : ProjectConfiguration() {

    val dependencies = mutableListOf<String>()
    var spongeApiVersion = ""

    override var type = PlatformType.SPONGE

    init {
        type = PlatformType.SPONGE
    }

    private fun hasDependencies() = listContainsAtLeastOne(dependencies)
    fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator) {
        if (project.isDisposed) {
            return
        }

        val baseConfig = base ?: return
        val dirs = buildSystem.directories ?: return

        runWriteTask {
            indicator.text = "Writing main class"

            var file = dirs.sourceDirectory
            val files = baseConfig.mainClass.split("\\.".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            val className = files[files.size - 1]
            val packageName = baseConfig.mainClass.substring(0, baseConfig.mainClass.length - className.length - 1)
            file = getMainClassDirectory(files, file)

            val mainClassFile = file.findOrCreateChildData(this, "$className.java")
            SpongeTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className, hasDependencies())

            val mainClassPsi = PsiManager.getInstance(project).findFile(mainClassFile) as PsiJavaFile? ?: return@runWriteTask
            val psiClass = mainClassPsi.classes[0]

            writeMainSpongeClass(
                project,
                mainClassPsi,
                psiClass,
                buildSystem,
                baseConfig.pluginName,
                baseConfig.description ?: "",
                baseConfig.website ?: "",
                hasAuthors(),
                baseConfig.authors,
                hasDependencies(),
                dependencies
            )

            EditorHelper.openInEditor(mainClassPsi)
        }
    }
}

fun writeMainSpongeClass(
    project: Project,
    mainClassPsi: PsiJavaFile,
    psiClass: PsiClass,
    buildSystem: BuildSystem,
    pluginName: String,
    description: String,
    website: String,
    hasAuthors: Boolean,
    authors: List<String>,
    hasDependencies: Boolean,
    dependencies: List<String>
) {
    val annotationString = StringBuilder("@Plugin(")
    annotationString + "\nid = ${escape(buildSystem.artifactId.toLowerCase())}"
    annotationString + ",\nname = ${escape(pluginName)}"
    if (buildSystem !is GradleBuildSystem) {
        // SpongeGradle will automatically set the Gradle version as plugin version
        annotationString + ",\nversion = ${escape(buildSystem.version)}"
    }

    if (description.isNotEmpty()) {
        annotationString + ",\ndescription = ${escape(description)}"
    }

    if (website.isNotEmpty()) {
        annotationString + ",\nurl = ${escape(website)}"
    }

    if (hasAuthors) {
        annotationString + ",\nauthors = {\n${authors.joinToString(",\n", transform = ::escape)}\n}"
    }

    if (hasDependencies) {
        annotationString + ",\ndependencies = {\n${dependencies.joinToString(",\n") { "@Dependency(id = ${escape(it)})" }}\n}"
    }

    annotationString + "\n)"
    val factory = JavaPsiFacade.getElementFactory(project)
    val annotation = factory.createAnnotationFromText(annotationString.toString(), null)

    mainClassPsi.runWriteAction {
        psiClass.modifierList?.addBefore(annotation, psiClass.modifierList!!.firstChild)
        CodeStyleManager.getInstance(project).reformat(psiClass)
    }
}

private fun escape(text: String): String {
    return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

private operator fun StringBuilder.plus(text: String) = this.append(text)
