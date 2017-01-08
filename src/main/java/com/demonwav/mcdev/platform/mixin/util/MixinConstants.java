/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util;

import org.jetbrains.annotations.NotNull;

public final class MixinConstants {
    private MixinConstants() {}

    @NotNull public static final String SMAP_STRATUM = "Mixin";

    public static final class Annotations {
        private Annotations() {}

        @NotNull public static final String DEBUG = "org.spongepowered.asm.mixin.Debug";
        @NotNull public static final String FINAL = "org.spongepowered.asm.mixin.Final";
        @NotNull public static final String IMPLEMENTS = "org.spongepowered.asm.mixin.Implements";
        @NotNull public static final String INTERFACE = "org.spongepowered.asm.mixin.Interface";
        @NotNull public static final String INTRINSIC = "org.spongepowered.asm.mixin.Intrinsic";
        @NotNull public static final String MIXIN = "org.spongepowered.asm.mixin.Mixin";
        @NotNull public static final String MUTABLE = "org.spongepowered.asm.mixin.Mutable";
        @NotNull public static final String OVERWRITE = "org.spongepowered.asm.mixin.Overwrite";
        @NotNull public static final String SHADOW = "org.spongepowered.asm.mixin.Shadow";
        @NotNull public static final String SOFT_OVERRIDE = "org.spongepowered.asm.mixin.SoftOverride";
        @NotNull public static final String UNIQUE = "org.spongepowered.asm.mixin.Unique";
        @NotNull public static final String INJECT = "org.spongepowered.asm.mixin.injection.Inject";
        @NotNull public static final String MODIFY_ARG = "org.spongepowered.asm.mixin.injection.ModifyArg";
        @NotNull public static final String MODIFY_CONSTANT = "org.spongepowered.asm.mixin.injection.ModifyConstant";
        @NotNull public static final String MODIFY_VARIABLE = "org.spongepowered.asm.mixin.injection.ModifyVariable";
        @NotNull public static final String REDIRECT = "org.spongepowered.asm.mixin.injection.Redirect";
        @NotNull public static final String SURROGATE = "org.spongepowered.asm.mixin.injection.Surrogate";
    }
}
