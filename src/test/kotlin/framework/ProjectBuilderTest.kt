/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.framework

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.fixtures.TempDirTestFixture
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import kotlin.reflect.KClass
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class ProjectBuilderTest(descriptor: LightProjectDescriptor? = null) {

    protected val fixture: JavaCodeInsightTestFixture
    protected val project: Project
        get() = fixture.project
    protected val module: Module
        get() = fixture.module

    private val tempDirFixture: TempDirTestFixture

    init {
        val lightFixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createLightFixtureBuilder(descriptor, "mcdev_test_project")
            .fixture

        // This is a poorly named class - it actually means create a temp dir test fixture _in-memory_
        tempDirFixture = LightTempDirTestFixtureImpl(true)
        fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(lightFixture, tempDirFixture)
    }

    fun buildProject(builder: ProjectBuilder.() -> Unit) =
        ProjectBuilder(fixture, tempDirFixture).build(builder)

    fun JavaCodeInsightTestFixture.enableInspections(vararg classes: KClass<out LocalInspectionTool>) {
        this.enableInspections(classes.map { it.java })
    }

    @NoEdt
    @BeforeEach
    fun setup() {
        fixture.setUp()
    }

    @AfterEach
    fun tearDown() {
        fixture.tearDown()
    }
}
