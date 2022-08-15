/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.fabricloom

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.unscramble.UnscrambleSupport
import java.awt.Dimension
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.JPanel
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import org.jdesktop.swingx.combobox.MapComboBoxModel
import org.jetbrains.plugins.gradle.util.GradleUtil

class TinyUnscrambler : UnscrambleSupport<TinyUnscrambler.SettingsComponent> {

    private val symbolPattern = Regex("(([\\w$]+\\.)*(class_\\d+\\$?)+)|((?:field|method)_\\d+)")

    override fun getPresentableName() = "Remap Tiny names"

    class SettingsComponent(mappings: Map<String, Path>) : JPanel(GridLayoutManager(1, 2)) {

        val mappingsBoxModel = MapComboBoxModel(mappings)
        val mappingsBox = ComboBox(mappingsBoxModel)

        init {
            mappingsBox.renderer = SimpleListCellRenderer.create { label, value, _ ->
                val path = mappingsBoxModel.getValue(value)
                label.text = "[$value] $path"
            }
            add(
                JBLabel("Mappings: "),
                GridConstraints(
                    0,
                    0,
                    1,
                    1,
                    GridConstraints.ANCHOR_WEST,
                    GridConstraints.FILL_NONE,
                    GridConstraints.SIZEPOLICY_FIXED,
                    GridConstraints.SIZEPOLICY_FIXED,
                    Dimension(-1, -1),
                    Dimension(-1, -1),
                    Dimension(-1, -1),
                )
            )
            add(
                mappingsBox,
                GridConstraints(
                    0,
                    1,
                    1,
                    1,
                    GridConstraints.ANCHOR_WEST,
                    GridConstraints.FILL_HORIZONTAL,
                    GridConstraints.SIZEPOLICY_WANT_GROW,
                    GridConstraints.SIZEPOLICY_FIXED,
                    Dimension(-1, -1),
                    Dimension(-1, -1),
                    Dimension(-1, -1),
                )
            )
        }
    }

    override fun createSettingsComponent(): SettingsComponent {
        val mappings = mutableMapOf<String, Path?>()
        for (project in ProjectManager.getInstance().openProjects) {
            ModuleManager.getInstance(project).modules
                .filter { MinecraftFacet.getInstance(it, FabricModuleType) != null }
                .associateTo(mappings) { module ->
                    val loomData = GradleUtil.findGradleModuleData(module)?.children
                        ?.find { it.key == FabricLoomData.KEY }?.data as? FabricLoomData
                        ?: return@associateTo module.name to null
                    module.name to loomData.tinyMappings?.toPath()
                }
        }

        @Suppress("UNCHECKED_CAST")
        return SettingsComponent(mappings.filterValues { it != null } as Map<String, Path>)
    }

    override fun unscramble(project: Project, text: String, logName: String, settings: SettingsComponent?): String? {
        val mappingsFile = logName.takeIf(String::isNotBlank)?.let(Paths::get)
            ?: settings?.mappingsBoxModel?.let { mappings -> mappings.selectedItem?.let(mappings::getValue) }
            ?: return null

        val interToNamed = mutableMapOf<String, String>()
        val visitor = object : MappingVisitor {
            var interNsIndex = -1
            var namedNsIndex = -1

            lateinit var src: String
            var inter: String? = null
            var named: String? = null

            override fun visitNamespaces(srcNamespace: String, dstNamespaces: MutableList<String>) {
                namedNsIndex = dstNamespaces.indexOf("named")
                interNsIndex = dstNamespaces.indexOf("intermediary")
            }

            override fun visitContent(): Boolean {
                return interNsIndex >= 0 && namedNsIndex >= 0
            }

            override fun visitClass(srcName: String): Boolean {
                src = srcName.replace('/', '.')
                return true
            }

            override fun visitField(srcName: String, srcDesc: String): Boolean {
                src = srcName
                return true
            }

            override fun visitMethod(srcName: String, srcDesc: String): Boolean {
                src = srcName
                return true
            }

            override fun visitMethodArg(argPosition: Int, lvIndex: Int, srcName: String): Boolean {
                return false
            }

            override fun visitMethodVar(lvtRowIndex: Int, lvIndex: Int, startOpIdx: Int, srcName: String): Boolean {
                return false
            }

            override fun visitDstName(targetKind: MappedElementKind, namespace: Int, name: String) {
                when (namespace) {
                    interNsIndex -> inter = name
                    namedNsIndex -> named = name
                    else -> return
                }

                if (inter != null && named != null) {
                    interToNamed[inter!!] = named!!
                    if (targetKind == MappedElementKind.CLASS) {
                        // Remap dot-separated qualified classes and simple class names too
                        interToNamed[inter!!.replace('/', '.')] = named!!.replace('/', '.')
                        interToNamed[inter!!.substringAfterLast('/')] = named!!.substringAfterLast('/')
                    }
                }
            }

            override fun visitComment(targetKind: MappedElementKind, comment: String) {
            }

            override fun visitElementContent(targetKind: MappedElementKind?): Boolean {
                return targetKind != MappedElementKind.METHOD
            }
        }

        MappingReader.read(mappingsFile, visitor)
        return symbolPattern.replace(text) {
            interToNamed[it.value] ?: it.value
        }
    }
}
