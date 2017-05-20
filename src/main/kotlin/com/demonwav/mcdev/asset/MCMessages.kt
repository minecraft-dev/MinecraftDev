/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
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

object MCMessages {
    operator fun get(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) = CommonBundle.message(bundle, key, *params)
    operator fun get(@PropertyKey(resourceBundle = BUNDLE) key: String) = CommonBundle.message(bundle, key)

    @NonNls private const val BUNDLE = "assets.messages.MinecraftDevelopment"
    private var ourBundle: Reference<ResourceBundle>? = null

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
