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

package com.demonwav.mcdev.platform.mcp

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.AbstractModule
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.platform.mcp.srg.SrgManager
import com.demonwav.mcdev.platform.mcp.util.McpConstants
import com.demonwav.mcdev.translations.TranslationFileListener
import com.demonwav.mcdev.util.runWriteTaskLater
import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.util.messages.MessageBusConnection
import javax.swing.Icon

class McpModule(facet: MinecraftFacet) : AbstractModule(facet) {

    private lateinit var connection: MessageBusConnection

    private val settings: McpModuleSettings = McpModuleSettings.getInstance(module)
    val accessTransformers = mutableSetOf<VirtualFile>()

    var srgManager: SrgManager? = null
        private set

    override fun init() {
        initSrg()
        connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, TranslationFileListener)

        runWriteTaskLater {
            FileTypeManager.getInstance().associatePattern(JsonFileType.INSTANCE, McpConstants.PNG_MCMETA)
        }
    }

    private fun initSrg() {
        val settings = getSettings()
        val file = settings.mappingFile ?: return
        val srgType = settings.srgType ?: return

        srgManager = SrgManager.getInstance(file, srgType)
        srgManager?.parse()
    }

    override val moduleType = McpModuleType
    override val type = PlatformType.MCP
    override val icon: Icon? = null

    override fun writeErrorMessageForEventParameter(eventClass: PsiClass, method: PsiMethod) = ""

    fun getSettings() = settings.state

    fun updateSettings(data: McpModuleSettings.State) {
        this.settings.loadState(data)
        val mappingFile = data.mappingFile ?: return
        val srgType = data.srgType ?: return

        srgManager = SrgManager.getInstance(mappingFile, srgType)
        srgManager?.parse()
    }

    fun addAccessTransformerFile(file: VirtualFile) {
        accessTransformers.add(file)
    }

    override fun dispose() {
        super.dispose()

        connection.disconnect()
        accessTransformers.clear()
        srgManager = null
    }
}
