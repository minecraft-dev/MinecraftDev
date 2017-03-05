/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.facet

import com.demonwav.mcdev.buildsystem.SourceType
import com.google.common.collect.HashMultimap
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ui.configuration.libraries.LibraryPresentationManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.jetbrains.jps.model.java.JavaSourceRootType

class ModuleData private constructor(private val module: Module) {

    val roots: HashMultimap<SourceType, VirtualFile> = HashMultimap.create()

    init {
        val rootManager = ModuleRootManager.getInstance(module)
        for (entry in rootManager.contentEntries) {
            for (sourceFolder in entry.sourceFolders) {
                when (sourceFolder.rootType) {
                    JavaSourceRootType.SOURCE -> roots.put(SourceType.SOURCE, sourceFolder.file)
                    JavaSourceRootType.TEST_SOURCE -> roots.put(SourceType.TEST_SOURCE, sourceFolder.file)
                    JavaResourceRootType.RESOURCE -> roots.put(SourceType.RESOURCE, sourceFolder.file)
                    JavaResourceRootType.TEST_RESOURCE -> roots.put(SourceType.TEST_RESOURCE, sourceFolder.file)
                    else -> {
                    }
                }
            }
        }

        val presentationManager = LibraryPresentationManager.getInstance()
        rootManager.orderEntries()
            .recursively()
            .librariesOnly()
//            .forEachLibrary { library ->
//                if (presentationManager.isLibraryOfKind(library, ))
//            }
        for (entry in rootManager.orderEntries) {
            when (entry) {
                is LibraryOrderEntry -> {
                    entry.library
                }
            }
        }
    }

    companion object {
        val instanceMap = mutableMapOf<Module, ModuleData>()

        fun getInstance(module: Module): ModuleData {
            return instanceMap.computeIfAbsent(module, ::ModuleData)
        }
    }
}
