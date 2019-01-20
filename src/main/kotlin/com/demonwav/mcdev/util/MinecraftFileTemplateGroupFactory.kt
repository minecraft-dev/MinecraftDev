/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.asset.PlatformAssets

import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class MinecraftFileTemplateGroupFactory : FileTemplateGroupDescriptorFactory {

    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Minecraft", PlatformAssets.MINECRAFT_ICON)

        group.addTemplate(FileTemplateDescriptor(BUKKIT_MAIN_CLASS_TEMPLATE, PlatformAssets.BUKKIT_ICON))
        group.addTemplate(FileTemplateDescriptor(BUKKIT_PLUGIN_YML_TEMPLATE, PlatformAssets.BUKKIT_ICON))
        group.addTemplate(FileTemplateDescriptor(BUKKIT_POM_TEMPLATE, PlatformAssets.BUKKIT_ICON))

        group.addTemplate(FileTemplateDescriptor(BUNGEECORD_MAIN_CLASS_TEMPLATE, PlatformAssets.BUNGEECORD_ICON))
        group.addTemplate(FileTemplateDescriptor(BUNGEECORD_PLUGIN_YML_TEMPLATE, PlatformAssets.BUNGEECORD_ICON))
        group.addTemplate(FileTemplateDescriptor(BUNGEECORD_POM_TEMPLATE, PlatformAssets.BUNGEECORD_ICON))

        group.addTemplate(FileTemplateDescriptor(SPONGE_BUILD_GRADLE_TEMPLATE, PlatformAssets.SPONGE_ICON))
        group.addTemplate(FileTemplateDescriptor(SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.SPONGE_ICON))
        group.addTemplate(FileTemplateDescriptor(SPONGE_MAIN_CLASS_TEMPLATE, PlatformAssets.SPONGE_ICON))
        group.addTemplate(FileTemplateDescriptor(SPONGE_POM_TEMPLATE, PlatformAssets.SPONGE_ICON))

        group.addTemplate(FileTemplateDescriptor(FORGE_GRADLE_PROPERTIES_TEMPLATE, PlatformAssets.FORGE_ICON))
        group.addTemplate(FileTemplateDescriptor(FORGE_BUILD_GRADLE_TEMPLATE, PlatformAssets.FORGE_ICON))
        group.addTemplate(FileTemplateDescriptor(FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.FORGE_ICON))
        group.addTemplate(FileTemplateDescriptor(FORGE_MAIN_CLASS_TEMPLATE, PlatformAssets.FORGE_ICON))
        group.addTemplate(FileTemplateDescriptor(MCMOD_INFO_TEMPLATE, PlatformAssets.FORGE_ICON))

        group.addTemplate(FileTemplateDescriptor(GRADLE_PROPERTIES_TEMPLATE, PlatformAssets.MINECRAFT_ICON))
        group.addTemplate(FileTemplateDescriptor(BUILD_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON))
        group.addTemplate(FileTemplateDescriptor(MULTI_MODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON))
        group.addTemplate(FileTemplateDescriptor(SETTINGS_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON))
        group.addTemplate(FileTemplateDescriptor(SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.MINECRAFT_ICON))

        group.addTemplate(FileTemplateDescriptor(LITELOADER_GRADLE_PROPERTIES_TEMPLATE, PlatformAssets.LITELOADER_ICON))
        group.addTemplate(FileTemplateDescriptor(LITELOADER_BUILD_GRADLE_TEMPLATE, PlatformAssets.LITELOADER_ICON))
        group.addTemplate(FileTemplateDescriptor(LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE, PlatformAssets.LITELOADER_ICON))
        group.addTemplate(FileTemplateDescriptor(LITELOADER_MAIN_CLASS_TEMPLATE, PlatformAssets.LITELOADER_ICON))

        group.addTemplate(FileTemplateDescriptor(MIXIN_OVERWRITE_FALLBACK, PlatformAssets.MIXIN_ICON))

        return group
    }

    companion object {
        const val BUKKIT_MAIN_CLASS_TEMPLATE = "bukkit_main_class.java"
        const val BUKKIT_PLUGIN_YML_TEMPLATE = "bukkit_plugin_description_file.yml"
        const val BUKKIT_POM_TEMPLATE = "bukkit_pom_template.xml"

        const val BUNGEECORD_MAIN_CLASS_TEMPLATE = "bungeecord_main_class.java"
        const val BUNGEECORD_PLUGIN_YML_TEMPLATE = "bungeecord_plugin_description_file.yml"
        const val BUNGEECORD_POM_TEMPLATE = "bungeecord_pom_template.xml"

        const val SPONGE_BUILD_GRADLE_TEMPLATE = "sponge_build.gradle"
        const val SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "sponge_submodule_build.gradle"
        const val SPONGE_MAIN_CLASS_TEMPLATE = "sponge_main_class.java"
        const val SPONGE_POM_TEMPLATE = "sponge_pom_template.xml"

        const val FORGE_GRADLE_PROPERTIES_TEMPLATE = "forge_gradle.properties"
        const val FORGE_BUILD_GRADLE_TEMPLATE = "forge_build.gradle"
        const val FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "forge_submodule_build.gradle"
        const val FORGE_MAIN_CLASS_TEMPLATE = "forge_main_class.java"
        const val MCMOD_INFO_TEMPLATE = "mcmod.info"

        const val GRADLE_PROPERTIES_TEMPLATE = "gradle.properties"
        const val MULTI_MODULE_BUILD_GRADLE_TEMPLATE = "multi_module_build.gradle"
        const val BUILD_GRADLE_TEMPLATE = "build.gradle"
        const val SETTINGS_GRADLE_TEMPLATE = "settings.gradle"
        const val SUBMODULE_BUILD_GRADLE_TEMPLATE = "submodule_build.gradle"

        const val LITELOADER_GRADLE_PROPERTIES_TEMPLATE = "liteloader_gradle.properties"
        const val LITELOADER_BUILD_GRADLE_TEMPLATE = "liteloader_build.gradle"
        const val LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE = "liteloader_submodule_build.gradle"
        const val LITELOADER_MAIN_CLASS_TEMPLATE = "liteloader_main_class.java"

        const val MIXIN_OVERWRITE_FALLBACK = "Mixin Overwrite Fallback.java"
    }

}
