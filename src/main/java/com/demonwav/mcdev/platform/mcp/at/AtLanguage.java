/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at;

import com.intellij.lang.Language;

public class AtLanguage extends Language{
    private static final AtLanguage instance = new AtLanguage();

    public static AtLanguage getInstance() {
        return instance;
    }

    private AtLanguage() {
        super("Access Transformers");
    }
}
