/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.asset

import com.intellij.CommonBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.lang.ref.Reference
import java.lang.ref.SoftReference
import java.util.ResourceBundle
import com.intellij.reference.SoftReference as IJSoftReference

object MessageAssets : Assets() {
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        return CommonBundle.message(bundle, key, *params)
    }

    private var ourBundle: Reference<ResourceBundle>? = null
    @NonNls
    private const val BUNDLE = "messages.MinecraftDevelopment"

    val generateEventListenerTitle: String
        get() = message("generate.event_listener")

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String): String {
        return CommonBundle.message(bundle, key)
    }

    private val bundle: ResourceBundle
        get() {
            var bundle = IJSoftReference.dereference(ourBundle)
            if (bundle == null) {
                bundle = ResourceBundle.getBundle(BUNDLE)
                ourBundle = SoftReference<ResourceBundle>(bundle)
            }
            return bundle!!
        }
}
