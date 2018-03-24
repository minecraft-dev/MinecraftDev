/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.config

import com.intellij.openapi.fileTypes.FileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

class MixinConfigFileTypeFactory : FileTypeFactory(), FileNameMatcher {

    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(MixinConfigFileType, this)
    }

    override fun getPresentableString() = "Mixin Configuration"
    override fun accept(fileName: String) = fileName.startsWith("mixins.") && fileName.endsWith(".json")
}
