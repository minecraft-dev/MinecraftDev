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
import com.demonwav.mcdev.platform.MinecraftModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor

abstract class BaseMinecraftTestCase(
    private vararg val moduleTypes: AbstractModuleType<*>
) : ProjectBuilderTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : DefaultLightProjectDescriptor() {
            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)
                moduleTypes.forEach {
                    MinecraftModuleType.addOption(module, it.id)
                }
            }
        }
    }
}
