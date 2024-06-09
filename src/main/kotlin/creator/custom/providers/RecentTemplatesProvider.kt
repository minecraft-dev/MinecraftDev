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

package com.demonwav.mcdev.creator.custom.providers

import com.demonwav.mcdev.creator.custom.RecentProjectTemplates
import com.demonwav.mcdev.creator.modalityState
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.observable.properties.PropertyGraph
import java.util.function.Consumer
import javax.swing.JComponent

class RecentTemplatesProvider : TemplateProvider {

    override fun getLabel(): String = "Recent"

    override fun setupUi(
        context: WizardContext,
        propertyGraph: PropertyGraph,
        provideTemplate: Consumer<() -> Collection<LoadedTemplate>>
    ): JComponent? {
        provideTemplate.accept {
            RecentProjectTemplates.instance.state.templates.mapNotNull { (provider, element) ->
                TemplateProvider.get(provider)?.deserializeAndLoad(element, context.modalityState)
            }
        }
        return null
    }

    override fun deserializeAndLoad(element: String, modalityState: ModalityState): LoadedTemplate =
        throw UnsupportedOperationException("The recent templates provider is not supposed to deserialize nor load")
}
