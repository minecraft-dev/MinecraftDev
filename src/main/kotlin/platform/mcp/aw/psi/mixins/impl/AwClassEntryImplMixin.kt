/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.aw.psi.mixins.impl

import com.demonwav.mcdev.platform.mcp.aw.psi.mixins.AwClassEntryMixin
import com.intellij.lang.ASTNode

abstract class AwClassEntryImplMixin(node: ASTNode) : AwEntryImplMixin(node), AwClassEntryMixin
