/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang.format

import com.demonwav.mcdev.nbt.lang.NbttLanguage
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

@Suppress("PropertyName")
class NbttCodeStyleSettings(container: CodeStyleSettings) : CustomCodeStyleSettings(NbttLanguage.id, container) {

    @JvmField
    var SPACE_AFTER_COLON = true

    @JvmField
    var SPACE_BEFORE_COLON = false

    @JvmField
    var LIST_WRAPPING = CommonCodeStyleSettings.WRAP_AS_NEEDED

    @JvmField
    var ARRAY_WRAPPING = CommonCodeStyleSettings.WRAP_AS_NEEDED
}
