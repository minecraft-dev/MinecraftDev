package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.creator.selectProxy
import com.demonwav.mcdev.update.PluginUtil
import com.demonwav.mcdev.util.virtualFile
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.result.getOrNull
import com.github.kittinunf.result.onError
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.observable.properties.PropertyGraph
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.ZipUtil
import java.util.function.Consumer
import javax.swing.JComponent
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.moveTo
import kotlin.io.path.writeBytes

class BuiltInTemplateProvider : TemplateProvider {

    private val builtinTemplatesPath = PluginUtil.plugin.pluginPath.resolve("lib/resources/builtin-templates")
    private var updatedBuiltinTemplates = false

    override fun getLabel(): String = "Built In"

    override fun init(indicator: ProgressIndicator) {
        if (!updatedBuiltinTemplates && MinecraftSettings.instance.isAutoUpdateBuiltinTemplate) {
            indicator.text2 = "Updating builtin templates"

            val manager = FuelManager()
            val url = "https://github.com/RedNesto/mcdev-templates/archive/refs/heads/main.zip"

            manager.proxy = selectProxy(url)

            val (_, _, result) = manager.get(url)
                .header("User-Agent", "github_org/minecraft-dev/${PluginUtil.pluginVersion}")
                .header("Accepts", "application/json")
                .timeout(10000)
                .response()

            val data = result.onError {
                thisLogger().warn("Could not fetch builtin templates update", it)
            }.getOrNull() ?: return

            try {
                val zipPath = PluginUtil.plugin.pluginPath.resolve("lib/resources/builtin-templates.zip")
                zipPath.writeBytes(data)
                FileUtil.deleteRecursively(builtinTemplatesPath)
                ZipUtil.extract(zipPath, builtinTemplatesPath, null)
                for (child in builtinTemplatesPath.resolve("mcdev-templates-main").listDirectoryEntries()) {
                    child.moveTo(builtinTemplatesPath.resolve(child.fileName))
                }

                updatedBuiltinTemplates = true
                thisLogger().info("Builtin template update applied successfully")
            } catch (e: Exception) {
                thisLogger().error("Failed to apply builtin templates update", e)
            }
        }
    }

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> Collection<LoadedTemplate>>
    ): JComponent? {
        provideTemplate.accept {
            builtinTemplatesPath.virtualFile?.let(TemplateProvider::findTemplates) ?: emptyList()
        }

        return null
    }

    override fun deserializeAndLoad(element: String): LoadedTemplate? = TemplateProvider.deserializeAndLoadVfs(element)
}
