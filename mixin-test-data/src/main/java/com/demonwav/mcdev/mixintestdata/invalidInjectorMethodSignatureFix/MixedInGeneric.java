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

import java.util.List;
import java.util.Map;

public class MixedInGeneric {
    public GenericOneParam<String> genericMethod(
            String noGenerics,
            GenericOneParam<String> oneParam,
            GenericTwoParams<String, Integer> twoParams,
            GenericOneParam<GenericOneParam<String>> nestedParam,
            Map<String, List<Map.Entry<String, Map<Integer, int[]>>>[]> pleaseJava
    ) {
        return null;
    }

    public Map.Entry<String, List<Map.Entry<String, Map<Integer, int[]>>>[]> returnComplex() {
        return null;
    }
}
