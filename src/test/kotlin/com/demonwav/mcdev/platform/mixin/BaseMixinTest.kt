/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.VirtualFileManager

abstract class BaseMixinTest : BaseMinecraftTest(PlatformType.MIXIN) {

    override fun configureModule(module: Module, model: ModifiableRootModel) {
        // If we're lucky, the following code adds the Mixin library to the project
        val mixinPath = FileUtil.toSystemIndependentName(System.getProperty("mixinUrl")!!)

        val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)

        val library = table.createLibrary("Mixin")
        val libraryModel = library.modifiableModel
        libraryModel.addRoot(VirtualFileManager.constructUrl(JarFileSystem.PROTOCOL, mixinPath) + JarFileSystem.JAR_SEPARATOR,
            OrderRootType.CLASSES)
        libraryModel.commit()
        model.addLibraryEntry(library)
    }
}
