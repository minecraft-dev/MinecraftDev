/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.toml.platform.forge.completion

import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.toml.TomlStringValueInsertionHandler
import com.demonwav.mcdev.toml.inModsTomlKey
import com.demonwav.mcdev.toml.inModsTomlValueWithKey
import com.demonwav.mcdev.toml.platform.forge.ModsTomlSchema
import com.demonwav.mcdev.util.isAncestorOf
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.toml.lang.psi.TomlArrayTable
import org.toml.lang.psi.TomlHeaderOwner
import org.toml.lang.psi.TomlKey
import org.toml.lang.psi.TomlKeySegment
import org.toml.lang.psi.TomlKeyValue
import org.toml.lang.psi.TomlKeyValueOwner
import org.toml.lang.psi.TomlLiteral
import org.toml.lang.psi.TomlTable
import org.toml.lang.psi.TomlTableHeader

class ModsTomlCompletionContributor : CompletionContributor() {

    init {
        extend(CompletionType.BASIC, inModsTomlKey(), ModsTomlKeyCompletionProvider)
        extendKnownValues("modLoader", "javafml")
        extendKnownValues("version", ForgeConstants.KNOWN_SUBSTITUTIONS)
        extendKnownValues("ordering", ForgeConstants.DEPENDENCY_ORDER)
        extendKnownValues("side", ForgeConstants.DEPENDENCY_SIDES)
        extendBooleanValues("showAsResourcePack")
        extendBooleanValues("logoBlur")
        extendBooleanValues("mandatory")
    }

    private fun extendKnownValues(key: String, values: Set<String>) =
        extend(
            CompletionType.BASIC,
            inModsTomlValueWithKey(key),
            ModsTomlKnownStringValuesCompletionProvider(values)
        )

    private fun extendKnownValues(key: String, vararg values: String) =
        extendKnownValues(key, values.toSet())

    private fun extendBooleanValues(key: String) =
        extend(CompletionType.BASIC, inModsTomlValueWithKey(key), ModsTomlBooleanCompletionProvider)
}

object ModsTomlKeyCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val schema = ModsTomlSchema.get(parameters.position.project)

        val keySegment = parameters.position.parent as? TomlKeySegment ?: return
        val key = keySegment.parent as? TomlKey ?: return
        val table = key.parentOfType<TomlKeyValueOwner>()
        val variants = when (val parent = key.parent) {
            is TomlTableHeader -> {
                if (key != parent.key?.segments?.firstOrNull()) {
                    return
                }
                val isArray = when (table) {
                    is TomlArrayTable -> true
                    is TomlTable -> false
                    else -> return
                }
                schema.topLevelKeys(isArray) - table.entries.map { it.key.text }
            }
            is TomlKeyValue -> when (table) {
                null -> {
                    schema.topLevelEntries.map { it.key } -
                        key.containingFile.children.filterIsInstance<TomlKeyValue>().map { it.key.text }
                }
                is TomlHeaderOwner -> {
                    val tableName = table.header.key?.segments?.firstOrNull()?.text ?: return
                    schema.keysForTable(tableName) - table.entries.map { it.key.text }
                }
                else -> return
            }
            else -> return
        }
        result.addAllElements(variants.map(LookupElementBuilder::create))
    }
}

class ModsTomlKnownStringValuesCompletionProvider(private val knownValues: Set<String>) :
    CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val keyValue = getClosestKeyValueAncestor(parameters.position) ?: return
        result.addAllElements(
            knownValues.map {
                LookupElementBuilder.create(it).withInsertHandler(TomlStringValueInsertionHandler(keyValue))
            }
        )
    }
}

object ModsTomlBooleanCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        // Make sure we do not complete after an existing value
        getClosestKeyValueAncestor(parameters.position) ?: return
        result.addAllElements(listOf(LookupElementBuilder.create("true"), LookupElementBuilder.create("false")))
    }
}

private fun getClosestKeyValueAncestor(position: PsiElement): TomlKeyValue? {
    val parent = position.parent ?: return null
    val keyValue = PsiTreeUtil.getParentOfType(parent, TomlKeyValue::class.java, false)
        ?: error("PsiElementPattern must not allow values outside of TomlKeyValues")
    // If a value is already present we should ensure that the value is a literal
    // and the caret is inside the value to forbid completion in cases like
    // `key = "" <caret>`
    val value = keyValue.value
    return keyValue.takeIf { value == null || !(value !is TomlLiteral || !value.isAncestorOf(position)) }
}
