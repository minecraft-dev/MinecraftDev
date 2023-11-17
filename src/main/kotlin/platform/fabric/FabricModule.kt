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

package com.demonwav.mcdev.platform.fabric

import com.demonwav.mcdev.asset.PlatformAssets
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.fabric.reference.EntryPointReference
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.mcp.fabricloom.FabricLoomData
import com.demonwav.mcdev.platform.mcp.mappings.HardcodedYarnToMojmap
import com.demonwav.mcdev.platform.mcp.mappings.HasCustomNamedMappings
import com.demonwav.mcdev.platform.mcp.mappings.MappingsManager
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.nullable
import com.demonwav.mcdev.util.runCatchingKtIdeaExceptions
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.ReferencesSearch
import java.io.IOException
import net.fabricmc.mappingio.MappedElementKind
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.MappingVisitor
import org.jetbrains.plugins.gradle.util.GradleUtil
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UIdentifier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.toUElementOfType

class FabricModule internal constructor(facet: MinecraftFacet) : AbstractModule(facet), HasCustomNamedMappings {

    var fabricJson by nullable { facet.findFile(FabricConstants.FABRIC_MOD_JSON, SourceType.RESOURCE) }
        private set

    private var namedToMojangManagerField: MappingsManager? = null
    override val namedToMojangManager: MappingsManager?
        get() = namedToMojangManagerField

    override val moduleType = FabricModuleType
    override val type = PlatformType.FABRIC
    override val icon = PlatformAssets.FABRIC_ICON

    override fun isEventClassValid(eventClass: PsiClass, method: PsiMethod?) = true

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    override fun shouldShowPluginIcon(element: PsiElement?): Boolean {
        val identifier = element?.toUElementOfType<UIdentifier>()
            ?: return false

        val parent = runCatchingKtIdeaExceptions { identifier.uastParent }
        if (parent !is UClass && parent !is UMethod) {
            return false
        }

        val psiParent = parent.sourcePsi
            ?: return false
        return ReferencesSearch.search(psiParent).anyMatch { EntryPointReference.isEntryPointReference(it) }
    }

    override fun refresh() {
        namedToMojangManagerField = if (detectYarn()) {
            MappingsManager.Immediate(HardcodedYarnToMojmap.createMappings())
        } else {
            null
        }
    }

    private fun detectYarn(): Boolean {
        val gradleData = GradleUtil.findGradleModuleData(facet.module) ?: return false
        val loomData =
            gradleData.children.find { it.key == FabricLoomData.KEY }?.data as? FabricLoomData ?: return false
        val mappingsFile = loomData.tinyMappings ?: return false

        var yarnDetected = false
        val visitor = object : MappingVisitor {
            private var namedIndex = -1

            override fun visitNamespaces(srcNamespace: String?, dstNamespaces: List<String>) {
                namedIndex = dstNamespaces.indexOf("named")
            }

            override fun visitContent() = namedIndex >= 0

            override fun visitClass(srcName: String) = true

            override fun visitField(srcName: String?, srcDesc: String?) = false

            override fun visitMethod(srcName: String?, srcDesc: String?) = false

            override fun visitMethodArg(argPosition: Int, lvIndex: Int, srcName: String?) = false

            override fun visitMethodVar(lvtRowIndex: Int, lvIndex: Int, startOpIdx: Int, srcName: String?) = false

            override fun visitDstName(targetKind: MappedElementKind?, namespace: Int, name: String) {
                if (namespace == namedIndex && name == "net/minecraft/client/MinecraftClient") {
                    yarnDetected = true
                }
            }

            override fun visitComment(targetKind: MappedElementKind?, comment: String?) {
            }
        }

        try {
            MappingReader.read(mappingsFile.toPath(), visitor)
        } catch (e: IOException) {
            return false
        }

        return yarnDetected
    }

    override fun dispose() {
        super.dispose()
        fabricJson = null
    }
}
