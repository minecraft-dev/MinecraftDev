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

package com.demonwav.mcdev.platform.mixin.config

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.json.JsonLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile

object MixinConfigFileType : LanguageFileType(JsonLanguage.INSTANCE), FileTypeIdentifiableByVirtualFile {

    private val filenameRegex = "(^|\\.)mixins?(\\.[^.]+)*\\.json\$".toRegex()

    // Dynamic file type detection is sadly needed as we're overriding the built-in json file type.
    // Simply using an extension pattern is not sufficient as there is no way to bump the version to tell
    // the cache that the pattern has changed, as it now has, without changing the file type name.
    // See https://www.plugin-dev.com/intellij/custom-language/file-type-detection/#guidelines
    override fun isMyFileType(file: VirtualFile) = file.name.contains(filenameRegex)

    override fun getName() = "Mixin Configuration"
    override fun getDescription() = "Mixin configuration"
    override fun getDefaultExtension() = ""
    override fun getIcon() = PlatformAssets.MIXIN_ICON
}
