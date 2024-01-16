/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
