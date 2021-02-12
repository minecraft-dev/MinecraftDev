/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.buildsystem

import java.nio.file.Files
import java.nio.file.Path

data class DirectorySet(
    val sourceDirectory: Path,
    val resourceDirectory: Path,
    val testSourceDirectory: Path,
    val testResourceDirectory: Path
) {
    companion object {
        fun create(dir: Path): DirectorySet {
            val sourceDirectory = dir.resolve("src/main/java")
            val resourceDirectory = dir.resolve("src/main/resources")
            val testSourceDirectory = dir.resolve("src/test/java")
            val testResourceDirectory = dir.resolve("src/test/resources")
            Files.createDirectories(sourceDirectory)
            Files.createDirectories(resourceDirectory)
            Files.createDirectories(testSourceDirectory)
            Files.createDirectories(testResourceDirectory)
            return DirectorySet(sourceDirectory, resourceDirectory, testSourceDirectory, testResourceDirectory)
        }
    }
}
