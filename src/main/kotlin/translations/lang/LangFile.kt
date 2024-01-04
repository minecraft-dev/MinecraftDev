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

package com.demonwav.mcdev.translations.lang

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.psi.FileViewProvider

class LangFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, MCLangLanguage) {
    override fun getFileType() = LangFileType
    override fun getIcon(flags: Int) = PlatformAssets.MINECRAFT_ICON
}
