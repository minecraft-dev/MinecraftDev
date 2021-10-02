/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2021 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.mixintestdata.invalidInjectorMethodSignatureInspection;

import java.lang.String;

public class MixedInOuter {
    public MixedInOuter() {
        this(method1());
        method2();
    }

    public MixedInOuter(String arg) {
    }

    private static String method1() {
        return null;
    }

    private static void method2() {
    }

    public class MixedInInner {
        public MixedInInner() {
        }

        public MixedInInner(String string) {
        }
    }

    public static class MixedInStaticInner {
        public MixedInStaticInner() {
        }

        public MixedInStaticInner(String string) {
        }
    }
}
