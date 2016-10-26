/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.asset;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

public final class MessageAssets {

    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static Reference<ResourceBundle> ourBundle;
    @NonNls
    private static final String BUNDLE = "messages.MinecraftDevelopment";

    private MessageAssets() {
    }

    @NotNull
    public static String getGenerateEventListenerTitle() {
        return message("generate.event_listener");
    }

    public static String message(@PropertyKey(resourceBundle = BUNDLE) String key) {
        return CommonBundle.message(getBundle(), key);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            ourBundle = new SoftReference<>(bundle);
        }
        return bundle;
    }
}
