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

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder

val MIXIN_DEBUG_KEY = Key.create<Boolean>("mixin.debug")

fun UserDataHolder.hasMixinDebugKey(): Boolean = getUserData(MIXIN_DEBUG_KEY) == true
