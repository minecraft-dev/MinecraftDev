/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge

import com.demonwav.mcdev.util.Constants
import com.intellij.codeInsight.ImportFilter
import com.intellij.psi.PsiFile

class SpongeImportFilter : ImportFilter() {

    override fun shouldUseFullyQualifiedName(targetFile: PsiFile, classQualifiedName: String): Boolean {
        if (!SpongeModuleType.isInModule(targetFile)) {
            return false
        }

        return classQualifiedName == Constants.JAVA_UTIL_LOGGER
    }
}
