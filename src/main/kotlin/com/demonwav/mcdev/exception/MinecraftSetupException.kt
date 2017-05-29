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
                "empty" -> return "<html>${MCMessages["setup.error.empty"]}</html>"
                "bad" -> return "<html>${MCMessages["setup.error.bad"]}</html>"
                "fillAll" -> return "<html>${MCMessages["setup.error.fill_all"]}</html>"
                else -> return "<html>$message</html>"
            }
        }
}
