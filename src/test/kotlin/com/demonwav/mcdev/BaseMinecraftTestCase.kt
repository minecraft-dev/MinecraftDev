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

import com.demonwav.mcdev.platform.AbstractModuleType
import com.demonwav.mcdev.platform.MinecraftModule
import com.demonwav.mcdev.platform.MinecraftModuleType
import com.intellij.JavaTestUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor

abstract class BaseMinecraftTestCase(
    private vararg val moduleTypes: AbstractModuleType<*>
) : ProjectBuilderTestCase() {

    protected open fun configureModule(module: Module, model: ModifiableRootModel) {
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : DefaultLightProjectDescriptor() {
            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)
                moduleTypes.forEach {
                    MinecraftModuleType.addOption(module, it.id)
                    // TODO: DemonWav: Why do we need this? Module type isn't added properly otherwise
                    MinecraftModule.getInstance(module)!!.addModuleType(it.id)
                }
                configureModule(module, model)
            }

            // TODO: Figure out how to package Mock JDK to speed up builds
            override fun getSdk() = JavaTestUtil.getTestJdk()
        }
    }
}
