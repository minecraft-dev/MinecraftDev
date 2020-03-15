/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.forge.inspections.simpleimpl

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.util.extendsOrImplements
import com.intellij.psi.PsiClass

object SimpleImplUtil {

    fun isMessageOrHandler(aClass: PsiClass): Boolean {
        return aClass.extendsOrImplements(ForgeConstants.NETWORK_MESSAGE) ||
            aClass.extendsOrImplements(ForgeConstants.NETWORK_MESSAGE_HANDLER)
    }
}
