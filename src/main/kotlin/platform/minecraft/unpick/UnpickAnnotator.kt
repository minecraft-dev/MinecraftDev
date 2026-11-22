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

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickClassName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickDataType
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFieldExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFieldName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFlagsAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFormatAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickFormatName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupConstant
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupDefinition
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickGroupRef
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickHeader
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickLiteralExpression
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickMethodName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickPackageName
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickStrictAttribute
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTargetAnnotation
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTargetMethod
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickTypes
import com.demonwav.mcdev.platform.minecraft.unpick.gen.psi.UnpickUnknownAttribute
import com.demonwav.mcdev.util.isValidTypeDescriptor
import com.demonwav.mcdev.util.isValidMethodDescriptor
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import org.jetbrains.annotations.Nls

class UnpickAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (element is UnpickGroupAttribute) {
            highlightAttributeName(element, holder)
        }
        when (element) {
            is UnpickFile -> annotateFile(element, holder)
            is UnpickHeader -> annotateHeader(element, holder)
            is UnpickTargetAnnotation -> {
                val version = element.containingFile.let { (it as? UnpickFile)?.version }
                if (version != null && version < 4) {
                    holder.error(
                        MCDevBundle("minecraft.unpick.annotator.target_annotation_requires_v4"),
                        element.firstChild,
                    )
                }
            }
            is UnpickDataType -> annotateDataType(element, holder)
            is UnpickFormatName -> annotateFormat(element, holder)
            is UnpickUnknownAttribute -> {
                val name = element.node.findChildByType(UnpickTypes.IDENTIFIER)
                if (name == null) {
                    holder.error(MCDevBundle("minecraft.unpick.annotator.expected_attribute"), element)
                } else {
                    holder.error(MCDevBundle("minecraft.unpick.annotator.unknown_attribute", name.text), name.psi)
                }
            }
            is UnpickFlagsAttribute -> annotateFlags(element, holder)
            is UnpickGroupDefinition -> annotateGroupDefinition(element, holder)
            is UnpickGroupConstant -> {
                val wildcards = PsiTreeUtil.findChildrenOfType(element, UnpickFieldExpression::class.java)
                    .mapNotNull { it.wildcardElement }
                for (wildcard in wildcards.drop(1)) {
                    holder.error(MCDevBundle("minecraft.unpick.annotator.multiple_wildcards"), wildcard)
                }
            }
            is UnpickTargetMethod -> annotateTargetMethod(element, holder)
            is UnpickMethodName -> {
                holder.highlight(element, UnpickSyntaxHighlighter.METHOD_NAME)
                if (element.node.findChildByType(UnpickTypes.LT) != null) {
                    val name = element.node.findChildByType(UnpickTypes.IDENTIFIER)
                    if (name != null && name.text != "init" && name.text != "clinit") {
                        holder.error(MCDevBundle("minecraft.unpick.annotator.invalid_special_method"), name.psi)
                    }
                }
            }
            is UnpickLiteralExpression -> {
                val result = element.parseResult
                if (result is UnpickLiterals.Result.Error) {
                    holder.error(result.message, element)
                }
            }
            is UnpickGroupName, is UnpickGroupRef -> holder.highlight(element, UnpickSyntaxHighlighter.GROUP_NAME)
            is UnpickClassName, is UnpickPackageName -> holder.highlight(element, UnpickSyntaxHighlighter.CLASS_NAME)
            is UnpickFieldName -> holder.highlight(element, UnpickSyntaxHighlighter.FIELD_NAME)
            else -> when (element.elementType) {
                // Attribute names are highlighted with the attribute
                UnpickTypes.KEYWORD -> if (element.parent !is UnpickGroupAttribute) {
                    holder.highlight(element, UnpickSyntaxHighlighter.KEYWORD)
                }
                UnpickTypes.METHOD_DESCRIPTOR -> if (!isValidMethodDescriptor(element.text)) {
                    holder.error(MCDevBundle("minecraft.unpick.descriptor.invalid_method"), element)
                }
                UnpickTypes.FIELD_DESCRIPTOR -> if (!isValidTypeDescriptor(element.text)) {
                    holder.error(MCDevBundle("minecraft.unpick.descriptor.invalid_field"), element)
                }
            }
        }
    }

    private fun annotateFile(file: UnpickFile, holder: AnnotationHolder) {
        if (file.textLength == 0) {
            return
        }
        val text = file.viewProvider.contents
        val firstLineEnd = text.indexOfAny(charArrayOf('\r', '\n')).let { if (it == -1) text.length else it }
        val firstLine = text.subSequence(0, firstLineEnd)
        val header = file.header
        if (header == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, MCDevBundle("minecraft.unpick.annotator.missing_header"))
                .range(TextRange(0, firstLineEnd.coerceIn(1, file.textLength)))
                .create()
            return
        }

        // The reference implementation requires the first line to match exactly
        val version = header.version ?: return
        val expectedFirstLine = "unpick v$version"
        if (version in UnpickLanguage.SUPPORTED_VERSIONS && !firstLine.contentEquals(expectedFirstLine)) {
            holder.newAnnotation(
                HighlightSeverity.ERROR,
                MCDevBundle("minecraft.unpick.annotator.malformed_header", expectedFirstLine),
            ).range(TextRange(0, firstLineEnd)).create()
        }
    }

    private fun annotateHeader(header: UnpickHeader, holder: AnnotationHolder) {
        val version = header.version
        if (version == null) {
            holder.error(MCDevBundle("minecraft.unpick.annotator.missing_version"), header.versionElement ?: header)
            return
        }
        if (version !in UnpickLanguage.SUPPORTED_VERSIONS) {
            holder.newAnnotation(
                HighlightSeverity.WARNING,
                MCDevBundle("minecraft.unpick.annotator.unsupported_version", header.versionElement!!.text),
            ).range(header).create()
        }
    }

    private fun highlightAttributeName(attribute: UnpickGroupAttribute, holder: AnnotationHolder) {
        val name = attribute.node.findChildByType(UnpickTypes.KEYWORD) ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(TextRange(attribute.textRange.startOffset, name.textRange.endOffset))
            .textAttributes(UnpickSyntaxHighlighter.ATTRIBUTE)
            .create()
    }

    private fun annotateDataType(element: UnpickDataType, holder: AnnotationHolder) {
        val dataType = element.dataType
        if (dataType == null) {
            holder.error(MCDevBundle("minecraft.unpick.annotator.unknown_data_type", element.text), element)
            return
        }
        holder.highlight(element, UnpickSyntaxHighlighter.DATA_TYPE)
        if (element.parent is UnpickGroupDefinition && !dataType.isValidGroupType) {
            holder.error(MCDevBundle("minecraft.unpick.annotator.invalid_group_data_type", element.text), element)
        }
    }

    private fun annotateFormat(element: UnpickFormatName, holder: AnnotationHolder) {
        val format = GroupFormat.fromKeyword(element.text)
        if (format == null) {
            holder.error(MCDevBundle("minecraft.unpick.annotator.unknown_format", element.text), element)
            return
        }
        holder.highlight(element, UnpickSyntaxHighlighter.FORMAT)
        val dataType = PsiTreeUtil.getParentOfType(element, UnpickGroupDefinition::class.java)?.dataType ?: return
        if (dataType.dataType?.isValidGroupType == true && !format.isApplicableTo(dataType.dataType!!)) {
            holder.error(
                MCDevBundle("minecraft.unpick.annotator.format_not_applicable", element.text, dataType.text),
                element,
            )
        }
    }

    private fun annotateFlags(element: UnpickFlagsAttribute, holder: AnnotationHolder) {
        val group = element.parent as? UnpickGroupDefinition ?: return
        val dataType = group.dataType?.dataType
        if (dataType != null && dataType.isValidGroupType && dataType != DataType.INT && dataType != DataType.LONG) {
            holder.error(MCDevBundle("minecraft.unpick.annotator.flags_not_applicable"), element)
        }
        if (group.isDefaultGroup) {
            holder.error(MCDevBundle("minecraft.unpick.annotator.flags_default_group"), element)
        }
    }

    private fun annotateGroupDefinition(group: UnpickGroupDefinition, holder: AnnotationHolder) {
        val attributes = group.groupAttributeList
        val duplicates = listOf(
            attributes.filterIsInstance<UnpickFlagsAttribute>() to "minecraft.unpick.annotator.duplicate_flags",
            attributes.filterIsInstance<UnpickStrictAttribute>() to "minecraft.unpick.annotator.duplicate_strict",
            attributes.filterIsInstance<UnpickFormatAttribute>() to "minecraft.unpick.annotator.duplicate_format",
        )
        for ((attributesOfType, key) in duplicates) {
            for (duplicate in attributesOfType.drop(1)) {
                holder.error(MCDevBundle(key), duplicate)
            }
        }

        val firstConstantOffset = group.groupConstantList.firstOrNull()?.textOffset ?: return
        for (attribute in attributes) {
            if (attribute.textOffset > firstConstantOffset) {
                holder.error(MCDevBundle("minecraft.unpick.annotator.attribute_after_constant"), attribute)
            }
        }
    }

    private fun annotateTargetMethod(target: UnpickTargetMethod, holder: AnnotationHolder) {
        val seenParams = HashSet<Int>()
        for (param in target.targetMethodParamList) {
            val indexElement = param.index ?: continue
            val index = UnpickLiterals.parseInt(indexElement.text)
            if (index == null) {
                holder.error(MCDevBundle("minecraft.unpick.literal.integer_out_of_bounds"), indexElement)
                continue
            }
            if (!seenParams.add(index)) {
                holder.error(MCDevBundle("minecraft.unpick.annotator.duplicate_param", index), param)
            }
        }
        for (duplicate in target.targetMethodReturnList.drop(1)) {
            holder.error(MCDevBundle("minecraft.unpick.annotator.duplicate_return"), duplicate)
        }
    }

    private fun AnnotationHolder.error(@Nls message: String, element: PsiElement) {
        newAnnotation(HighlightSeverity.ERROR, message).range(element).create()
    }

    private fun AnnotationHolder.highlight(element: PsiElement, key: TextAttributesKey) {
        newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(key).create()
    }
}
