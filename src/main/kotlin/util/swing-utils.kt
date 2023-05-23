/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2023 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
