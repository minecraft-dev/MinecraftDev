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

package com.demonwav.mcdev.util

import com.demonwav.mcdev.asset.GeneralAssets
import com.demonwav.mcdev.asset.PlatformAssets
import com.intellij.ide.fileTemplates.FileTemplateDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptor
import com.intellij.ide.fileTemplates.FileTemplateGroupDescriptorFactory

class MinecraftTemplates : FileTemplateGroupDescriptorFactory {

    override fun getFileTemplatesDescriptor(): FileTemplateGroupDescriptor {
        val group = FileTemplateGroupDescriptor("Minecraft", PlatformAssets.MINECRAFT_ICON)

        FileTemplateGroupDescriptor("Mixin", PlatformAssets.MIXIN_ICON).let { mixinGroup ->
            group.addTemplate(mixinGroup)
            mixinGroup.addTemplate(FileTemplateDescriptor(MIXIN_OVERWRITE_FALLBACK))
        }

        FileTemplateGroupDescriptor("NeoForge", PlatformAssets.NEOFORGE_ICON).let { forgeGroup ->
            group.addTemplate(forgeGroup)
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_BLOCK_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_ITEM_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_PACKET_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_ENCHANTMENT_TEMPLATE))
            forgeGroup.addTemplate(FileTemplateDescriptor(NEOFORGE_MOB_EFFECT_TEMPLATE))
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
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_1_21_11_BLOCK_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_1_21_11_ITEM_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_1_21_11_ENCHANTMENT_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_1_21_11_STATUS_EFFECT_TEMPLATE))

                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_26_1_BLOCK_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_26_1_ITEM_TEMPLATE))
                fabricSkeletonGroup.addTemplate(FileTemplateDescriptor(FABRIC_26_1_MOB_EFFECT_TEMPLATE))

            }

        }

        return group
    }

    companion object {
        const val MIXIN_OVERWRITE_FALLBACK = "Mixin Overwrite Fallback.java"

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

        const val FABRIC_1_21_11_BLOCK_TEMPLATE = "FabricBlock (1.21.11-).java"
        const val FABRIC_1_21_11_ITEM_TEMPLATE = "FabricItem (1.21.11-).java"
        const val FABRIC_1_21_11_ENCHANTMENT_TEMPLATE = "FabricEnchantment (1.21.11-).java"
        const val FABRIC_1_21_11_STATUS_EFFECT_TEMPLATE = "FabricStatusEffect (1.21.11-).java"

        const val FABRIC_26_1_BLOCK_TEMPLATE = "FabricBlock (26.1+).java"
        const val FABRIC_26_1_ITEM_TEMPLATE = "FabricItem (26.1+).java"
        const val FABRIC_26_1_MOB_EFFECT_TEMPLATE = "FabricMobEffect (26.1+).java"

        const val NEOFORGE_BLOCK_TEMPLATE = "NeoForgeBlock.java"
        const val NEOFORGE_ITEM_TEMPLATE = "NeoForgeItem.java"
        const val NEOFORGE_PACKET_TEMPLATE = "NeoForgePacket.java"
        const val NEOFORGE_ENCHANTMENT_TEMPLATE = "NeoForgeEnchantment.java"
        const val NEOFORGE_MOB_EFFECT_TEMPLATE = "NeoForgeMobEffect.java"
    }
}
