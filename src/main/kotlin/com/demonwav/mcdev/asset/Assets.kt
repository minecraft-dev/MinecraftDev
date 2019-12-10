/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.asset

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

abstract class Assets protected constructor() {
    protected fun loadIcon(path: String): Icon {
        return IconLoader.getIcon(path, Assets::class.java)
    }
}
