/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.creator.MinecraftProjectCreator
import com.demonwav.mcdev.creator.ProjectConfig
import com.demonwav.mcdev.creator.buildsystem.BuildSystem
import javax.swing.JTextField
import org.apache.commons.lang.WordUtils

inline fun <reified T : ProjectConfig> modUpdateStep(
    creator: MinecraftProjectCreator,
    modNameField: JTextField
): Pair<T, BuildSystem>? {
    val buildSystem = creator.buildSystem ?: return null

    modNameField.text = WordUtils.capitalize(buildSystem.artifactId.replace('-', ' '))

    val config = creator.config as? T ?: return null
    return config to buildSystem
}
