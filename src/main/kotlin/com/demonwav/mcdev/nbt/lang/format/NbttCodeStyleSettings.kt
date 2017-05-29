/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

class NbttCodeStyleSettings(container: CodeStyleSettings) : CustomCodeStyleSettings(NbttLanguage.id, container) {

    var SPACE_AFTER_COLON = true
    var SPACE_BEFORE_COLON = false
    var LIST_WRAPPING = CommonCodeStyleSettings.WRAP_ALWAYS
    var OBJECT_WRAPPING = CommonCodeStyleSettings.WRAP_ALWAYS
}
