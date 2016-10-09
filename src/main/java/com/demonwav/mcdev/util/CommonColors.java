/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 Kyle Wood (DemonWav)
 *
 * MIT License
 */
package com.demonwav.mcdev.util;

import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Map;

public final class CommonColors {

    @NotNull public static final Color DARK_RED = new Color(0xAA0000);
    @NotNull public static final Color RED = new Color(0xFF5555);
    @NotNull public static final Color GOLD = new Color(0xFFAA00);
    @NotNull public static final Color YELLOW = new Color(0xFFFF55);
    @NotNull public static final Color DARK_GREEN = new Color(0x00AA00);
    @NotNull public static final Color GREEN = new Color(0x55FF55);
    @NotNull public static final Color AQUA = new Color(0x55FFFF);
    @NotNull public static final Color DARK_AQUA = new Color(0x00AAAA);
    @NotNull public static final Color DARK_BLUE = new Color(0x0000AA);
    @NotNull public static final Color BLUE = new Color(0x5555FF);
    @NotNull public static final Color LIGHT_PURPLE = new Color(0xFF55FF);
    @NotNull public static final Color DARK_PURPLE = new Color(0xAA00AA);
    @NotNull public static final Color WHITE = new Color(0xFFFFFF);
    @NotNull public static final Color GRAY = new Color(0xAAAAAA);
    @NotNull public static final Color DARK_GRAY = new Color(0x555555);
    @NotNull public static final Color BLACK = new Color(0x000000);

    private CommonColors() {
    }

    public static void applyStandardColors(@NotNull Map<String, Color> map, @NotNull String prefix) {
        map.put(prefix + ".DARK_RED", DARK_RED);
        map.put(prefix + ".RED", RED);
        map.put(prefix + ".GOLD", GOLD);
        map.put(prefix + ".YELLOW", YELLOW);
        map.put(prefix + ".DARK_GREEN", DARK_GREEN);
        map.put(prefix + ".GREEN", GREEN);
        map.put(prefix + ".AQUA", AQUA);
        map.put(prefix + ".DARK_AQUA", DARK_AQUA);
        map.put(prefix + ".DARK_BLUE", DARK_BLUE);
        map.put(prefix + ".BLUE", BLUE);
        map.put(prefix + ".LIGHT_PURPLE", LIGHT_PURPLE);
        map.put(prefix + ".DARK_PURPLE", DARK_PURPLE);
        map.put(prefix + ".WHITE", WHITE);
        map.put(prefix + ".GRAY", GRAY);
        map.put(prefix + ".DARK_GRAY", DARK_GRAY);
        map.put(prefix + ".BLACK", BLACK);
    }
}
