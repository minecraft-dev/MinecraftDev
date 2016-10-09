/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.update;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

import java.util.Arrays;

public class PluginUtil {

    public static final PluginId PLUGIN_ID = PluginId.getId("com.demonwav.minecraft-dev");

    public static String getPluginVersion() {
        IdeaPluginDescriptor plugin = PluginManager.getPlugin(PLUGIN_ID);
        assert plugin != null : "Minecraft Development plugin not found: " + Arrays.toString(PluginManagerCore.getPlugins());
        return plugin.getVersion();
    }
}
