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

package com.demonwav.mcdev.creator.step

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.creator.updateWhenChanged
import com.intellij.ide.users.LocalUserSettings
import com.intellij.ide.wizard.AbstractNewProjectWizardStep
import com.intellij.ide.wizard.NewProjectWizardBaseData.Companion.baseData
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.openapi.observable.util.bindStorage
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns

abstract class AbstractOptionalStringStep(parent: NewProjectWizardStep) : AbstractNewProjectWizardStep(parent) {
    protected abstract val label: String
    protected open val bindToStorage = false

    val valueProperty = propertyGraph.property("").apply {
        if (bindToStorage) {
            bindStorage("${this@AbstractOptionalStringStep.javaClass.name}.value")
        }
    }
    var value by valueProperty

    override fun setupUI(builder: Panel) {
        with(builder) {
            row(label) {
                textField()
                    .bindText(valueProperty)
                    .columns(COLUMNS_LARGE)
            }
        }
    }
}

abstract class AbstractOptionalStringBasedOnProjectNameStep(
    parent: NewProjectWizardStep,
) : AbstractOptionalStringStep(parent) {
    private val formatProperty = propertyGraph.property("").bindStorage("${javaClass.name}.format")
    var format by formatProperty

    init {
        if (format.isNotEmpty()) {
            value = suggestValue()
        }
        valueProperty.updateWhenChanged(formatProperty, ::suggestValue)
        valueProperty.updateWhenChanged(baseData!!.nameProperty, ::suggestValue)
        formatProperty.updateWhenChanged(valueProperty, ::suggestFormat)
    }

    private fun suggestValue() = format.replace(PROJECT_NAME_PLACEHOLDER, baseData!!.name)

    private fun suggestFormat(): String {
        val index = value.indexOf(baseData!!.name)
        if (index == -1) {
            return value
        }
        if (value.indexOf(baseData!!.name, startIndex = index + baseData!!.name.length) != -1) {
            // don't change format if there are multiple instances of the project name
            return format
        }
        return value.replace(baseData!!.name, PROJECT_NAME_PLACEHOLDER)
    }

    companion object {
        const val PROJECT_NAME_PLACEHOLDER = "{PROJECT_NAME}"
    }
}

class DescriptionStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label
        get() = MCDevBundle("creator.ui.description.label")

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${DescriptionStep::class.java.name}.description")
    }
}

class AuthorsStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label
        get() = MCDevBundle("creator.ui.authors.label")
    override val bindToStorage = true

    override fun setupProject(project: Project) {
        data.putUserData(KEY, parseAuthors(value))
    }

    companion object {
        val KEY = Key.create<List<String>>("${AuthorsStep::class.java.name}.authors")

        private val bracketRegex = Regex("[\\[\\]]")
        private val commaRegex = Regex("\\s*,\\s*")

        fun parseAuthors(string: String): List<String> {
            return if (string.isNotBlank()) {
                string.trim().replace(bracketRegex, "").split(commaRegex).toList()
            } else {
                emptyList()
            }
        }
    }
}

class WebsiteStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label
        get() = MCDevBundle("creator.ui.website.label")
    override val bindToStorage = true

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${WebsiteStep::class.java.name}.website")
    }
}

class RepositoryStep(parent: NewProjectWizardStep) : AbstractOptionalStringBasedOnProjectNameStep(parent) {
    override val label
        get() = MCDevBundle("creator.ui.repository.label")

    init {
        if (format.isEmpty()) {
            format = "https://github.com/${LocalUserSettings.userName}/$PROJECT_NAME_PLACEHOLDER"
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${RepositoryStep::class.java.name}.repository")
    }
}

class IssueTrackerStep(parent: NewProjectWizardStep) : AbstractOptionalStringBasedOnProjectNameStep(parent) {
    override val label: String
        get() = MCDevBundle("creator.ui.issue_tracker.label")

    init {
        if (format.isEmpty()) {
            format = "https://github.com/${LocalUserSettings.userName}/$PROJECT_NAME_PLACEHOLDER/issues"
        }
    }

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${IssueTrackerStep::class.java.name}.issueTracker")
    }
}

class UpdateUrlStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label
        get() = MCDevBundle("creator.ui.update_url.label")

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${UpdateUrlStep::class.java.name}.updateUrl")
    }
}

class DependStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label
        get() = MCDevBundle("creator.ui.depend.label")

    override fun setupProject(project: Project) {
        data.putUserData(KEY, AuthorsStep.parseAuthors(value))
    }

    companion object {
        val KEY = Key.create<List<String>>("${DependStep::class.java.name}.depend")
    }
}

class SoftDependStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label
        get() = MCDevBundle("creator.ui.soft_depend.label")

    override fun setupProject(project: Project) {
        data.putUserData(KEY, AuthorsStep.parseAuthors(value))
    }

    companion object {
        val KEY = Key.create<List<String>>("${SoftDependStep::class.java.name}.depend")
    }
}
