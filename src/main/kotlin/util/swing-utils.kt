/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.xmlb.Converter
import java.awt.Component
import java.awt.event.HierarchyEvent
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import kotlin.reflect.KMutableProperty0

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

fun Cell<JBTextField>.doBindText(property: KMutableProperty0<String>): Cell<JBTextField> {
    return doBindText(property.getter, property.setter)
}

fun Cell<JBTextField>.doBindText(getter: () -> String, setter: (String) -> Unit): Cell<JBTextField> {
    component.text = getter()
    component.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
            setter(component.text)
        }
    })
    return this
}

fun <T> Cell<ComboBox<T>>.doBindItem(property: KMutableProperty0<T>): Cell<ComboBox<T>> {
    component.selectedItem = property.get()
    component.addActionListener {
        @Suppress("UNCHECKED_CAST")
        val selectedItem = component.selectedItem as T?
        if (selectedItem != null) {
            property.set(selectedItem)
        }
    }
    return this
}

fun Cell<JBTextField>.regexValidator(): Cell<JBTextField> {
    var hasRegisteredValidator = false
    component.onShown {
        if (!hasRegisteredValidator) {
            hasRegisteredValidator = true
            val disposable = DialogWrapper.findInstance(component)?.disposable ?: return@onShown
            ComponentValidator(disposable).withValidator(
                Supplier {
                    try {
                        Pattern.compile(component.text)
                        null
                    } catch (_: PatternSyntaxException) {
                        ValidationInfoBuilder(component).error("Invalid regex")
                    }
                }
            ).andRegisterOnDocumentListener(component).installOn(component)
        }
    }
    return this
}

class RegexConverter : Converter<Regex>() {
    override fun toString(value: Regex) = value.pattern

    override fun fromString(value: String) = value.toRegexOrNull()
}
