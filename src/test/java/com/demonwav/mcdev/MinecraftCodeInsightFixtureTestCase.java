/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev;

import com.demonwav.mcdev.platform.MinecraftModuleType;
import com.demonwav.mcdev.platform.bukkit.PaperModuleType;
import com.demonwav.mcdev.platform.bungeecord.BungeeCordModuleType;
import com.demonwav.mcdev.platform.canary.NeptuneModuleType;
import com.demonwav.mcdev.platform.forge.ForgeModuleType;
import com.demonwav.mcdev.platform.liteloader.LiteLoaderModuleType;
import com.demonwav.mcdev.platform.mcp.McpModuleType;
import com.demonwav.mcdev.platform.mixin.MixinModuleType;
import com.demonwav.mcdev.platform.sponge.SpongeModuleType;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;

public class MinecraftCodeInsightFixtureTestCase extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return new LightProjectDescriptor() {
            @Override
            protected void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
                // enable everything
                MinecraftModuleType.addOption(module, PaperModuleType.getInstance().getId());
                MinecraftModuleType.addOption(module, SpongeModuleType.getInstance().getId());
                MinecraftModuleType.addOption(module, ForgeModuleType.getInstance().getId());
                MinecraftModuleType.addOption(module, LiteLoaderModuleType.getInstance().getId());
                MinecraftModuleType.addOption(module, BungeeCordModuleType.getInstance().getId());
                MinecraftModuleType.addOption(module, MixinModuleType.getInstance().getId());
                MinecraftModuleType.addOption(module, McpModuleType.getInstance().getId());
                MinecraftModuleType.addOption(module, NeptuneModuleType.getInstance().getId());
            }
        };
    }
}
