/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem

import com.demonwav.mcdev.buildsystem.maven.MavenBuildSystem
import com.demonwav.mcdev.platform.MinecraftModule

class MavenBuildSystemTest : BaseBuildSystemTest() {

    override fun setUp() {
        super.setUp()

        buildProject {
            xml("pom.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">

                    <modelVersion>4.0.0</modelVersion>

                    <groupId>com.demonwav.test</groupId>
                    <artifactId>test</artifactId>
                    <version>0.4.0-SNAPSHOT</version>
                </project>
            """)
        }
    }

    fun testDetectMaven() {
        assertInstanceOf(MinecraftModule.getInstance(myModule)?.buildSystem, MavenBuildSystem::class.java)
    }
}
