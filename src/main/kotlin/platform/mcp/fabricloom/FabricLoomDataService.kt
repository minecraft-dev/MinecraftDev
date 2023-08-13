/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.fabric.EntryPoint
import com.demonwav.mcdev.platform.fabric.creator.MAGIC_DEFERRED_INIT_FILE
import com.demonwav.mcdev.util.addImplements
import com.demonwav.mcdev.util.addMethod
import com.demonwav.mcdev.util.ifNotBlank
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.runWriteAction
import com.demonwav.mcdev.util.runWriteTaskInSmartMode
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.codeInsight.generation.OverrideImplementUtil
import com.intellij.ide.util.EditorHelper
import com.intellij.json.psi.JsonArray
import com.intellij.json.psi.JsonElementGenerator
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.IncorrectOperationException
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines
import org.jetbrains.plugins.gradle.util.GradleConstants

class FabricLoomDataService : AbstractProjectDataService<FabricLoomData, Module>() {

    override fun getTargetDataKey(): Key<FabricLoomData> = FabricLoomData.KEY

    override fun importData(
        toImport: Collection<DataNode<FabricLoomData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider,
    ) {
        // Dummy service to enable platform-side DataNodes cache
    }

    override fun postProcess(
        toImport: MutableCollection<out DataNode<FabricLoomData>>,
        projectData: ProjectData?,
        project: Project,
        modelsProvider: IdeModifiableModelsProvider
    ) {
        if (projectData == null || toImport.isEmpty() || projectData.owner != GradleConstants.SYSTEM_ID) {
            return
        }

        deferredProjectInit(project)
    }

    private fun deferredProjectInit(project: Project) {
        val baseDir = project.guessProjectDir()?.toNioPath()
            ?: return
        val deferredInitFile = baseDir / ".gradle" / MAGIC_DEFERRED_INIT_FILE
        if (!deferredInitFile.isRegularFile()) {
            return
        }

        val lines = deferredInitFile.readLines()
        if (lines.size < 4) {
            return
        }

        val (rawAuthors, website, repo, rawEntrypoints) = lines

        val authors = rawAuthors.ifNotBlank { it.split(',') } ?: emptyList()

        val entrypoints = rawEntrypoints.split(';').map { rawEntrypoint ->
            val (category, type, clazzName, interfaceName) = rawEntrypoint.split(',')
            EntryPoint(category, EntryPoint.Type.valueOf(type), clazzName, interfaceName)
        }

        project.runWriteTaskInSmartMode {
            fixupFabricModJson(project, baseDir, authors, website, repo, entrypoints)
            createEntryPoints(project, entrypoints)
            deferredInitFile.deleteIfExists()
        }
    }

    private fun fixupFabricModJson(
        project: Project,
        baseDir: Path,
        authors: List<String>,
        website: String,
        repo: String,
        entryPoints: List<EntryPoint>
    ) {
        val fabricModJsonFile =
            VfsUtil.findFile(baseDir.resolve(Path.of("src", "main", "resources", "fabric.mod.json")), true)
                ?: return
        val jsonFile = PsiManager.getInstance(project).findFile(fabricModJsonFile) as? JsonFile ?: return
        val json = jsonFile.topLevelValue as? JsonObject ?: return
        val generator = JsonElementGenerator(project)

        NonProjectFileWritingAccessProvider.allowWriting(listOf(fabricModJsonFile))
        jsonFile.runWriteAction {
            (json.findProperty("authors")?.value as? JsonArray)?.let { authorsArray ->
                for (i in authors.indices) {
                    if (i != 0) {
                        authorsArray.addBefore(generator.createComma(), authorsArray.lastChild)
                    }
                    authorsArray.addBefore(generator.createStringLiteral(authors[i]), authorsArray.lastChild)
                }
            }

            (json.findProperty("contact")?.value as? JsonObject)?.let { contactObject ->
                val properties = mutableListOf<Pair<String, String>>()
                if (!website.isNullOrBlank()) {
                    properties += "website" to website
                }
                if (!repo.isNullOrBlank()) {
                    properties += "repo" to repo
                }
                for (i in properties.indices) {
                    if (i != 0) {
                        contactObject.addBefore(generator.createComma(), contactObject.lastChild)
                    }
                    val key = StringUtil.escapeStringCharacters(properties[i].first)
                    val value = "\"" + StringUtil.escapeStringCharacters(properties[i].second) + "\""
                    contactObject.addBefore(generator.createProperty(key, value), contactObject.lastChild)
                }
            }

            (json.findProperty("entrypoints")?.value as? JsonObject)?.let { entryPointsObject ->
                val entryPointsByCategory = entryPoints
                    .groupBy { it.category }
                    .asSequence()
                    .sortedBy { it.key }
                    .toList()
                for (i in entryPointsByCategory.indices) {
                    val entryPointCategory = entryPointsByCategory[i]
                    if (i != 0) {
                        entryPointsObject.addBefore(generator.createComma(), entryPointsObject.lastChild)
                    }
                    val values = generator.createValue<JsonArray>("[]")
                    for (j in entryPointCategory.value.indices) {
                        if (j != 0) {
                            values.addBefore(generator.createComma(), values.lastChild)
                        }
                        val entryPointReference = entryPointCategory.value[j].computeReference(project)
                        val value = generator.createStringLiteral(entryPointReference)
                        values.addBefore(value, values.lastChild)
                    }
                    val key = StringUtil.escapeStringCharacters(entryPointCategory.key)
                    val prop = generator.createProperty(key, "[]")
                    prop.value?.replace(values)
                    entryPointsObject.addBefore(prop, entryPointsObject.lastChild)
                }
            }

            ReformatCodeProcessor(project, jsonFile, null, false).run()
        }
    }

    private fun createEntryPoints(project: Project, entryPoints: List<EntryPoint>) {
        val root = project.guessProjectDir() ?: return
        val psiManager = PsiManager.getInstance(project)

        val generatedClasses = mutableSetOf<PsiClass>()

        for (entryPoint in entryPoints) {
            // find the class, and create it if it doesn't exist
            val clazz = JavaPsiFacade.getInstance(project).findClass(
                entryPoint.className,
                GlobalSearchScope.projectScope(project),
            ) ?: run {
                val packageName = entryPoint.className.substringBeforeLast('.', missingDelimiterValue = "")
                val className = entryPoint.className.substringAfterLast('.')

                val dir = VfsUtil.createDirectoryIfMissing(root, "src/main/java/${packageName.replace('.', '/')}")
                val psiDir = psiManager.findDirectory(dir) ?: return@run null
                try {
                    JavaDirectoryService.getInstance().createClass(psiDir, className)
                } catch (e: IncorrectOperationException) {
                    invokeLater {
                        val message = MCDevBundle.message(
                            "intention.error.cannot.create.class.message",
                            className,
                            e.localizedMessage,
                        )
                        Messages.showErrorDialog(
                            project,
                            message,
                            MCDevBundle.message("intention.error.cannot.create.class.title"),
                        )
                    }
                    return
                }
            } ?: continue

            clazz.containingFile.runWriteAction {
                clazz.addImplements(entryPoint.interfaceName)

                val methodsToImplement = OverrideImplementUtil.getMethodsToOverrideImplement(clazz, true)
                val methods = OverrideImplementUtil.overrideOrImplementMethodCandidates(clazz, methodsToImplement, true)
                for (method in methods) {
                    clazz.addMethod(method)
                }
            }

            generatedClasses += clazz
        }

        for (clazz in generatedClasses) {
            ReformatCodeProcessor(project, clazz.containingFile, null, false).run()
            EditorHelper.openInEditor(clazz)
        }
    }
}
