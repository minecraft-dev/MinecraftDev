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

public class MixedInOuter {
    public class MixedInInner {
        public MixedInInner() {
        }

        public MixedInInner(String string) {
        }
    }

    public MixedInInner methodWithInnerType(MixedInInner param) {
        return null;
    }
}
