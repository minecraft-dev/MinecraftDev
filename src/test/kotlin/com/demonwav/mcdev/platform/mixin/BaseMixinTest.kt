/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.createLibrary
import com.demonwav.mcdev.platform.PlatformType
import com.demonwav.mcdev.util.runWriteTask
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseMixinTest : BaseMinecraftTest(PlatformType.MIXIN) {

    private var library: Library? = null

    @BeforeEach
    fun initMixin() {
        runWriteTask {
            library = createLibrary(project, "mixin")
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
    fun cleanupMixin() {
        library?.let { l ->
            ModuleRootModificationUtil.updateModel(module) { model ->
                model.removeOrderEntry(
                    model.findLibraryOrderEntry(l) ?: throw IllegalStateException("Library not found")
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
