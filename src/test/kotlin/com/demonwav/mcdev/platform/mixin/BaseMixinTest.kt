/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
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

abstract class BaseMixinTest : BaseMinecraftTest(PlatformType.MIXIN) {

    private var library: Library? = null

    override fun setUp() {
        super.setUp()

        runWriteTask {
            library = createLibrary(project, "mixin")
        }

        ModuleRootModificationUtil.updateModel(myModule) { model ->
            model.addLibraryEntry(library ?: throw IllegalStateException("Library not created"))
            val orderEntries = model.orderEntries
            val last = orderEntries.last()
            System.arraycopy(orderEntries, 0, orderEntries, 1, orderEntries.size - 1)
            orderEntries[0] = last
            model.rearrangeOrderEntries(orderEntries)
        }
    }

    override fun tearDown() {
        library?.let { l ->
            ModuleRootModificationUtil.updateModel(myModule) { model ->
                model.removeOrderEntry(model.findLibraryOrderEntry(l) ?: throw IllegalStateException("Library not found"))
            }

            runWriteTask {
                val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
                table.modifiableModel.let { model ->
                    model.removeLibrary(l)
                    model.commit()
                }
            }
        }

        super.tearDown()
    }
}
