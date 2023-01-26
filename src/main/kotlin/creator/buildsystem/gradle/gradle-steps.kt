/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem.gradle

import com.demonwav.mcdev.creator.*
import com.demonwav.mcdev.creator.CreatorStep.Companion.writeText
import com.demonwav.mcdev.creator.buildsystem.*
import com.demonwav.mcdev.util.*
import com.demonwav.mcdev.util.MinecraftTemplates.Companion.GRADLE_WRAPPER_PROPERTIES
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.execution.RunManager
import com.intellij.ide.ui.UISettings
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.StatusBarEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.util.concurrent.CountDownLatch
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getCallNameExpression
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.service.project.open.canLinkAndRefreshGradleProject
import org.jetbrains.plugins.gradle.service.project.open.linkAndRefreshGradleProject
import org.jetbrains.plugins.groovy.GroovyLanguage
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner

val DEFAULT_GRADLE_VERSION = SemanticVersion.release(7, 3, 3)
val GRADLE_VERSION_KEY = Key.create<SemanticVersion>("mcdev.gradleVersion")

fun FixedAssetsNewProjectWizardStep.addGradleWrapperProperties(project: Project) {
    val gradleVersion = data.getUserData(GRADLE_VERSION_KEY) ?: DEFAULT_GRADLE_VERSION
    addTemplateProperties("GRADLE_WRAPPER_VERSION" to gradleVersion)
    addTemplates(project, "gradle/wrapper/gradle-wrapper.properties" to GRADLE_WRAPPER_PROPERTIES)
}

abstract class AbstractRunGradleTaskStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    abstract val task: String
    override val description get() = "Running Gradle task: '$task'"

    override fun perform(project: Project) {
        val outputDirectory = context.projectFileDirectory
        runGradleTaskAndWait(project, Path.of(outputDirectory)) { settings ->
            settings.taskNames = listOf(task)
        }
    }
}

class GradleWrapperStep(parent: NewProjectWizardStep) : AbstractRunGradleTaskStep(parent) {
    override val task = "wrapper"
}

abstract class AbstractPatchGradleFilesStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description = "Patching Gradle files"

    abstract fun patch(project: Project, gradleFiles: GradleFiles)

    protected fun addRepositories(project: Project, buildGradle: GradleFile?, repositories: List<BuildRepository>) {
        if (buildGradle == null || repositories.isEmpty()) {
            return
        }

        buildGradle.psi.runWriteAction {
            when (buildGradle) {
                is GroovyGradleFile -> {
                    val reposBlock = findOrCreateGroovyBlock(project, buildGradle.psi, "repositories")
                    val elementFactory = GroovyPsiElementFactory.getInstance(project)
                    for (repo in repositories) {
                        if (BuildSystemType.GRADLE !in repo.buildSystems) {
                            continue
                        }
                        val mavenBlock =
                            elementFactory.createStatementFromText("maven {\n}", reposBlock) as GrMethodCallExpression
                        val mavenClosure = mavenBlock.closureArguments[0]
                        if (repo.id.isNotBlank()) {
                            val idStatement =
                                elementFactory.createStatementFromText("name = ${makeStringLiteral(repo.id)}")
                            mavenClosure.addStatementBefore(idStatement, null)
                        }
                        val urlStatement =
                            elementFactory.createStatementFromText("url = ${makeStringLiteral(repo.url)}")
                        mavenClosure.addStatementBefore(urlStatement, null)
                        reposBlock.addStatementBefore(mavenBlock, null)
                    }
                }

                is KotlinGradleFile -> {
                    val script = buildGradle.psi.script?.blockExpression ?: return@runWriteAction
                    val reposBlock = findOrCreateKotlinBlock(project, script, "repositories")
                    val elementFactory = KtPsiFactory(project)
                    for (repo in repositories) {
                        if (BuildSystemType.GRADLE !in repo.buildSystems) {
                            continue
                        }
                        val mavenBlock = elementFactory.createExpression("maven {\n}") as KtCallExpression
                        val mavenLambda = mavenBlock.lambdaArguments[0].getLambdaExpression()!!.bodyExpression!!
                        if (repo.id.isNotBlank()) {
                            val idStatement = elementFactory.createAssignment("name = ${makeStringLiteral(repo.id)}")
                            mavenLambda.addBefore(idStatement, mavenLambda.rBrace)
                        }
                        val urlStatement = elementFactory.createAssignment("url = uri(${makeStringLiteral(repo.url)})")
                        mavenLambda.addBefore(urlStatement, mavenLambda.rBrace)
                        reposBlock.addBefore(mavenBlock, reposBlock.rBrace)
                    }
                }
            }
        }
    }

    protected fun addDependencies(project: Project, buildGradle: GradleFile?, dependencies: List<BuildDependency>) {
        if (buildGradle == null || dependencies.isEmpty()) {
            return
        }

        buildGradle.psi.runWriteAction {
            when (buildGradle) {
                is GroovyGradleFile -> {
                    val depsBlock = findOrCreateGroovyBlock(project, buildGradle.psi, "dependencies")
                    val elementFactory = GroovyPsiElementFactory.getInstance(project)
                    for (dep in dependencies) {
                        val gradleConfig = dep.gradleConfiguration ?: continue
                        val stmt = elementFactory.createStatementFromText(
                            "$gradleConfig \"${escapeGString(dep.groupId)}:${
                                escapeGString(dep.artifactId)
                            }:${escapeGString(dep.version)}\"", depsBlock
                        )
                        depsBlock.addStatementBefore(stmt, null)
                    }
                }

                is KotlinGradleFile -> {
                    val script = buildGradle.psi.script?.blockExpression ?: return@runWriteAction
                    val depsBlock = findOrCreateKotlinBlock(project, script, "dependencies")
                    val elementFactory = KtPsiFactory(project)
                    for (dep in dependencies) {
                        val gradleConfig = dep.gradleConfiguration ?: continue
                        val stmt = elementFactory.createExpression(
                            "$gradleConfig(\"${escapeGString(dep.groupId)}:${
                                escapeGString(dep.artifactId)
                            }:${escapeGString(dep.version)}\")"
                        )
                        depsBlock.addBefore(stmt, depsBlock.rBrace)
                    }
                }
            }
        }
    }

    protected fun addPlugins(project: Project, buildGradle: GradleFile?, plugins: List<GradlePlugin>) {
        if (buildGradle == null || plugins.isEmpty()) {
            return
        }

        buildGradle.psi.runWriteAction {
            fun makePluginStatement(plugin: GradlePlugin, kotlin: Boolean): String {
                return buildString {
                    if (kotlin) {
                        append("id(${makeStringLiteral(plugin.id)})")
                    } else {
                        append("id ${makeStringLiteral(plugin.id)}")
                    }
                    plugin.version?.let { append(" version ${makeStringLiteral(it)}") }
                    if (!plugin.apply) {
                        append(" apply false")
                    }
                }
            }

            when (buildGradle) {
                is GroovyGradleFile -> {
                    val pluginsBlock = findOrCreateGroovyBlock(project, buildGradle.psi, "plugins", first = true)
                    val elementFactory = GroovyPsiElementFactory.getInstance(project)
                    for (plugin in plugins) {
                        val stmt = elementFactory.createStatementFromText(makePluginStatement(plugin, false))
                        pluginsBlock.addStatementBefore(stmt, null)
                    }
                }
                is KotlinGradleFile -> {
                    val script = buildGradle.psi.script?.blockExpression ?: return@runWriteAction
                    val pluginsBlock = findOrCreateKotlinBlock(project, script, "plugins", first = true)
                    val elementFactory = KtPsiFactory(project)
                    for (plugin in plugins) {
                        val stmt = elementFactory.createExpression(makePluginStatement(plugin, true))
                        pluginsBlock.addBefore(stmt, pluginsBlock.rBrace)
                    }
                }
            }
        }
    }

    protected fun makeStringLiteral(str: String): String {
        return "\"${escapeGString(str)}\""
    }

    private fun escapeGString(str: String): String {
        return StringUtil.escapeStringCharacters(str.length, str, "\"\$", StringBuilder()).toString()
    }

    protected fun findGroovyBlock(element: GrStatementOwner, name: String): GrClosableBlock? {
        return element.statements
            .mapFirstNotNull { call ->
                if (call is GrMethodCallExpression && call.callReference?.methodName == name) {
                    call.closureArguments.firstOrNull()
                } else {
                    null
                }
            }
    }

    protected fun findOrCreateGroovyBlock(project: Project, element: GrStatementOwner, name: String, first: Boolean = false): GrClosableBlock {
        findGroovyBlock(element, name)?.let { return it }
        val block = GroovyPsiElementFactory.getInstance(project).createStatementFromText("$name {\n}", element)
        val anchor = if (first) {
            element.statements.firstOrNull()
        } else {
            null
        }
        return (element.addStatementBefore(block, anchor) as GrMethodCallExpression).closureArguments.first()
    }

    protected fun findKotlinBlock(element: KtBlockExpression, name: String): KtBlockExpression? {
        return element.childrenOfType<KtScriptInitializer>()
            .flatMap { it.childrenOfType<KtCallExpression>() }
            .mapFirstNotNull { call ->
                if ((call.calleeExpression as? KtNameReferenceExpression)?.getReferencedName() == name) {
                    call.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression
                } else {
                    null
                }
            }
    }

    protected fun findOrCreateKotlinBlock(project: Project, element: KtBlockExpression, name: String, first: Boolean = false): KtBlockExpression {
        findKotlinBlock(element, name)?.let { return it }
        val block = KtPsiFactory(project).createExpression("$name {\n}")
        val addedBlock = if (first) {
            element.addAfter(block, element.lBrace)
        } else {
            element.addBefore(block, element.rBrace)
        }
        return (addedBlock as KtCallExpression).lambdaArguments.first().getLambdaExpression()!!.bodyExpression!!
    }

    protected fun KtPsiFactory.createAssignment(text: String): KtBinaryExpression {
        return this.createBlock(text).firstStatement as KtBinaryExpression
    }

    override fun perform(project: Project) {
        invokeAndWait {
            if (project.isDisposed) {
                return@invokeAndWait
            }

            runWriteTask {
                val rootDir = VfsUtil.findFile(Path.of(context.projectFileDirectory), true)
                    ?: return@runWriteTask
                val gradleFiles = GradleFiles(project, rootDir)
                NonProjectFileWritingAccessProvider.disableChecksDuring {
                    patch(project, gradleFiles)
                    gradleFiles.commit()
                }
            }
        }
    }

    class GradleFiles(
        private val project: Project,
        private val rootDir: VirtualFile
    ) {
        private val lazyBuildGradle = lazy {
            val file = rootDir.findChild("build.gradle") ?: rootDir.findChild("build.gradle.kts")
                ?: return@lazy null
            makeGradleFile(file)
        }
        private val lazySettingsGradle = lazy {
            val file = rootDir.findChild("settings.gradle") ?: rootDir.findChild("settings.gradle.kts")
                ?: return@lazy null
            makeGradleFile(file)
        }
        private val lazyGradleProperties = lazy {
            val file = rootDir.findChild("gradle.properties") ?: return@lazy null
            PsiManager.getInstance(project).findFile(file) as? PropertiesFile
        }

        val buildGradle by lazyBuildGradle
        val settingsGradle by lazySettingsGradle
        val gradleProperties by lazyGradleProperties

        private fun makeGradleFile(virtualFile: VirtualFile): GradleFile? {
            val psi = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
            return when (psi) {
                is GroovyFile -> GroovyGradleFile(psi)
                is KtFile -> KotlinGradleFile(psi)
                else -> null
            }
        }

        fun commit() {
            val files = mutableListOf<PsiFile>()
            if (lazyBuildGradle.isInitialized()) {
                buildGradle?.psi?.let { files += it }
            }
            if (lazySettingsGradle.isInitialized()) {
                settingsGradle?.psi?.let { files += it }
            }
            if (lazyGradleProperties.isInitialized()) {
                (gradleProperties as? PsiFile)?.let { files += it }
            }

            val psiDocumentManager = PsiDocumentManager.getInstance(project)
            val fileDocumentManager = FileDocumentManager.getInstance()
            for (file in files) {
                val document = psiDocumentManager.getDocument(file) ?: continue
                fileDocumentManager.saveDocument(document)
            }
        }
    }

    sealed class GradleFile {
        abstract val psi: PsiFile
    }
    class GroovyGradleFile(override val psi: GroovyFile) : GradleFile()
    class KotlinGradleFile(override val psi: KtFile) : GradleFile()
}

open class GradleImportStep(parent: NewProjectWizardStep) : AbstractLongRunningStep(parent) {
    override val description = "Importing Gradle project"

    open val additionalRunTasks = emptyList<String>()

    override fun perform(project: Project) {
        val rootDirectory = Path.of(context.projectFileDirectory)
        val buildSystemProps = findStep<BuildSystemPropertiesStep<*>>()

        // Tell IntelliJ to import this project
        rootDirectory.virtualFileOrError.refresh(false, true)

        val latch = CountDownLatch(1)

        invokeLater(project.disposed) {
            val path = rootDirectory.toAbsolutePath().toString()
            if (canLinkAndRefreshGradleProject(path, project, false)) {
                linkAndRefreshGradleProject(path, project)
                showProgress(project)
            }

            StartupManager.getInstance(project).runAfterOpened {
                latch.countDown()
            }
        }

        // Set up the run config
        // Get the gradle external task type, this is what sets it as a gradle task
        addRunTaskConfiguration(project, rootDirectory, buildSystemProps, "build")
        for (tasks in additionalRunTasks) {
            addRunTaskConfiguration(project, rootDirectory, buildSystemProps, tasks)
        }

        if (!ApplicationManager.getApplication().isDispatchThread) {
            latch.await()
        }
    }

    private fun addRunTaskConfiguration(project: Project, rootDirectory: Path, buildSystemProps: BuildSystemPropertiesStep<*>, task: String) {
        val gradleType = GradleExternalTaskConfigurationType.getInstance()

        val runManager = RunManager.getInstance(project)
        val runConfigName = buildSystemProps.artifactId + ' ' + task

        val runConfiguration = GradleRunConfiguration(project, gradleType.factory, runConfigName)

        // Set relevant gradle values
        runConfiguration.settings.externalProjectPath = rootDirectory.toAbsolutePath().toString()
        runConfiguration.settings.executionName = runConfigName
        runConfiguration.settings.taskNames = listOf(task)

        runConfiguration.isAllowRunningInParallel = false

        val settings = runManager.createConfiguration(
            runConfiguration,
            gradleType.factory
        )

        settings.isActivateToolWindowBeforeRun = true
        settings.storeInLocalWorkspace()

        runManager.addConfiguration(settings)
        if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = settings
        }
    }
}

class ReformatBuildGradleStep(parent: NewProjectWizardStep) : AbstractReformatFilesStep(parent) {
    override fun addFilesToReformat() {
        addFileToReformat("build.gradle")
        addFileToReformat("build.gradle.kts")
    }
}

class SimpleGradleSetupStep(
    private val project: Project,
    private val rootDirectory: Path,
    private val buildSystem: BuildSystem,
    private val gradleFiles: GradleFiles<String>,
    private val kotlinScript: Boolean = false
) : CreatorStep {

    override fun runStep(indicator: ProgressIndicator) {
        runWriteTask {
            if (project.isDisposed) {
                return@runWriteTask
            }

            buildSystem.directories =
                DirectorySet.create(rootDirectory)
            val (buildGradle, gradleProp, settingsGradle) = setupGradleFiles(
                rootDirectory,
                gradleFiles,
                kotlinScript
            )

            val psiManager = PsiManager.getInstance(project)
            writeText(
                buildGradle,
                gradleFiles.buildGradle,
                psiManager
            )
            if (gradleProp != null && gradleFiles.gradleProperties != null) {
                writeText(
                    gradleProp,
                    gradleFiles.gradleProperties,
                    psiManager
                )
            }
            if (settingsGradle != null && gradleFiles.settingsGradle != null) {
                writeText(
                    settingsGradle,
                    gradleFiles.settingsGradle,
                    psiManager
                )
            }
        }
    }
}

class GradleSetupStep(
    private val project: Project,
    private val rootDirectory: Path,
    private val buildSystem: BuildSystem,
    private val gradleFiles: GradleFiles<String>,
    private val kotlinScript: Boolean = false
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val (_, gradleProp, settingsGradle) = setupGradleFiles(rootDirectory, gradleFiles, kotlinScript)

        runWriteTask {
            if (project.isDisposed) {
                return@runWriteTask
            }

            val buildGradlePsi = addBuildGradleDependencies(project, buildSystem, gradleFiles.buildGradle, kotlinScript)
            val psiManager = PsiManager.getInstance(project)
            psiManager.findDirectory(rootDirectory.virtualFileOrError)?.let { dir ->
                dir.findFile(buildGradlePsi.name)?.delete()
                val newFile = dir.add(buildGradlePsi) as? PsiFile ?: return@let
                ReformatCodeProcessor(newFile, false).run()
            }

            if (gradleProp != null && gradleFiles.gradleProperties != null) {
                writeText(gradleProp, gradleFiles.gradleProperties, psiManager)
            }
            if (settingsGradle != null && gradleFiles.settingsGradle != null) {
                writeText(settingsGradle, gradleFiles.settingsGradle, psiManager)
            }
        }
    }
}

data class GradleFiles<out T>(
    val buildGradle: T,
    val gradleProperties: T?,
    val settingsGradle: T?
)

fun setupGradleFiles(dir: Path, givenFiles: GradleFiles<String>, kotlinScript: Boolean = false): GradleFiles<Path> {
    return GradleFiles(
        dir.resolve(if (kotlinScript) "build.gradle.kts" else "build.gradle"),
        givenFiles.gradleProperties?.let { dir.resolve("gradle.properties") },
        givenFiles.settingsGradle?.let { dir.resolve(if (kotlinScript) "settings.gradle.kts" else "settings.gradle") },
    ).apply {
        Files.deleteIfExists(buildGradle)
        Files.createFile(buildGradle)
        gradleProperties?.let { Files.deleteIfExists(it); Files.createFile(it) }
        settingsGradle?.let { Files.deleteIfExists(it); Files.createFile(it) }
    }
}

fun addBuildGradleDependencies(
    project: Project,
    buildSystem: BuildSystem,
    text: String,
    kotlinScript: Boolean = false
): PsiFile {
    val file = PsiFileFactory.getInstance(project).createFileFromText(GroovyLanguage, text)
    return file.runWriteAction {
        val fileName = if (kotlinScript) "build.gradle.kts" else "build.gradle"
        file.name = fileName

        val groovyFile = file as GroovyFile

        buildSystem.repositories.asSequence()
            .filter { it.buildSystems.contains(BuildSystemType.GRADLE) }
            .map { "maven {name = '${it.id}'\nurl = '${it.url}'\n}" }
            .toList()
            .ifNotEmpty { reps -> appendExpressions(project, groovyFile, "repositories", reps) }

        buildSystem.dependencies.asSequence()
            .filter { it.gradleConfiguration != null }
            .map { "${it.gradleConfiguration} '${it.groupId}:${it.artifactId}:${it.version}'" }
            .toList()
            .ifNotEmpty { deps -> appendExpressions(project, groovyFile, "dependencies", deps) }

        return@runWriteAction file
    }
}

class AddGradlePluginStep(
    private val project: Project,
    private val rootDirectory: Path,
    private val plugins: Collection<GradlePlugin>,
    private val kotlinScript: Boolean = false
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val fileName = if (kotlinScript) "build.gradle.kts" else "build.gradle"
        val virtualFile = rootDirectory.resolve(fileName).virtualFileOrError
        runWriteTask {
            if (project.isDisposed) {
                return@runWriteTask
            }

            val file = PsiManager.getInstance(project).findFile(virtualFile)
                ?: throw IllegalStateException("Could not find $fileName")
            file.runWriteAction {
                if (project.isDisposed) {
                    return@runWriteAction
                }

                val groovyFile = file as GroovyFile

                plugins.asSequence()
                    .map { plugin ->
                        buildString {
                            append("id \"${plugin.id}\"")
                            plugin.version?.let { append(" version \"$it\"") }
                            if (!plugin.apply) {
                                append(" apply false")
                            }
                        }
                    }
                    .toList()
                    .ifNotEmpty { plugins -> appendExpressions(project, groovyFile, "plugins", plugins) }

                ReformatCodeProcessor(file, false).run()
            }
        }
    }
}

private fun appendExpressions(
    project: Project,
    file: GroovyFile,
    name: String,
    expressions: Iterable<String>
) {
    // Get the block so we can start working with it
    val block = getClosableBlockByName(file, name)
        ?: throw IllegalStateException("Failed to parse build.gradle files")

    // Create a super expression with all the expressions tied together
    val expressionText = expressions.joinToString("\n")

    // We can't create each expression and add them to the file...that won't work. Groovy requires a new line
    // from one method call expression to another, and there's no way to (easily) put whitespace in Psi because Psi is
    // stupid. So instead we make the whole thing as one big clump and insert it into the block.
    val fakeFile = GroovyPsiElementFactory.getInstance(project).createGroovyFile(expressionText, false, null)
    val last = block.children.last()
    block.addBefore(fakeFile, last)
}

private fun getClosableBlockByName(element: PsiElement, name: String) =
    element.children.asSequence()
        .filter {
            // We want to find the child which has a GrReferenceExpression with the right name
            it.children.any { child -> child is GrReferenceExpression && child.text == name }
        }.map {
            // We want to find the grandchild which is a GrClosable block
            it.children.mapNotNull { child -> child as? GrClosableBlock }.firstOrNull()
        }.filterNotNull()
        .firstOrNull()

class BasicGradleFinalizerStep(
    private val module: Module,
    private val rootDirectory: Path,
    private val buildSystem: BuildSystem,
    private vararg val additionalRunTasks: String
) : CreatorStep {
    private val project
        get() = module.project

    override fun runStep(indicator: ProgressIndicator) {
        // Tell IntelliJ to import this project
        rootDirectory.virtualFileOrError.refresh(false, true)

        invokeLater(module.disposed) {
            val path = rootDirectory.toAbsolutePath().toString()
            if (canLinkAndRefreshGradleProject(path, project, false)) {
                linkAndRefreshGradleProject(path, project)
                showProgress(project)
            }
        }

        // Set up the run config
        // Get the gradle external task type, this is what sets it as a gradle task
        addRunTaskConfiguration("build")
        for (tasks in additionalRunTasks) {
            addRunTaskConfiguration(tasks)
        }
    }

    private fun addRunTaskConfiguration(task: String) {
        val gradleType = GradleExternalTaskConfigurationType.getInstance()

        val runManager = RunManager.getInstance(project)
        val runConfigName = buildSystem.artifactId + ' ' + task

        val runConfiguration = GradleRunConfiguration(project, gradleType.factory, runConfigName)

        // Set relevant gradle values
        runConfiguration.settings.externalProjectPath = rootDirectory.toAbsolutePath().toString()
        runConfiguration.settings.executionName = runConfigName
        runConfiguration.settings.taskNames = listOf(task)

        runConfiguration.isAllowRunningInParallel = false

        val settings = runManager.createConfiguration(
            runConfiguration,
            gradleType.factory
        )

        settings.isActivateToolWindowBeforeRun = true
        settings.storeInLocalWorkspace()

        runManager.addConfiguration(settings)
        if (runManager.selectedConfiguration == null) {
            runManager.selectedConfiguration = settings
        }
    }
}

class GradleWrapperStepOld(
    private val project: Project,
    private val rootDirectory: Path,
    private val buildSystem: GradleBuildSystem
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val wrapperVersion = buildSystem.gradleVersion

        // Setup gradle wrapper
        // We'll write the properties file to ensure it sets up with the right version
        val wrapperDir = rootDirectory.resolve("gradle/wrapper")
        Files.createDirectories(wrapperDir)
        val wrapperProp = wrapperDir.resolve("gradle-wrapper.properties")

        val text = "distributionUrl=https\\://services.gradle.org/distributions/gradle-$wrapperVersion-bin.zip\n"

        Files.write(wrapperProp, text.toByteArray(Charsets.UTF_8), CREATE, WRITE, TRUNCATE_EXISTING)

        indicator.text = "Setting up Gradle Wrapper"
        indicator.text2 = "Running Gradle task: 'wrapper'"
        runGradleTaskAndWait(project, rootDirectory) { settings ->
            settings.taskNames = listOf("wrapper")
        }
        indicator.text2 = null
    }
}

// Show the background processes window for setup tasks
private fun showProgress(project: Project) {
    if (!UISettings.getInstance().showStatusBar || UISettings.getInstance().presentationMode) {
        return
    }

    val statusBar = WindowManager.getInstance().getStatusBar(project) as? StatusBarEx ?: return
    statusBar.isProcessWindowOpen = true
}

class GradleGitignoreStep(
    private val project: Project,
    private val rootDirectory: Path
) : CreatorStep {
    override fun runStep(indicator: ProgressIndicator) {
        val gitignoreFile = rootDirectory.resolve(".gitignore")

        val fileText = BuildSystemTemplate.applyGradleGitignore(project)

        Files.write(gitignoreFile, fileText.toByteArray(Charsets.UTF_8), CREATE, WRITE, TRUNCATE_EXISTING)
    }
}
