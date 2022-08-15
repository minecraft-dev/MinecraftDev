/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.util

@Suppress("MemberVisibilityCanBePrivate")
object MixinConstants {
    const val PACKAGE = "org.spongepowered.asm.mixin."
    const val SMAP_STRATUM = "Mixin"
    const val DEFAULT_SHADOW_PREFIX = "shadow$"

    object Classes {
        const val CALLBACK_INFO = "org.spongepowered.asm.mixin.injection.callback.CallbackInfo"
        const val CALLBACK_INFO_RETURNABLE = "org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable"
        const val ARGS = "org.spongepowered.asm.mixin.injection.invoke.arg.Args"
        const val COMPATIBILITY_LEVEL = "org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel"
        const val CONSTANT_CONDITION = "org.spongepowered.asm.mixin.injection.Constant.Condition"
        const val INJECTION_POINT = "org.spongepowered.asm.mixin.injection.InjectionPoint"
        const val SELECTOR = "org.spongepowered.asm.mixin.injection.InjectionPoint.Selector"
        const val MIXIN_AGENT = "org.spongepowered.tools.agent.MixinAgent"
        const val MIXIN_CONFIG = "org.spongepowered.asm.mixin.transformer.MixinConfig"
        const val MIXIN_PLUGIN = "org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin"
        const val TARGET_SELECTOR_DYNAMIC = "org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic"
        const val SELECTOR_ID = "org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic.SelectorId"
        const val SHIFT = "org.spongepowered.asm.mixin.injection.At.Shift"

        const val SERIALIZED_NAME = "com.google.gson.annotations.SerializedName"
        const val MIXIN_SERIALIZED_NAME = "org.spongepowered.include.$SERIALIZED_NAME"
    }

    object Annotations {
        const val ACCESSOR = "org.spongepowered.asm.mixin.gen.Accessor"
        const val ANNOTATION_TYPE = "org.spongepowered.asm.mixin.injection.struct.InjectionInfo.AnnotationType"
        const val AT = "org.spongepowered.asm.mixin.injection.At"
        const val AT_CODE = "org.spongepowered.asm.mixin.injection.InjectionPoint.AtCode"
        const val COERCE = "org.spongepowered.asm.mixin.injection.Coerce"
        const val CONSTANT = "org.spongepowered.asm.mixin.injection.Constant"
        const val DEBUG = "org.spongepowered.asm.mixin.Debug"
        const val DESC = "org.spongepowered.asm.mixin.injection.Desc"
        const val DYNAMIC = "org.spongepowered.asm.mixin.Dynamic"
        const val FINAL = "org.spongepowered.asm.mixin.Final"
        const val IMPLEMENTS = "org.spongepowered.asm.mixin.Implements"
        const val INTERFACE = "org.spongepowered.asm.mixin.Interface"
        const val INTRINSIC = "org.spongepowered.asm.mixin.Intrinsic"
        const val MIXIN = "org.spongepowered.asm.mixin.Mixin"
        const val MUTABLE = "org.spongepowered.asm.mixin.Mutable"
        const val OVERWRITE = "org.spongepowered.asm.mixin.Overwrite"
        const val SHADOW = "org.spongepowered.asm.mixin.Shadow"
        const val SLICE = "org.spongepowered.asm.mixin.injection.Slice"
        const val SOFT_OVERRIDE = "org.spongepowered.asm.mixin.SoftOverride"
        const val UNIQUE = "org.spongepowered.asm.mixin.Unique"
        const val INJECT = "org.spongepowered.asm.mixin.injection.Inject"
        const val INVOKER = "org.spongepowered.asm.mixin.gen.Invoker"
        const val MODIFY_ARG = "org.spongepowered.asm.mixin.injection.ModifyArg"
        const val MODIFY_ARGS = "org.spongepowered.asm.mixin.injection.ModifyArgs"
        const val MODIFY_CONSTANT = "org.spongepowered.asm.mixin.injection.ModifyConstant"
        const val MODIFY_VARIABLE = "org.spongepowered.asm.mixin.injection.ModifyVariable"
        const val REDIRECT = "org.spongepowered.asm.mixin.injection.Redirect"
        const val SURROGATE = "org.spongepowered.asm.mixin.injection.Surrogate"
    }
}
