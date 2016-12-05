/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.exception

import javax.swing.JComponent

class MinecraftSetupException(msg: String, val j: JComponent) : Exception(msg) {

    val error: String
        get() {
            when (message) {
                "empty" -> return "<html>Please fill in all required fields</html>"
                "bad" -> return "<html>Please enter author and plugin names as a comma separated list</html>"
                "fillAll" -> return "<html>Please fill in all fields</html>"
                else -> return String.format("<html>%s</html>", message)
            }
        }
}
