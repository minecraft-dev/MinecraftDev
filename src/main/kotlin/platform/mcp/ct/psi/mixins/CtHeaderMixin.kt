/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.ct.psi.mixins

import com.demonwav.mcdev.platform.mcp.ct.psi.CtElement
import com.intellij.psi.PsiElement

interface CtHeaderMixin : CtElement {

    val nameElement: PsiElement
    val nameString: String
    val versionElement: PsiElement?
    val versionString: String?
    val namespaceElement: PsiElement?
    val namespaceString: String?
    // Effective version, AW is 1 and 2; CT starts with 3, so CT v1 has effective version 3.
    val effectiveVersion: Int?
}
