/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.creator.custom

import com.intellij.openapi.observable.properties.GraphProperty
import com.intellij.openapi.progress.TaskInfo
import com.intellij.openapi.progress.util.ProgressIndicatorBase

class CreatorProgressIndicator(
    val loadingProperty: GraphProperty<Boolean>? = null,
    val textProperty: GraphProperty<String>? = null,
    val text2Property: GraphProperty<String>? = null,
) : ProgressIndicatorBase(false, false) {

    init {
        loadingProperty?.set(false)
        textProperty?.set("")
        text2Property?.set("")
    }

    override fun start() {
        super.start()
        loadingProperty?.set(true)
    }

    override fun finish(task: TaskInfo) {
        super.finish(task)
        loadingProperty?.set(false)
    }

    override fun setText(text: String?) {
        super.setText(text)
        textProperty?.set(text ?: "")
    }

    override fun setText2(text: String?) {
        super.setText2(text)
        text2Property?.set(text ?: "")
    }
}
