/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
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

package com.demonwav.mcdev.creator

import com.demonwav.mcdev.MinecraftSettings
import com.demonwav.mcdev.creator.custom.TemplateDescriptor
import com.demonwav.mcdev.creator.custom.providers.LoadedTemplate
import com.demonwav.mcdev.creator.custom.providers.TemplateProvider
import com.demonwav.mcdev.creator.custom.providers.VfsLoadedTemplate
import com.demonwav.mcdev.util.refreshSync
import com.demonwav.mcdev.util.virtualFile
import com.google.gson.JsonParser
import com.intellij.ide.util.projectWizard.WizardContext
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.readText

/**
 * The built-in templates from the `templates` submodule, loaded through the same provider code the wizard uses.
 *
 * Templates are named by their descriptor's path in the checkout without the `.mcdev.template.json` suffix, for
 * example `forge` or `bukkit/paper`.
 *
 * Loading fails with a message saying what to fix if the checkout, or the branch `.gitmodules` pins it to, isn't on the
 * format version the tests expect.
 */
object PinnedTemplates {

    private const val ROOT_PROPERTY = "mcdev.test.templates"
    private const val PINNED_VERSION_PROPERTY = "mcdev.test.templatesFormatVersion"
    private const val DESCRIPTOR_SUFFIX = ".mcdev.template.json"

    val root: Path by lazy { Path(requireProperty(ROOT_PROPERTY)) }

    // The format version of the branch .gitmodules pins the templates submodule to
    private val pinnedFormatVersion: Int by lazy { requireProperty(PINNED_VERSION_PROPERTY).toInt() }

    // A wrong pin also makes checkFormatVersion's advice wrong, so only report the pin in that case
    private val checkoutProblem: String? by lazy { checkPinnedVersion(pinnedFormatVersion) ?: checkFormatVersion(root) }

    /**
     * Throws [AssertionError] if the checkout or `.gitmodules` isn't on [TemplateDescriptor.LATEST_FORMAT_VERSION].
     */
    fun requireExpectedFormat() {
        checkoutProblem?.let { throw AssertionError(it) }
    }

    /**
     * Loads one template the way [TemplateProvider.findTemplates] does.
     */
    suspend fun load(context: WizardContext, name: String): VfsLoadedTemplate {
        requireExpectedFormat()
        val descriptorPath = descriptors(root)[name] ?: throw AssertionError("There's no template named $name in $root")
        val modalityState = context.modalityState
        val repoRoot = checkNotNull(root.virtualFile) { "Couldn't find the templates checkout at $root" }
        repoRoot.refreshSync(modalityState)
        val descriptorFile = checkNotNull(descriptorPath.virtualFile) { "Couldn't find $descriptorPath" }
        val bundle = TemplateProvider.loadMessagesBundle(modalityState, repoRoot)
        val template = TemplateProvider.createVfsLoadedTemplate(
            modalityState,
            descriptorFile.parent,
            descriptorFile,
            bundle = bundle,
        )
        return template ?: throw AssertionError("The template $name is hidden")
    }

    /**
     * Loads every template in the checkout through the local template provider.
     */
    suspend fun loadAll(context: WizardContext): Collection<LoadedTemplate> {
        requireExpectedFormat()
        val repo = MinecraftSettings.TemplateRepo("Built-in templates", "local", root.toString())
        return checkNotNull(TemplateProvider.get("local")).loadTemplates(context, repo)
    }

    /**
     * The names of the templates in the checkout, sorted.
     */
    fun names(): List<String> = descriptors(root).keys.toList()

    /**
     * Returns a message explaining what's wrong if [root] has no templates or any template isn't on [expected], or
     * null if every template is on it.
     */
    fun checkFormatVersion(root: Path, expected: Int = TemplateDescriptor.LATEST_FORMAT_VERSION): String? {
        val descriptors = descriptors(root)
        if (descriptors.isEmpty()) {
            return "There are no templates at $root. Check that the templates submodule is checked out."
        }

        val mismatches = descriptors.mapNotNull { (name, file) ->
            val version = runCatching {
                JsonParser.parseString(file.readText()).asJsonObject.get("version")?.asInt
            }.getOrNull()
            when (version) {
                expected -> null
                null -> "$name: no readable format version"
                else -> "$name: format version $version"
            }
        }
        if (mismatches.isEmpty()) {
            return null
        }

        return buildString {
            append("The templates at $root aren't all on format version $expected, which the creator tests expect:\n")
            for (mismatch in mismatches) {
                append("  ").append(mismatch).append('\n')
            }
            append("Check out the v$expected branch in the templates submodule, as pinned in .gitmodules.")
        }
    }

    private fun checkPinnedVersion(pinned: Int): String? {
        val expected = TemplateDescriptor.LATEST_FORMAT_VERSION
        if (pinned == expected) {
            return null
        }
        return ".gitmodules pins the templates submodule to the v$pinned branch, but the plugin's latest template " +
            "format version is $expected. Change them together."
    }

    private fun requireProperty(name: String): String =
        checkNotNull(System.getProperty(name)) { "The $name system property isn't set" }

    // Template names to descriptor files, sorted by name
    private fun descriptors(root: Path): Map<String, Path> {
        if (!root.isDirectory()) {
            return emptyMap()
        }
        return root.toFile().walkTopDown()
            .onEnter { it.name != ".git" }
            .filter { it.isFile && it.name.endsWith(DESCRIPTOR_SUFFIX) }
            .map { file ->
                val relative = file.relativeTo(root.toFile()).invariantSeparatorsPath
                relative.removeSuffix(DESCRIPTOR_SUFFIX).removeSuffix("/") to file.toPath()
            }
            .sortedBy { (name, _) -> name }
            .toMap()
    }
}
