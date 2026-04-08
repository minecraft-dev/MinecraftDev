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

package com.demonwav.mcdev.platform.mixin.inspection.addedMembers

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.RegexConverter
import com.demonwav.mcdev.util.doBindText
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.regexValidator
import com.demonwav.mcdev.util.toRegexOrDefault
import com.demonwav.mcdev.util.toRegexOrNull
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.annotations.Attribute
import com.siyeh.ig.fixes.RenameFix
import java.util.regex.Pattern
import javax.swing.JComponent

class EnumConstantNameFormatInspection : AbstractAddedMembersInspection() {
    @JvmField
    var validNameFormat = "^{MODID}_.+"

    @Attribute(converter = RegexConverter::class)
    @JvmField
    var validNameFixSearch = "^.+$".toRegex()

    @JvmField
    var validNameFixReplace = "{MODID}_$0"

    override fun getStaticDescription() = "Reports enum constants that are not properly prefixed by the mod id"

    override fun visitAddedField(holder: ProblemsHolder, field: PsiField) {
        if (field !is PsiEnumConstant) {
            return
        }

        val module = field.findModule() ?: return

        val validNameFormat = getValidNameFormat(module) ?: return
        val name = field.name
        if (validNameFormat.matches(name)) {
            return
        }

        // try to get a quick fix
        val fixed = try {
            validNameFixSearch.replace(name, validNameFixReplace)
                .replace("{MODID}", convertModIdToScreamingSnakeCase(holder.project.name))
        } catch (_: RuntimeException) {
            null
        }

        if (fixed != null && StringUtil.isJavaIdentifier(fixed) && validNameFormat.matches(fixed)) {
            holder.registerProblem(
                field.nameIdentifier,
                "Name does not match the pattern for added mixin members: \"${this.validNameFormat}\"",
                RenameFix(fixed)
            )
        } else {
            holder.registerProblem(
                field.nameIdentifier,
                "Name does not match the pattern for added mixin members: \"${this.validNameFormat}\"",
            )
        }
    }

    override fun visitAddedMethod(holder: ProblemsHolder, method: PsiMethod, isInherited: Boolean) {
    }

    private fun getValidNameFormat(module: Module): Regex? {
        return CachedValuesManager.getManager(module.project).getCachedValue(module) {
            CachedValueProvider.Result(getValidNameFormatUncached(module), PsiModificationTracker.MODIFICATION_COUNT)
        }
    }

    private fun getValidNameFormatUncached(module: Module): Regex? {
        val modules = MinecraftFacet.getInstance(module)?.modules ?: emptyList()
        val possibleModIds = setOf(module.project.name) + modules.flatMap { it.modIds }
        val validPrefixes = possibleModIds.map { convertModIdToScreamingSnakeCase(it) }

        return this.validNameFormat.replace(
            "{MODID}",
            "(?:${validPrefixes.joinToString("|") { Pattern.quote(it) }})"
        ).toRegexOrNull()
    }

    private fun convertModIdToScreamingSnakeCase(modId: String): String {
        return buildString(modId.length) {
            for ((i, ch) in modId.withIndex()) {
                if (!ch.isJavaIdentifierPart()) {
                    append('_')
                } else if (i > 0 && ch.isUpperCase() && modId[i - 1].isLowerCase()) {
                    append('_')
                    append(ch)
                } else {
                    if (i == 0 && !ch.isJavaIdentifierStart()) {
                        append('_')
                    }
                    append(ch.uppercase())
                }
            }
        }.ifEmpty { "_" }
    }

    override fun createOptionsPanel(): JComponent {
        return panel {
            row {
                val toolTip = "\"{MODID}\" is replaced with the mod ID, converted to SCREAMING_SNAKE_CASE."
                label("Valid name format:")
                    .applyToComponent { horizontalTextPosition = JBLabel.LEFT }
                    .applyToComponent { icon = AllIcons.General.ContextHelp }
                    .applyToComponent { toolTipText = toolTip }
                textField()
                    .doBindText(::validNameFormat)
                    .columns(COLUMNS_SHORT)
            }
            row("Valid name fix search:") {
                textField()
                    .doBindText({ validNameFixSearch.pattern }, { validNameFixSearch = it.toRegexOrDefault("^.+$") })
                    .columns(COLUMNS_SHORT)
                    .regexValidator()
            }
            row {
                val toolTip =
                    "Uses regex replacement syntax after matching from the regex in the option above.<br/>" +
                        "\"{MODID}\" is replaced with the mod ID, converted to SCREAMING_SNAKE_CASE."
                label("Valid name fix replace:")
                    .applyToComponent { horizontalTextPosition = JBLabel.LEFT }
                    .applyToComponent { icon = AllIcons.General.ContextHelp }
                    .applyToComponent { toolTipText = toolTip }
                textField().doBindText(::validNameFixReplace).columns(COLUMNS_SHORT)
            }
        }
    }
}
