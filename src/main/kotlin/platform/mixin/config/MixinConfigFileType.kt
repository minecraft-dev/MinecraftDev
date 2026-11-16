/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.json.JsonLanguage
import com.intellij.json.json5.Json5Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.ex.FileTypeIdentifiableByVirtualFile
import com.intellij.openapi.vfs.VirtualFile

interface MixinConfigFileType : FileTypeIdentifiableByVirtualFile {
    fun getFilenameRegex(): Regex

    // Dynamic file type detection is sadly needed as we're overriding the built-in json file type.
    // Simply using an extension pattern is not sufficient as there is no way to bump the version to tell
    // the cache that the pattern has changed, as it now has, without changing the file type name.
    // See https://www.plugin-dev.com/intellij/custom-language/file-type-detection/#guidelines
    override fun isMyFileType(file: VirtualFile) = file.name.contains(getFilenameRegex())

    override fun getDefaultExtension() = ""
    override fun getIcon() = PlatformAssets.MIXIN_ICON

    object Json : LanguageFileType(JsonLanguage.INSTANCE), MixinConfigFileType {
        private val filenameRegex = "(^|\\.)mixins?(\\.[^.]+)*\\.json$".toRegex()

        override fun getFilenameRegex(): Regex = filenameRegex
        override fun getName() = "Mixin Json Configuration"
        override fun getDescription() = MCDevBundle("mixin.config.file_type.description")
    }

    object Json5 : LanguageFileType(Json5Language.INSTANCE), MixinConfigFileType {
        private var filenameRegex = "(^|\\.)mixins?(\\.[^.]+)*\\.json5$".toRegex()

        override fun getFilenameRegex(): Regex = filenameRegex
        override fun getName() = "Mixin Json5 Configuration"
        override fun getDescription() = MCDevBundle("mixin.config.file_type.description.json5")
    }
}
