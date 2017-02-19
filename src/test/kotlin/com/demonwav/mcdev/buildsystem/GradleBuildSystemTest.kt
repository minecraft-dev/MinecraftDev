/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */
package com.demonwav.mcdev.buildsystem

import com.demonwav.mcdev.buildsystem.gradle.GradleBuildSystem
import com.demonwav.mcdev.platform.MinecraftModule

class GradleBuildSystemTest : BaseBuildSystemTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            gradle("build.gradle", """
                apply plugin: 'java'

                group = pluginGroup
                version = pluginVersion
            """)
        }
    }

    fun testDetectGradle() {
        assertTrue(MinecraftModule.getInstance(myModule)?.buildSystem is GradleBuildSystem)
    }
}
