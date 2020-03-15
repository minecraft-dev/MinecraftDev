/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.debug

import com.intellij.ui.classFilter.ClassFilter
import com.intellij.ui.classFilter.DebuggerClassFilterProvider

/**
 * Prevents stepping into Mixin's CallbackInfo class.
 */
class MixinDebuggerClassFilterProvider : DebuggerClassFilterProvider {

    private val filters = listOf(
        ClassFilter("org.spongepowered.asm.mixin.injection.callback.*")
    )

    override fun getFilters(): List<ClassFilter> {
        return filters
    }
}
