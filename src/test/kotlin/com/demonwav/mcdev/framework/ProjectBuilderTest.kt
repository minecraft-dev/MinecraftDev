/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.util.ReflectionUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class ProjectBuilderTest(descriptor: LightProjectDescriptor? = null) {

    protected val fixture: JavaCodeInsightTestFixture
    protected val project: Project
        get() = fixture.project
    protected val module: Module
        get() = fixture.module

    init {
        val lightFixture = IdeaTestFixtureFactory.getFixtureFactory()
            .createLightFixtureBuilder(descriptor)
            .fixture

        fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(lightFixture)
    }

    fun buildProject(root: VirtualFile = fixture.project.baseDirPath, builder: ProjectBuilder.() -> Unit) =
            ProjectBuilder(fixture, root).build(builder)

    fun ProjectBuilder.src(block: ProjectBuilder.() -> Unit) {
        val srcFolder = VfsUtil.createDirectoryIfMissing(project.baseDirPath, "src")
        val entry = fixture.module.rootManager.contentEntries.first { it.file == project.baseDirPath }
        if (!entry.sourceFolderFiles.contains(srcFolder)) {
            ModuleRootModificationUtil.updateModel(fixture.module) { model ->
                model.contentEntries.first { it.file == project.baseDirPath }.addSourceFolder(srcFolder, false)
            }
        }

        dir("src", block)
    }

    @NoEdt
    @BeforeEach
    fun setup() {
        if (this is CustomDataPath) {
            // Total hack, but I have yet to find a solution to this otherwise..
            // This field has to be set for the comment tests
            //   - I18nCommenterTest
            //   - AtCommenterTest
            val myTestDataPathField = ReflectionUtil.getDeclaredField(fixture.javaClass, "myTestDataPath")!!
            myTestDataPathField.isAccessible = true
            myTestDataPathField.set(fixture, testDataPath)
        }
        fixture.setUp()
    }

    @AfterEach
    fun tearDown() {
        ModuleRootModificationUtil.updateModel(fixture.module) { model ->
            model.removeContentEntry(model.contentEntries.first { it.file == project.baseDirPath })
        }

        fixture.tearDown()
    }
}
