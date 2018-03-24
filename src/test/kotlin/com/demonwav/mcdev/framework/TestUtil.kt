/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

@file:JvmName("TestUtil")
package com.demonwav.mcdev.framework

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.StandardFileSystems

val mockJdk by lazy {
    val path = findLibraryPath("mockJDK")
    val rtFile = StandardFileSystems.local().findFileByPath(path)!!
    val rt = JarFileSystem.getInstance().getRootByLocal(rtFile)!!
    val home = rtFile.parent!!
    MockJdk("1.7", rt, home)
}

private fun findLibraryPath(name: String) = FileUtil.toSystemIndependentName(System.getProperty("testLibs.$name")!!)
private fun findLibrary(name: String) = StandardFileSystems.jar().refreshAndFindFileByPath(findLibraryPath(name) + JarFileSystem.JAR_SEPARATOR)

fun createLibrary(project: Project, name: String): Library {
    val table = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
    return table.getLibraryByName(name) ?: run {
        val library = table.createLibrary(name)
        val libraryModel = library.modifiableModel
        libraryModel.addRoot(findLibrary(name)!!, OrderRootType.CLASSES)
        libraryModel.commit()
        library
    }
}

fun String.toSnakeCase(postFix: String = "") =
    replace(" ", "_") + postFix
