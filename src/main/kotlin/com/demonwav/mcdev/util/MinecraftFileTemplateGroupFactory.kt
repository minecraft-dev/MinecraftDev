/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
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

        group.addTemplate(FileTemplateDescriptor(SPONGE_GRADLE_PROPERTIES_TEMPLATE, PlatformAssets.SPONGE_ICON))
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

        group.addTemplate(FileTemplateDescriptor(CANARY_MAIN_CLASS_TEMPLATE, PlatformAssets.CANARY_ICON))
        group.addTemplate(FileTemplateDescriptor(CANARY_INF_TEMPLATE, PlatformAssets.CANARY_ICON))
        group.addTemplate(FileTemplateDescriptor(CANARY_POM_TEMPLATE, PlatformAssets.CANARY_ICON))

        return group
    }

    companion object {

        val BUKKIT_MAIN_CLASS_TEMPLATE = "bukkit_main_class.java"
        val BUKKIT_PLUGIN_YML_TEMPLATE = "bukkit_plugin_description_file.yml"
        val BUKKIT_POM_TEMPLATE = "bukkit_pom_template.xml"

        val BUNGEECORD_MAIN_CLASS_TEMPLATE = "bungeecord_main_class.java"
        val BUNGEECORD_PLUGIN_YML_TEMPLATE = "bungeecord_plugin_description_file.yml"
        val BUNGEECORD_POM_TEMPLATE = "bungeecord_pom_template.xml"

        val SPONGE_GRADLE_PROPERTIES_TEMPLATE = "sponge_gradle.properties"
        val SPONGE_BUILD_GRADLE_TEMPLATE = "sponge_build.gradle"
        val SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "sponge_submodule_build.gradle"
        val SPONGE_MAIN_CLASS_TEMPLATE = "sponge_main_class.java"
        val SPONGE_POM_TEMPLATE = "sponge_pom_template.xml"

        val FORGE_GRADLE_PROPERTIES_TEMPLATE = "forge_gradle.properties"
        val FORGE_BUILD_GRADLE_TEMPLATE = "forge_build.gradle"
        val FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "forge_submodule_build.gradle"
        val FORGE_MAIN_CLASS_TEMPLATE = "forge_main_class.java"
        val MCMOD_INFO_TEMPLATE = "mcmod.info"

        val GRADLE_PROPERTIES_TEMPLATE = "gradle.properties"
        val MULTI_MODULE_BUILD_GRADLE_TEMPLATE = "multi_module_build.gradle"
        val BUILD_GRADLE_TEMPLATE = "build.gradle"
        val SETTINGS_GRADLE_TEMPLATE = "settings.gradle"
        val SUBMODULE_BUILD_GRADLE_TEMPLATE = "submodule_build.gradle"

        val LITELOADER_GRADLE_PROPERTIES_TEMPLATE = "liteloader_gradle.properties"
        val LITELOADER_BUILD_GRADLE_TEMPLATE = "liteloader_build.gradle"
        val LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE = "liteloader_submodule_build.gradle"
        val LITELOADER_MAIN_CLASS_TEMPLATE = "liteloader_main_class.java"

        val CANARY_MAIN_CLASS_TEMPLATE = "canary_main_class.java"
        val CANARY_INF_TEMPLATE = "canary_plugin_description_file.inf"
        val CANARY_POM_TEMPLATE = "canary_pom_template.xml"
    }
}
