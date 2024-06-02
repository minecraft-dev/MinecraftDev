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

import com.demonwav.mcdev.creator.custom.providers.LoadedTemplate
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.application
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Text
import com.intellij.util.xmlb.annotations.XCollection

@State(name = "RecentProjectTemplates", storages = [Storage("minecraft_dev.xml")])
class RecentProjectTemplates : SimplePersistentStateComponent<RecentProjectTemplates.State>(State()) {

    class State : BaseState() {
        @get:XCollection(style = XCollection.Style.v2)
        var templates by list<TemplateItem>()
    }

    @Tag("template")
    data class TemplateItem(
        @get:Attribute("provider")
        var provider: String,
        @get:Text
        var location: String
    ) {
        constructor() : this("", "")
    }

    fun addNewTemplate(providerClassName: String, template: LoadedTemplate) {
        val serialized = template.serialize()
            ?: return

        val item = TemplateItem(providerClassName, serialized)

        state.templates.removeAll { it == item }
        state.templates.add(0, item)
    }

    companion object {
        val instance: RecentProjectTemplates
            get() = application.service()
    }
}
