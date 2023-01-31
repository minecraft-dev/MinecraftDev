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

import com.intellij.openapi.observable.properties.ObservableProperty
import com.intellij.openapi.observable.util.bindEnabled
import com.intellij.ui.dsl.builder.Cell
import java.awt.Component
import java.awt.event.HierarchyEvent
import javax.swing.JComponent

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

fun <T : JComponent> Cell<T>.bindEnabled(property: ObservableProperty<Boolean>): Cell<T> {
    applyToComponent {
        bindEnabled(property)
    }
    return this
}
