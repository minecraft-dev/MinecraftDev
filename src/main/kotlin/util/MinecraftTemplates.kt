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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.asset.GeneralAssets
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
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_GRADLE_PROPERTIES_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_SETTINGS_GRADLE_TEMPLATE))
            bukkitGroup.addTemplate(FileTemplateDescriptor(BUKKIT_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Paper", PlatformAssets.PAPER_ICON).let { paperGroup ->
            group.addTemplate(paperGroup)
            paperGroup.addTemplate(FileTemplateDescriptor(PAPER_PLUGIN_YML_TEMPLATE))
        }

        FileTemplateGroupDescriptor("BungeeCord", PlatformAssets.BUNGEECORD_ICON).let { bungeeGroup ->
            group.addTemplate(bungeeGroup)
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_MAIN_CLASS_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_PLUGIN_YML_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_BUILD_GRADLE_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_GRADLE_PROPERTIES_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_SETTINGS_GRADLE_TEMPLATE))
            bungeeGroup.addTemplate(FileTemplateDescriptor(BUNGEECORD_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Velocity", PlatformAssets.VELOCITY_ICON).let { velocityGroup ->
            group.addTemplate(velocityGroup)
            velocityGroup.addTemplate(FileTemplateDescriptor(VELOCITY_MAIN_CLASS_TEMPLATE))
            velocityGroup.addTemplate(FileTemplateDescriptor(VELOCITY_BUILD_CONSTANTS_TEMPLATE))
            velocityGroup.addTemplate(FileTemplateDescriptor(VELOCITY_MAIN_CLASS_V2_TEMPLATE))
            velocityGroup.addTemplate(FileTemplateDescriptor(VELOCITY_BUILD_GRADLE_TEMPLATE))
            velocityGroup.addTemplate(FileTemplateDescriptor(VELOCITY_GRADLE_PROPERTIES_TEMPLATE))
            velocityGroup.addTemplate(FileTemplateDescriptor(VELOCITY_SETTINGS_GRADLE_TEMPLATE))
            velocityGroup.addTemplate(FileTemplateDescriptor(VELOCITY_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Sponge", PlatformAssets.SPONGE_ICON).let { spongeGroup ->
            group.addTemplate(spongeGroup)
            fun sponge8Template(fileName: String) = template(fileName, fileName.replace("8+ ", ""))
            spongeGroup.addTemplate(sponge8Template(SPONGE8_MAIN_CLASS_TEMPLATE))
            spongeGroup.addTemplate(sponge8Template(SPONGE8_PLUGINS_JSON_TEMPLATE))
            spongeGroup.addTemplate(sponge8Template(SPONGE8_BUILD_GRADLE_TEMPLATE))
            spongeGroup.addTemplate(sponge8Template(SPONGE8_GRADLE_PROPERTIES_TEMPLATE))
            spongeGroup.addTemplate(sponge8Template(SPONGE8_SETTINGS_GRADLE_TEMPLATE))
            spongeGroup.addTemplate(template(SPONGE_POM_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Forge", PlatformAssets.FORGE_ICON).let { forgeGroup ->
            group.addTemplate(forgeGroup)
            forgeGroup.addTemplate(FileTemplateDescriptor(FORGE_MIXINS_JSON_TEMPLATE, PlatformAssets.FORGE_ICON))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_1_16_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_1_17_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_1_18_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_1_19_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_1_19_3_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_1_20_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_1_20_CONFIG_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_BUILD_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_GRADLE_PROPERTIES_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(FG3_SETTINGS_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(MODS_TOML_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(PACK_MCMETA_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Fabric", PlatformAssets.FABRIC_ICON).let { fabricGroup ->
            group.addTemplate(fabricGroup)
            fabricGroup.addTemplate(FileTemplateDescriptor(FABRIC_BUILD_GRADLE_TEMPLATE, PlatformAssets.FABRIC_ICON))
            fabricGroup.addTemplate(
                FileTemplateDescriptor(FABRIC_GRADLE_PROPERTIES_TEMPLATE, PlatformAssets.FABRIC_ICON),
            )
            fabricGroup.addTemplate(FileTemplateDescriptor(FABRIC_MIXINS_JSON_TEMPLATE, PlatformAssets.FABRIC_ICON))
            fabricGroup.addTemplate(FileTemplateDescriptor(FABRIC_MOD_JSON_TEMPLATE, PlatformAssets.FABRIC_ICON))
            fabricGroup.addTemplate(FileTemplateDescriptor(FABRIC_SETTINGS_GRADLE_TEMPLATE, PlatformAssets.FABRIC_ICON))
        }

        FileTemplateGroupDescriptor("Mixin", PlatformAssets.MIXIN_ICON).let { mixinGroup ->
            group.addTemplate(mixinGroup)
            mixinGroup.addTemplate(FileTemplateDescriptor(MIXIN_OVERWRITE_FALLBACK))
        }

        FileTemplateGroupDescriptor("NeoForge", PlatformAssets.NEOFORGE_ICON).let { forgeGroup ->
            group.addTemplate(forgeGroup)
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_MIXINS_JSON_TEMPLATE, PlatformAssets.NEOFORGE_ICON))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_MAIN_CLASS_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_CONFIG_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_BUILD_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_GRADLE_PROPERTIES_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_SETTINGS_GRADLE_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_MODS_TOML_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_PACK_MCMETA_TEMPLATE))

            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_BLOCK_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_ITEM_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_PACKET_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_ENCHANTMENT_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_MOB_EFFECT_TEMPLATE))
        }

        FileTemplateGroupDescriptor("Common", PlatformAssets.MINECRAFT_ICON).let { commonGroup ->
            group.addTemplate(commonGroup)
            commonGroup.addTemplate(FileTemplateDescriptor(GRADLE_GITIGNORE_TEMPLATE))
            commonGroup.addTemplate(FileTemplateDescriptor(MAVEN_GITIGNORE_TEMPLATE))
            commonGroup.addTemplate(FileTemplateDescriptor(GRADLE_WRAPPER_PROPERTIES))
        }

        FileTemplateGroupDescriptor("Skeletons", GeneralAssets.MC_TEMPLATE).let { skeletonGroup ->
            group.addTemplate(skeletonGroup)
            FileTemplateGroupDescriptor("Forge", PlatformAssets.FORGE_ICON).let { forgeSkeletonGroup ->
                skeletonGroup.addTemplate(forgeSkeletonGroup)
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_ENCHANTMENT_TEMPLATE))
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_PACKET_TEMPLATE))
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_BLOCK_TEMPLATE))
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_ITEM_TEMPLATE))
            }
            FileTemplateGroupDescriptor("Forge 1.17+", PlatformAssets.FORGE_ICON).let { forgeSkeletonGroup ->
                skeletonGroup.addTemplate(forgeSkeletonGroup)
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_1_17_ENCHANTMENT_TEMPLATE))
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_1_17_PACKET_TEMPLATE))
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_1_17_BLOCK_TEMPLATE))
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_1_17_ITEM_TEMPLATE))
            }
            FileTemplateGroupDescriptor("Forge 1.18+", PlatformAssets.FORGE_ICON).let { forgeSkeletonGroup ->
                skeletonGroup.addTemplate(forgeSkeletonGroup)
                forgeSkeletonGroup.addTemplate(FileTemplateDescriptor(FORGE_1_18_PACKET_TEMPLATE))
            }
            FileTemplateGroupDescriptor("Fabric", PlatformAssets.FABRIC_ICON).let { fabricSkeletonGroup ->
                skeletonGroup.addTemplate(fabricSkeletonGroup)
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_BLOCK_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_ITEM_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_ENCHANTMENT_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_STATUS_EFFECT_TEMPLATE))
            }
        }

        FileTemplateGroupDescriptor("Licenses", null).let { licenseGroup ->
            group.addTemplate(licenseGroup)
            enumValues<License>().forEach { license ->
                licenseGroup.addTemplate(FileTemplateDescriptor(license.id))
            }
        }

        return group
    }

    companion object {
        const val BUKKIT_MAIN_CLASS_TEMPLATE = "Bukkit Main Class.java"
        const val BUKKIT_PLUGIN_YML_TEMPLATE = "Bukkit plugin.yml"
        const val BUKKIT_BUILD_GRADLE_TEMPLATE = "Bukkit build.gradle"
        const val BUKKIT_GRADLE_PROPERTIES_TEMPLATE = "Bukkit gradle.properties"
        const val BUKKIT_SETTINGS_GRADLE_TEMPLATE = "Bukkit settings.gradle"
        const val BUKKIT_POM_TEMPLATE = "Bukkit pom.xml"

        const val PAPER_PLUGIN_YML_TEMPLATE = "Paper paper-plugin.yml"

        const val BUNGEECORD_MAIN_CLASS_TEMPLATE = "BungeeCord Main Class.java"
        const val BUNGEECORD_PLUGIN_YML_TEMPLATE = "BungeeCord bungee.yml"
        const val BUNGEECORD_BUILD_GRADLE_TEMPLATE = "BungeeCord build.gradle"
        const val BUNGEECORD_GRADLE_PROPERTIES_TEMPLATE = "BungeeCord gradle.properties"
        const val BUNGEECORD_SETTINGS_GRADLE_TEMPLATE = "BungeeCord settings.gradle"
        const val BUNGEECORD_POM_TEMPLATE = "BungeeCord pom.xml"

        const val VELOCITY_MAIN_CLASS_TEMPLATE = "Velocity Main Class.java"
        const val VELOCITY_MAIN_CLASS_V2_TEMPLATE = "Velocity Main Class V2.java"
        const val VELOCITY_BUILD_CONSTANTS_TEMPLATE = "Velocity Build Constants.java"
        const val VELOCITY_BUILD_GRADLE_TEMPLATE = "Velocity build.gradle"
        const val VELOCITY_GRADLE_PROPERTIES_TEMPLATE = "Velocity gradle.properties"
        const val VELOCITY_SETTINGS_GRADLE_TEMPLATE = "Velocity settings.gradle"
        const val VELOCITY_POM_TEMPLATE = "Velocity pom.xml"

        const val SPONGE_POM_TEMPLATE = "Sponge pom.xml"
        const val SPONGE8_MAIN_CLASS_TEMPLATE = "Sponge 8+ Main Class.java"
        const val SPONGE8_PLUGINS_JSON_TEMPLATE = "Sponge 8+ sponge_plugins.json"
        const val SPONGE8_BUILD_GRADLE_TEMPLATE = "Sponge 8+ build.gradle.kts"
        const val SPONGE8_GRADLE_PROPERTIES_TEMPLATE = "Sponge 8+ gradle.properties"
        const val SPONGE8_SETTINGS_GRADLE_TEMPLATE = "Sponge 8+ settings.gradle.kts"

        const val FORGE_MIXINS_JSON_TEMPLATE = "Forge Mixins Config.json"
        const val FG3_1_16_MAIN_CLASS_TEMPLATE = "Forge (1.16+) Main Class.java"
        const val FG3_1_17_MAIN_CLASS_TEMPLATE = "Forge (1.17+) Main Class.java"
        const val FG3_1_18_MAIN_CLASS_TEMPLATE = "Forge (1.18+) Main Class.java"
        const val FG3_1_19_MAIN_CLASS_TEMPLATE = "Forge (1.19+) Main Class.java"
        const val FG3_1_19_3_MAIN_CLASS_TEMPLATE = "Forge (1.19.3+) Main Class.java"
        const val FG3_1_20_MAIN_CLASS_TEMPLATE = "Forge (1.20+) Main Class.java"
        const val FG3_1_20_CONFIG_TEMPLATE = "Forge (1.20+) Config.java"
        const val FG3_BUILD_GRADLE_TEMPLATE = "Forge (1.13+) build.gradle"
        const val FG3_GRADLE_PROPERTIES_TEMPLATE = "Forge (1.13+) gradle.properties"
        const val FG3_SETTINGS_GRADLE_TEMPLATE = "Forge (1.13+) settings.gradle"
        const val MODS_TOML_TEMPLATE = "mods.toml"
        const val PACK_MCMETA_TEMPLATE = "pack.mcmeta"

        const val FABRIC_BUILD_GRADLE_TEMPLATE = "fabric_build.gradle"
        const val FABRIC_GRADLE_PROPERTIES_TEMPLATE = "fabric_gradle.properties"
        const val FABRIC_MIXINS_JSON_TEMPLATE = "fabric_mixins.json"
        const val FABRIC_MOD_JSON_TEMPLATE = "fabric_mod.json"
        const val FABRIC_SETTINGS_GRADLE_TEMPLATE = "fabric_settings.gradle"

        const val ARCHITECTURY_BUILD_GRADLE_TEMPLATE = "architectury_build.gradle"
        const val ARCHITECTURY_GRADLE_PROPERTIES_TEMPLATE = "architectury_gradle.properties"
        const val ARCHITECTURY_SETTINGS_GRADLE_TEMPLATE = "architectury_settings.gradle"
        const val ARCHITECTURY_COMMON_BUILD_GRADLE_TEMPLATE = "architectury_common_build.gradle"
        const val ARCHITECTURY_COMMON_MAIN_CLASS_TEMPLATE = "architectury_common_main_class.java"
        const val ARCHITECTURY_COMMON_MIXINS_JSON_TEMPLATE = "architectury_common_mixins.json"
        const val ARCHITECTURY_FABRIC_BUILD_GRADLE_TEMPLATE = "architectury_fabric_build.gradle"
        const val ARCHITECTURY_FABRIC_MAIN_CLASS_TEMPLATE = "architectury_fabric_main_class.java"
        const val ARCHITECTURY_FABRIC_MIXINS_JSON_TEMPLATE = "architectury_fabric_mixins.json"
        const val ARCHITECTURY_FABRIC_MOD_JSON_TEMPLATE = "architectury_fabric_mod.json"
        const val ARCHITECTURY_FORGE_BUILD_GRADLE_TEMPLATE = "architectury_forge_build.gradle"
        const val ARCHITECTURY_FORGE_GRADLE_PROPERTIES_TEMPLATE = "architectury_forge_gradle.properties"
        const val ARCHITECTURY_FORGE_MAIN_CLASS_TEMPLATE = "architectury_forge_main_class.java"
        const val ARCHITECTURY_FORGE_MIXINS_JSON_TEMPLATE = "architectury_forge_mixins.json"
        const val ARCHITECTURY_FORGE_MODS_TOML_TEMPLATE = "architectury_forge_mods.toml"
        const val ARCHITECTURY_FORGE_PACK_MCMETA_TEMPLATE = "architectury_forge_pack.mcmeta"

        const val MIXIN_OVERWRITE_FALLBACK = "Mixin Overwrite Fallback.java"

        const val GRADLE_WRAPPER_PROPERTIES = "MinecraftDev gradle-wrapper.properties"
        const val GRADLE_GITIGNORE_TEMPLATE = "Gradle.gitignore"
        const val MAVEN_GITIGNORE_TEMPLATE = "Maven.gitignore"

        const val FORGE_BLOCK_TEMPLATE = "ForgeBlock.java"
        const val FORGE_ITEM_TEMPLATE = "ForgeItem.java"
        const val FORGE_PACKET_TEMPLATE = "ForgePacket.java"
        const val FORGE_ENCHANTMENT_TEMPLATE = "ForgeEnchantment.java"

        const val FORGE_1_17_BLOCK_TEMPLATE = "ForgeBlock (1.17+).java"
        const val FORGE_1_17_ITEM_TEMPLATE = "ForgeItem (1.17+).java"
        const val FORGE_1_17_PACKET_TEMPLATE = "ForgePacket (1.17+).java"
        const val FORGE_1_17_ENCHANTMENT_TEMPLATE = "ForgeEnchantment (1.17+).java"
        const val FORGE_1_17_MOB_EFFECT_TEMPLATE = "ForgeMobEffect (1.17+).java"

        const val FORGE_1_18_PACKET_TEMPLATE = "ForgePacket (1.18+).java"

        const val FABRIC_BLOCK_TEMPLATE = "FabricBlock.java"
        const val FABRIC_ITEM_TEMPLATE = "FabricItem.java"
        const val FABRIC_ENCHANTMENT_TEMPLATE = "FabricEnchantment.java"
        const val FABRIC_STATUS_EFFECT_TEMPLATE = "FabricStatusEffect.java"

        const val NEOFORGE_MIXINS_JSON_TEMPLATE = "NeoForge Mixins Config.json"
        const val NEOFORGE_MAIN_CLASS_TEMPLATE = "NeoForge Main Class.java"
        const val NEOFORGE_CONFIG_TEMPLATE = "NeoForge Config.java"
        const val NEOFORGE_BUILD_GRADLE_TEMPLATE = "NeoForge build.gradle"
        const val NEOFORGE_GRADLE_PROPERTIES_TEMPLATE = "NeoForge gradle.properties"
        const val NEOFORGE_SETTINGS_GRADLE_TEMPLATE = "NeoForge settings.gradle"
        const val NEOFORGE_MODS_TOML_TEMPLATE = "NeoForge mods.toml"
        const val NEOFORGE_PACK_MCMETA_TEMPLATE = "NeoForge pack.mcmeta"

        const val NEOFORGE_BLOCK_TEMPLATE = "NeoForgeBlock.java"
        const val NEOFORGE_ITEM_TEMPLATE = "NeoForgeItem.java"
        const val NEOFORGE_PACKET_TEMPLATE = "NeoForgePacket.java"
        const val NEOFORGE_ENCHANTMENT_TEMPLATE = "NeoForgeEnchantment.java"
        const val NEOFORGE_MOB_EFFECT_TEMPLATE = "NeoForgeMobEffect.java"
    }

    private fun template(fileName: String, displayName: String? = null) = CustomDescriptor(fileName, displayName)

    private class CustomDescriptor(fileName: String, val visibleName: String?) : FileTemplateDescriptor(fileName) {
        override fun getDisplayName(): String = visibleName ?: fileName
    }
}
