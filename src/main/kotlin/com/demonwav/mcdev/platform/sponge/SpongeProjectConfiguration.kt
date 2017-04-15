/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.ProjectConfiguration
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager

class SpongeProjectConfiguration : ProjectConfiguration() {

    val dependencies = mutableListOf<String>()
    var generateDocumentedListeners = false
    var spongeApiVersion = ""

    init {
        type = PlatformType.SPONGE
    }

    fun hasDependencies() = listContainsAtLeastOne(dependencies)
    fun setDependencies(string: String) {
        dependencies.clear()
        dependencies.addAll(commaSplit(string))
    }

    override fun create(project: Project, buildSystem: BuildSystem, indicator: ProgressIndicator) {
        runWriteTask {
            indicator.text = "Writing main class"
            var file = buildSystem.sourceDirectory
            val files = this.mainClass.split("\\.".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
            val className = files[files.size - 1]
            val packageName = this.mainClass.substring(0, this.mainClass.length - className.length - 1)
            file = getMainClassDirectory(files, file)

            val mainClassFile = file.findOrCreateChildData(this, className + ".java")
            SpongeTemplate.applyMainClassTemplate(project, mainClassFile, packageName, className, hasDependencies(), generateDocumentedListeners)

            val mainClassPsi = PsiManager.getInstance(project).findFile(mainClassFile) as PsiJavaFile? ?: return@runWriteTask
            val psiClass = mainClassPsi.classes[0]

            writeMainSpongeClass(
                project,
                mainClassPsi,
                psiClass,
                buildSystem,
                pluginName,
                description,
                website ?: "",
                hasAuthors(),
                authors,
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

    if (!description.isNullOrEmpty()) {
        annotationString + ",\ndescription = ${escape(description)}"
    }

    if (!website.isNullOrEmpty()) {
        annotationString + ",\nurl = ${escape(website)}"
    }

    if (hasAuthors) {
        annotationString + ",\nauthors = {\n${authors.map(::escape).joinToString(",\n")}\n}"
    }

    if (hasDependencies) {
        annotationString + ",\ndependencies = {\n${dependencies.map { "@Dependency(id = ${escape(it)})" }.joinToString(",\n")}\n}"
    }

    annotationString + "\n)"
    val factory = JavaPsiFacade.getElementFactory(project)
    val annotation = factory.createAnnotationFromText(annotationString.toString(), null)

    object : WriteCommandAction.Simple<Any>(project, mainClassPsi) {
        override fun run() {
            psiClass.modifierList?.addBefore(annotation, psiClass.modifierList!!.firstChild)
            CodeStyleManager.getInstance(project).reformat(psiClass)
        }
    }.execute()
}

private fun escape(text: String): String {
    return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

private operator fun StringBuilder.plus(text: String) = this.append(text)
