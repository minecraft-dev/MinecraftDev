/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at.completion

import com.intellij.codeInsight.completion.PrefixMatcher
import com.intellij.codeInsight.lookup.LookupElement

class SrgPrefixMatcher(prefix: String) : PrefixMatcher(prefix) {
    override fun prefixMatches(name: String) = true
    override fun cloneWithPrefix(prefix: String) = SrgPrefixMatcher(prefix)

    override fun prefixMatches(element: LookupElement): Boolean {
        return element.lookupString.contains(myPrefix, ignoreCase = true)
    }
}
