/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2023 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.creator.step

import com.intellij.ide.wizard.AbstractNewProjectWizardStep
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

class DescriptionStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Description:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${DescriptionStep::class.java.name}.description")
    }
}

class AuthorsStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Authors:"
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
    override val label = "Website:"
    override val bindToStorage = true

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${WebsiteStep::class.java.name}.website")
    }
}

class RepositoryStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Repository:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${RepositoryStep::class.java.name}.repository")
    }
}

class IssueTrackerStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Issue Tracker:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${IssueTrackerStep::class.java.name}.issueTracker")
    }
}

class UpdateUrlStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Update URL:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, value)
    }

    companion object {
        val KEY = Key.create<String>("${UpdateUrlStep::class.java.name}.updateUrl")
    }
}

class DependStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Depend:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, AuthorsStep.parseAuthors(value))
    }

    companion object {
        val KEY = Key.create<List<String>>("${DependStep::class.java.name}.depend")
    }
}

class SoftDependStep(parent: NewProjectWizardStep) : AbstractOptionalStringStep(parent) {
    override val label = "Soft Depend:"

    override fun setupProject(project: Project) {
        data.putUserData(KEY, AuthorsStep.parseAuthors(value))
    }

    companion object {
        val KEY = Key.create<List<String>>("${SoftDependStep::class.java.name}.depend")
    }
}
