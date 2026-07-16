/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.bukkit

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.NoEdt
import com.demonwav.mcdev.framework.createLibrary
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseSpigotTest : BaseMinecraftTest(PlatformType.SPIGOT) {

    private var library: Library? = null

    @NoEdt
    @BeforeEach
    fun initSpigot() {
        runWriteTask {
            library = createLibrary(project, "spigot-api")
        }

        ModuleRootModificationUtil.updateModel(module) { model ->
            model.addLibraryEntry(library ?: throw IllegalStateException("Library not created"))
            val orderEntries = model.orderEntries
            val last = orderEntries.last()
            System.arraycopy(orderEntries, 0, orderEntries, 1, orderEntries.size - 1)
            orderEntries[0] = last
            model.rearrangeOrderEntries(orderEntries)
        }
    }

    @AfterEach
    fun cleanupSpigot() {
        library?.let { l ->
            ModuleRootModificationUtil.updateModel(module) { model ->
                model.removeOrderEntry(
                    model.findLibraryOrderEntry(l) ?: throw IllegalStateException("Library not found"),
                )
            }

            runWriteTask {
                val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
                table.modifiableModel.let { model ->
                    model.removeLibrary(l)
                    model.commit()
                }
            }
        }
    }
}
