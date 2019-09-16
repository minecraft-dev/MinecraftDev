/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class MinecraftTemplates : FileTemplateGroupDescriptorFactory {

    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Minecraft", PlatformAssets.MINECRAFT_ICON)

        FileTemplateGroupDescriptor("Bukkit", PlatformAssets.BUKKIT_ICON).let { bukkitGroup ->
            group.addTemplate(bukkitGroup)
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_MAIN_CLASS_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_PLUGIN_YML_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_BUILD_GRADLE_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_SUBMODULE_BUILD_GRADLE_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_GRADLE_PROPERTIES_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_SETTINGS_GRADLE_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_POM_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_SUBMODULE_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("BungeeCord", PlatformAssets.BUNGEECORD_ICON).let { bungeeGroup ->
            group.addTemplate(bungeeGroup)
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_MAIN_CLASS_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_PLUGIN_YML_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_BUILD_GRADLE_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_SUBMODULE_BUILD_GRADLE_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_GRADLE_PROPERTIES_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_SETTINGS_GRADLE_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_POM_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_SUBMODULE_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Sponge", PlatformAssets.SPONGE_ICON).let { spongeGroup ->
            group.addTemplate(spongeGroup)
            spongeGroup.addTemplate(FileTemplateDescriptor(SPONGE_MAIN_CLASS_TEMPLATE))
            spongeGroup.addTemplate(FileTemplateDescriptor(SPONGE_BUILD_GRADLE_TEMPLATE))
            spongeGroup.addTemplate(FileTemplateDescriptor(SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE))
            spongeGroup.addTemplate(FileTemplateDescriptor(SPONGE_GRADLE_PROPERTIES_TEMPLATE))
            spongeGroup.addTemplate(FileTemplateDescriptor(SPONGE_SETTINGS_GRADLE_TEMPLATE))
            spongeGroup.addTemplate(FileTemplateDescriptor(SPONGE_POM_TEMPLATE))
            spongeGroup.addTemplate(FileTemplateDescriptor(SPONGE_SUBMODULE_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Forge", PlatformAssets.FORGE_ICON).let { forgeGroup ->
            group.addTemplate(forgeGroup)
            forgeGroup.addTemplate(FileTemplateDescriptor(FORGE_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FORGE_BUILD_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FORGE_GRADLE_PROPERTIES_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FORGE_SETTINGS_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_BUILD_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_SUBMODULE_BUILD_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_GRADLE_PROPERTIES_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_SETTINGS_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(MCMOD_INFO_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(MODS_TOML_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(PACK_MCMETA_TEMPLATE))
        }

        FileTemplateGroupDescriptor("LiteLoader", PlatformAssets.LITELOADER_ICON).let { liteGroup ->
            group.addTemplate(liteGroup)
            liteGroup.addTemplate(FileTemplateDescriptor(LITELOADER_MAIN_CLASS_TEMPLATE))
            liteGroup.addTemplate(FileTemplateDescriptor(LITELOADER_BUILD_GRADLE_TEMPLATE))
            liteGroup.addTemplate(FileTemplateDescriptor(LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE))
            liteGroup.addTemplate(FileTemplateDescriptor(LITELOADER_GRADLE_PROPERTIES_TEMPLATE))
            liteGroup.addTemplate(FileTemplateDescriptor(LITELOADER_SETTINGS_GRADLE_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Multi-Module", PlatformAssets.MINECRAFT_ICON).let { multiGroup ->
            group.addTemplate(multiGroup)
            multiGroup.addTemplate(FileTemplateDescriptor(MULTI_MODULE_BUILD_GRADLE_TEMPLATE))
            multiGroup.addTemplate(FileTemplateDescriptor(MULTI_MODULE_GRADLE_PROPERTIES_TEMPLATE))
            multiGroup.addTemplate(FileTemplateDescriptor(MULTI_MODULE_SETTINGS_GRADLE_TEMPLATE))
            multiGroup.addTemplate(FileTemplateDescriptor(MULTI_MODULE_POM_TEMPLATE))
            multiGroup.addTemplate(FileTemplateDescriptor(MULTI_MODULE_COMMON_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Mixin", PlatformAssets.MIXIN_ICON).let { mixinGroup ->
            group.addTemplate(mixinGroup)
            mixinGroup.addTemplate(FileTemplateDescriptor(MIXIN_OVERWRITE_FALLBACK))
        }

        return group
    }

    companion object {
        const val BUKKIT_MAIN_CLASS_TEMPLATE = "Bukkit Main Class.java"
        const val BUKKIT_PLUGIN_YML_TEMPLATE = "Bukkit plugin.yml"
        const val BUKKIT_BUILD_GRADLE_TEMPLATE = "Bukkit build.gradle"
        const val BUKKIT_SUBMODULE_BUILD_GRADLE_TEMPLATE = "Bukkit Submodule build.gradle"
        const val BUKKIT_GRADLE_PROPERTIES_TEMPLATE = "Bukkit gradle.properties"
        const val BUKKIT_SETTINGS_GRADLE_TEMPLATE = "Bukkit settings.gradle"
        const val BUKKIT_POM_TEMPLATE = "Bukkit pom.xml"
        const val BUKKIT_SUBMODULE_POM_TEMPLATE = "Bukkit Submodule pom.xml"

        const val BUNGEECORD_MAIN_CLASS_TEMPLATE = "BungeeCord Main Class.java"
        const val BUNGEECORD_PLUGIN_YML_TEMPLATE = "BungeeCord bungee.yml"
        const val BUNGEECORD_BUILD_GRADLE_TEMPLATE = "BungeeCord build.gradle"
        const val BUNGEECORD_SUBMODULE_BUILD_GRADLE_TEMPLATE = "BungeeCord Submodule build.gradle"
        const val BUNGEECORD_GRADLE_PROPERTIES_TEMPLATE = "BungeeCord gradle.properties"
        const val BUNGEECORD_SETTINGS_GRADLE_TEMPLATE = "BungeeCord settings.gradle"
        const val BUNGEECORD_POM_TEMPLATE = "BungeeCord pom.xml"
        const val BUNGEECORD_SUBMODULE_POM_TEMPLATE = "BungeeCord Submodule pom.xml"

        const val SPONGE_MAIN_CLASS_TEMPLATE = "Sponge Main Class.java"
        const val SPONGE_BUILD_GRADLE_TEMPLATE = "Sponge build.gradle"
        const val SPONGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "Sponge Submodule build.gradle"
        const val SPONGE_GRADLE_PROPERTIES_TEMPLATE = "Sponge gradle.properties"
        const val SPONGE_SETTINGS_GRADLE_TEMPLATE = "Sponge settings.gradle"
        const val SPONGE_POM_TEMPLATE = "Sponge pom.xml"
        const val SPONGE_SUBMODULE_POM_TEMPLATE = "Sponge Submodule pom.xml"

        const val FORGE_MAIN_CLASS_TEMPLATE = "Forge Main Class.java"
        const val FORGE_BUILD_GRADLE_TEMPLATE = "Forge build.gradle"
        const val FORGE_SUBMODULE_BUILD_GRADLE_TEMPLATE = "Forge Submodule build.gradle"
        const val FORGE_GRADLE_PROPERTIES_TEMPLATE = "Forge gradle.properties"
        const val FORGE_SETTINGS_GRADLE_TEMPLATE = "Forge settings.gradle"
        const val FG3_MAIN_CLASS_TEMPLATE = "Forge (1.13+) Main Class.java"
        const val FG3_BUILD_GRADLE_TEMPLATE = "Forge (1.13+) build.gradle"
        const val FG3_SUBMODULE_BUILD_GRADLE_TEMPLATE = "Forge (1.13+) Submodule build.gradle"
        const val FG3_GRADLE_PROPERTIES_TEMPLATE = "Forge (1.13+) gradle.properties"
        const val FG3_SETTINGS_GRADLE_TEMPLATE = "Forge (1.13+) settings.gradle"
        const val MCMOD_INFO_TEMPLATE = "mcmod.info"
        const val MODS_TOML_TEMPLATE = "mods.toml"
        const val PACK_MCMETA_TEMPLATE = "pack.mcmeta"

        const val LITELOADER_MAIN_CLASS_TEMPLATE = "LiteLoader Main Class.java"
        const val LITELOADER_BUILD_GRADLE_TEMPLATE = "LiteLoader build.gradle"
        const val LITELOADER_SUBMODULE_BUILD_GRADLE_TEMPLATE = "LiteLoader Submodule build.gradle"
        const val LITELOADER_GRADLE_PROPERTIES_TEMPLATE = "LiteLoader gradle.properties"
        const val LITELOADER_SETTINGS_GRADLE_TEMPLATE = "LiteLoader settings.gradle"

        const val MULTI_MODULE_BUILD_GRADLE_TEMPLATE = "Multi-Module Base build.gradle"
        const val MULTI_MODULE_GRADLE_PROPERTIES_TEMPLATE = "Multi-Module Base gradle.properties"
        const val MULTI_MODULE_SETTINGS_GRADLE_TEMPLATE = "Multi-Module Base settings.gradle"
        const val MULTI_MODULE_POM_TEMPLATE = "Multi-Module Base pom.xml"
        const val MULTI_MODULE_COMMON_POM_TEMPLATE = "Multi-Module Common pom.xml"

        const val MIXIN_OVERWRITE_FALLBACK = "Mixin Overwrite Fallback.java"
    }
}
