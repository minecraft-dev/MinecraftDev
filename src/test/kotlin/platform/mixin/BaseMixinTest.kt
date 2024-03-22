/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.createLibrary
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.rotate
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseMixinTest : BaseMinecraftTest(PlatformType.MIXIN) {

    private var mixinLibrary: Library? = null
    private var mixinExtrasLibrary: Library? = null
    private var testDataLibrary: Library? = null

    @BeforeEach
    fun initMixin() {
        runWriteTask {
            mixinLibrary = createLibrary(project, "mixin")
            mixinExtrasLibrary = createLibrary(project, "mixinextras-common") // TODO: this will probably change
            testDataLibrary = createLibrary(project, "mixin-test-data")
        }

        ModuleRootModificationUtil.updateModel(module) { model ->
            model.addLibraryEntry(mixinLibrary ?: throw IllegalStateException("Mixin library not created"))
            model.addLibraryEntry(mixinExtrasLibrary ?: throw IllegalStateException("MixinExtras library not created"))
            model.addLibraryEntry(testDataLibrary ?: throw IllegalStateException("Test data library not created"))
            val orderEntries = model.orderEntries
            orderEntries.rotate(3)
            model.rearrangeOrderEntries(orderEntries)
        }
    }

    @AfterEach
    fun cleanupMixin() {
        for (l in listOfNotNull(mixinLibrary, testDataLibrary)) {
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
