/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
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
