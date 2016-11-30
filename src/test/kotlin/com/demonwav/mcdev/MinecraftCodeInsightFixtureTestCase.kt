package com.demonwav.mcdev

import com.demonwav.mcdev.platform.MinecraftModuleType
import com.demonwav.mcdev.platform.bukkit.PaperModuleType
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType
import com.demonwav.mcdev.platform.canary.NeptuneModuleType
import com.demonwav.mcdev.platform.forge.ForgeModuleType
import com.demonwav.mcdev.platform.liteloader.LiteLoaderModuleType
import com.demonwav.mcdev.platform.mcp.McpModuleType
import com.demonwav.mcdev.platform.mixin.MixinModuleType
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase

abstract class MinecraftCodeInsightFixtureTestCase : LightCodeInsightFixtureTestCase() {
    override fun getProjectDescriptor(): LightProjectDescriptor {
        return object : LightProjectDescriptor() {
            override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
                super.configureModule(module, model, contentEntry)
                // enable everything
                MinecraftModuleType.addOption(module, PaperModuleType.getInstance().id)
                MinecraftModuleType.addOption(module, SpongeModuleType.getInstance().id)
                MinecraftModuleType.addOption(module, ForgeModuleType.getInstance().id)
                MinecraftModuleType.addOption(module, LiteLoaderModuleType.getInstance().id)
                MinecraftModuleType.addOption(module, BungeeCordModuleType.getInstance().id)
                MinecraftModuleType.addOption(module, MixinModuleType.getInstance().id)
                MinecraftModuleType.addOption(module, McpModuleType.getInstance().id)
                MinecraftModuleType.addOption(module, NeptuneModuleType.getInstance().id)
            }
        }
    }
}