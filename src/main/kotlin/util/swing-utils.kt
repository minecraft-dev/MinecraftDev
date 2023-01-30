/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util

import java.awt.Component
import java.awt.event.HierarchyEvent

fun Component.onShown(func: (HierarchyEvent) -> Unit) {
    addHierarchyListener { event ->
        if ((event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && isShowing) {
            func(event)
        }
    }
}

fun Component.onHidden(func: (HierarchyEvent) -> Unit) {
    addHierarchyListener { event ->
        if ((event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L && !isShowing) {
            func(event)
        }
    }
}
