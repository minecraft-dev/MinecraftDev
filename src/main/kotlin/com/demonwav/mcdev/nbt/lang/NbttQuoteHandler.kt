/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.nbt.lang

import com.demonwav.mcdev.nbt.lang.gen.psi.NbttTypes
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler

class NbttQuoteHandler : SimpleTokenSetQuoteHandler(NbttTypes.STRING_LITERAL)
