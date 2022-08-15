/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.exception

import javax.swing.JComponent

sealed class SetupException(val j: JComponent) : Exception() {

    abstract val error: String
}

class EmptyFieldSetupException(j: JComponent) : SetupException(j) {
    override val error: String
        get() = "<html>Please fill in all required fields</html>"
}

class BadListSetupException(j: JComponent) : SetupException(j) {
    override val error: String
        get() = "<html>Please enter as a comma separated list</html>"
}

class EmptyInputSetupException(j: JComponent) : SetupException(j) {
    override val error: String
        get() = "<html>Please fill in all required fields</html>"
}

class InvalidClassNameException(j: JComponent) : SetupException(j) {
    override val error: String
        get() = "<html>Class Name must be a valid Java identifier and cannot be the default package</html>"
}

class OtherSetupException(private val msg: String, j: JComponent) : SetupException(j) {
    override val error: String
        get() = "<html>$msg</html>"
}
