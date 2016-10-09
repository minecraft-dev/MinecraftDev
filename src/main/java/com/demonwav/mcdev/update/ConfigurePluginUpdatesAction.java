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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;

public class ConfigurePluginUpdatesAction extends DumbAwareAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        new ConfigurePluginUpdatesDialog().show();
    }
}
