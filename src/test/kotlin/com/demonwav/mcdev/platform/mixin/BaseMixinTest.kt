/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.BaseMinecraftTest
import com.demonwav.mcdev.framework.createLibrary
import com.demonwav.mcdev.platform.PlatformType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel

abstract class BaseMixinTest : BaseMinecraftTest(PlatformType.MIXIN) {

    override fun postConfigureModule(module: Module, model: ModifiableRootModel) {
        model.addLibraryEntry(createLibrary(module.project, "mixin"))
    }
}
