/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

abstract class ProjectBuilderTestCase : LightCodeInsightFixtureTestCase() {

    fun buildProject(builder: TestProjectBuilder.() -> Unit) = TestProjectBuilder(myFixture).build(builder)
}
