/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureFix;

public class MixedInSimple {
    public void simpleMethod(String string, int i) {
        int testInt = Integer.parseInt("FF", 16);
    }
}
