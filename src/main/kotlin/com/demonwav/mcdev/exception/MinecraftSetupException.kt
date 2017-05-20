/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.exception

import com.demonwav.mcdev.asset.MCMessages
import javax.swing.JComponent

class MinecraftSetupException(msg: String, val j: JComponent) : Exception(msg) {

    val error: String
        get() {
            when (message) {
                "empty" -> return MCMessages["setup.error.empty"]
                "bad" -> return MCMessages["setup.error.bad"]
                "fillAll" -> return MCMessages["setup.error.fill_all"]
                else -> return String.format("<html>%s</html>", message)
            }
        }
}
