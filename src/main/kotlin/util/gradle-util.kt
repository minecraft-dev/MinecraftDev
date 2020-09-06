/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import java.nio.file.Path
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.util.childrenOfType

inline fun runGradleTask(
    project: Project,
    dir: Path,
    func: (ExternalSystemTaskExecutionSettings) -> Unit
) {
    val settings = ExternalSystemTaskExecutionSettings().apply {
        externalSystemIdString = GradleConstants.SYSTEM_ID.id
        externalProjectPath = dir.toString()
        func(this)
    }

    val lock = ReentrantLock()
    val condition = lock.newCondition()

    lock.withLock {
        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            GradleCallback(lock, condition),
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            false
        )

        condition.await()
    }
}

fun addGradleDependency(
    project: Project,
    module: Module,
    group: String,
    artifact: String,
    version: SemanticVersion
): Boolean {
    val buildGradleFile = module.rootManager.contentRoots.mapFirstNotNull { moduleFile ->
        moduleFile.findChild("build.gradle") ?: moduleFile.findChild("build.gradle.kts")
    } ?: ProjectRootManager.getInstance(project).contentRoots.mapFirstNotNull { projectFile ->
        projectFile.findChild("build.gradle") ?: projectFile.findChild("build.gradle.kts")
    } ?: return false
    val psiFile = PsiManager.getInstance(project).findFile(buildGradleFile) ?: return false
    val success = when (psiFile) {
        is GroovyFile -> addGroovyGradleDependency(project, psiFile, group, artifact, version)
        is KtFile -> addKotlinGradleDependency(project, psiFile, group, artifact, version)
        else -> false
    }
    if (success) {
        FileDocumentManager.getInstance().saveAllDocuments()
        ExternalSystemUtil.refreshProjects(
            ImportSpecBuilder(project, GradleConstants.SYSTEM_ID).use(ProgressExecutionMode.MODAL_SYNC)
        )
    }
    return success
}

private fun addGroovyGradleDependency(
    project: Project,
    file: GroovyFile,
    group: String,
    artifact: String,
    version: SemanticVersion
): Boolean {
    val dependenciesCall = file.childrenOfType<GrMethodCall>().firstOrNull { methodCall ->
        (methodCall.invokedExpression as? GrReferenceExpression)?.referenceName == "dependencies"
    } ?: return false
    val dependenciesClosure = dependenciesCall.closureArguments.firstOrNull() ?: return false
    val toInsert = GroovyPsiElementFactory.getInstance(project).createStatementFromText(
        "implementation '$group:$artifact:$version'",
        dependenciesClosure
    )
    WriteCommandAction.runWriteCommandAction(project) {
        dependenciesClosure.addStatementBefore(toInsert, null)
        CodeStyleManager.getInstance(project).reformat(dependenciesClosure)
    }
    return true
}

private fun addKotlinGradleDependency(
    project: Project,
    file: KtFile,
    group: String,
    artifact: String,
    version: SemanticVersion
): Boolean {
    val scriptBlock = file.script?.blockExpression ?: return false
    val dependenciesCall = scriptBlock.statements.asSequence()
        .mapNotNull { it as? KtScriptInitializer }
        .mapNotNull { it.body as? KtCallElement }
        .firstOrNull { (it.calleeExpression as? KtSimpleNameExpression)?.getReferencedName() == "dependencies" }
        ?: return false
    val dependenciesLambda = dependenciesCall.lambdaArguments.firstOrNull()?.getArgumentExpression()
        as? KtLambdaExpression ?: return false
    val dependenciesBlock = dependenciesLambda.functionLiteral.bodyBlockExpression ?: return false
    val factory = KtPsiFactory(project)
    val toInsert = factory.createExpression("implementation(\"$group:$artifact:$version\")")
    WriteCommandAction.runWriteCommandAction(project) {
        dependenciesBlock.addBefore(factory.createNewLine(), dependenciesBlock.rBrace)
        dependenciesBlock.addBefore(toInsert, dependenciesBlock.rBrace)
        CodeStyleManager.getInstance(project).reformat(dependenciesBlock)
    }
    return true
}

class GradleCallback(private val lock: ReentrantLock, private val condition: Condition) : TaskCallback {

    override fun onSuccess() = resume()
    override fun onFailure() = resume()

    private fun resume() {
        lock.withLock {
            condition.signalAll()
        }
    }
}
