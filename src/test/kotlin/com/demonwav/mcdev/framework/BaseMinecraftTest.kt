/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.framework

import com.demonwav.mcdev.ProjectComponentHandler
import com.demonwav.mcdev.buildsystem.BuildSystem
import com.demonwav.mcdev.buildsystem.BuildSystemInstanceManager
import com.demonwav.mcdev.framework.buildsystem.TestBuildSystemInstanceManager
import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.MinecraftModuleType
import com.demonwav.mcdev.platform.ProjectComponentManager
import com.intellij.JavaTestUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor

abstract class BaseMinecraftTest(protected vararg val moduleTypes: AbstractModuleType<*>) : ProjectBuilderTest() {

    protected var buildSystemInstanceManager: BuildSystemInstanceManager = TestBuildSystemInstanceManager
    protected var projectComponentHandler: ProjectComponentHandler = TestProjectComponentHandler

    protected open fun preConfigureModule(module: Module, model: ModifiableRootModel) {}
    protected open fun postConfigureModule(module: Module, model: ModifiableRootModel) {}

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : DefaultLightProjectDescriptor() {
            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)

                preConfigureModule(module, model)

                BuildSystem.instanceManager = buildSystemInstanceManager
                ProjectComponentManager.handler = projectComponentHandler

                moduleTypes.forEach {
                    MinecraftModuleType.addOption(module, it.id, false)
                }
                postConfigureModule(module, model)
            }

            // TODO: Figure out how to package Mock JDK to speed up builds
            override fun getSdk() = JavaTestUtil.getTestJdk()
        }
    }
}
