/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
    private var testDataLibrary: Library? = null

    @BeforeEach
    fun initMixin() {
        runWriteTask {
            mixinLibrary = createLibrary(project, "mixin")
            testDataLibrary = createLibrary(project, "mixin-test-data")
        }

        ModuleRootModificationUtil.updateModel(module) { model ->
            model.addLibraryEntry(mixinLibrary ?: throw IllegalStateException("Mixin library not created"))
            model.addLibraryEntry(testDataLibrary ?: throw IllegalStateException("Test data library not created"))
            val orderEntries = model.orderEntries
            orderEntries.rotate(2)
            model.rearrangeOrderEntries(orderEntries)
        }
    }

    @AfterEach
    fun cleanupMixin() {
        for (l in listOfNotNull(mixinLibrary, testDataLibrary)) {
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
