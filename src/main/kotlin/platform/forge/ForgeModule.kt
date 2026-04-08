/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.platform.forge

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.insight.generation.EventListenerGenerationSupport
import com.demonwav.mcdev.inspection.IsCancelled
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.forge.inspections.sideonly.SidedProxyAnnotator
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.demonwav.mcdev.toml.stringValue
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.nullable
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.demonwav.mcdev.util.runWriteTaskLater
import com.demonwav.mcdev.util.waitForAllSmart
import com.intellij.json.JsonFileType
import com.intellij.lang.jvm.JvmModifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.toUElementOfType
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlFile
import org.toml.lang.psi.ext.name

class ForgeModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet) {

    var mcmod by nullable { facet.findFile(ForgeConstants.MCMOD_INFO, SourceType.RESOURCE) }
        private set
    var modsToml by nullable { facet.findFile(ForgeConstants.MODS_TOML_PATH, SourceType.RESOURCE) }
        private set

    override val moduleType = ForgeModuleType
    override val type = PlatformType.FORGE
    override val icon = PlatformAssets.FORGE_ICON

    override fun computeModIds() = getModsFromModsToml(project, modsToml)

    override val eventListenerGenSupport: EventListenerGenerationSupport = ForgeEventListenerGenerationSupport()

    override fun init() {
        ApplicationManager.getApplication().executeOnPooledThread {
            waitForAllSmart()
            // Set mcmod.info icon
            runWriteTaskLater {
                FileTypeManager.getInstance().associatePattern(JsonFileType.INSTANCE, ForgeConstants.MCMOD_INFO)
                FileTypeManager.getInstance().associatePattern(JsonFileType.INSTANCE, ForgeConstants.PACK_MCMETA)
            }

            if (project.isDisposed) {
                return@executeOnPooledThread
            }

            // Index @SideOnly
            ReadAction.nonBlocking(
                Callable {
                    val scope = GlobalSearchScope.projectScope(project)
                    val sidedProxy = JavaPsiFacade.getInstance(project)
                        .findClass(ForgeConstants.SIDED_PROXY_ANNOTATION, scope) ?: return@Callable
                    val annotatedFields = AnnotatedElementsSearch.searchPsiFields(sidedProxy, scope).findAll()

                    for (field in annotatedFields) {
                        SidedProxyAnnotator.Util.check(field)
                    }
                }
            ).inSmartMode(project).submit(AppExecutorUtil.getAppExecutorService())
        }
    }

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?): Boolean {
        if (method == null) {
            return ForgeConstants.FML_EVENT == eventClass.qualifiedName ||
                ForgeConstants.EVENT == eventClass.qualifiedName ||
                ForgeConstants.EVENTBUS_EVENT == eventClass.qualifiedName
        }

        var annotation = method.modifierList.findAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION)
        if (annotation != null) {
            return ForgeConstants.FML_EVENT == eventClass.qualifiedName
        }

        annotation = method.modifierList.findAnnotation(ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION)
        if (annotation != null || method.hasAnnotation(ForgeConstants.EVENTBUS_SUBSCRIBE_EVENT_ANNOTATION)) {
            return ForgeConstants.EVENT == eventClass.qualifiedName ||
                ForgeConstants.EVENTBUS_EVENT == eventClass.qualifiedName
        }

        // just default to true
        return true
    }

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod): String {
        val mcVersion = McpModuleSettings.getInstance(module).state.minecraftVersion
            ?.let { SemanticVersion.parse(it) }
        if (mcVersion != null && mcVersion >= ForgeModuleType.FG3_MC_VERSION) {
            return formatWrongEventMessage(
                ForgeConstants.EVENTBUS_EVENT,
                ForgeConstants.EVENTBUS_SUBSCRIBE_EVENT_ANNOTATION,
                ForgeConstants.EVENTBUS_EVENT == eventClass.qualifiedName,
            )
        }

        val annotation = method.modifierList.findAnnotation(ForgeConstants.EVENT_HANDLER_ANNOTATION)

        if (annotation != null) {
            return formatWrongEventMessage(
                ForgeConstants.FML_EVENT,
                ForgeConstants.SUBSCRIBE_EVENT_ANNOTATION,
                ForgeConstants.EVENT == eventClass.qualifiedName,
            )
        }

        return formatWrongEventMessage(
            ForgeConstants.EVENT,
            ForgeConstants.EVENT_HANDLER_ANNOTATION,
            ForgeConstants.FML_EVENT == eventClass.qualifiedName,
        )
    }

    private fun formatWrongEventMessage(expected: String, suggested: String, wrong: Boolean): String {
        val base = "Parameter is not a subclass of $expected\n"
        if (wrong) {
            return base + "This method should be annotated with $suggested"
        }
        return base + "Compiling and running this listener may result in a runtime exception"
    }

    override fun isStaticListenerSupported(method: PsiMethod) = true

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val psiClass = runCatchingKtIdeaExceptions { identifier.uastParent as? UClass }
            ?: return false

        return !psiClass.javaPsi.hasModifier(JvmModifier.ABSTRACT) &&
            psiClass.findAnnotation(ForgeConstants.MOD_ANNOTATION) != null
    }

    override fun checkUselessCancelCheck(expression: PsiMethodCallExpression): IsCancelled? = null

    override fun dispose() {
        mcmod = null
        modsToml = null
        super.dispose()
    }

    companion object {
        fun getModsFromModsToml(project: Project, modsToml: VirtualFile?): List<String> {
            val tomlFile = PsiManager.getInstance(project).findFile(modsToml ?: return emptyList()) as? TomlFile
                ?: return emptyList()

            val result = mutableListOf<String>()

            for (child in tomlFile.children) {
                if (child is TomlArrayTable && child.header.key?.name == "mods") {
                    for (entry in child.entries) {
                        if (entry.key.name == "modId") {
                            entry.value?.stringValue()?.let { result += it }
                        }
                    }
                }
            }

            return result
        }
    }
}
