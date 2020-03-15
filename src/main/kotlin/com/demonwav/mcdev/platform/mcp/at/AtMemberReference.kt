/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.demonwav.mcdev.platform.mcp.at.gen.psi.AtEntry
import com.demonwav.mcdev.util.MemberReference
import com.intellij.psi.PsiElement

object AtMemberReference {

    fun get(entry: AtEntry, member: PsiElement): MemberReference? {
        val memberText = member.text

        val owner = entry.className?.text ?: return null

        val pos = memberText.indexOf('(')
        return if (pos != -1) {
            MemberReference(memberText.substring(0, pos), memberText.substring(pos), owner)
        } else {
            MemberReference(memberText, null, owner)
        }
    }
}
