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

import com.demonwav.mcdev.asset.MCDevBundle
import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.minecraft.ct.CtInterfaceInjections
import com.demonwav.mcdev.util.RegexConverter
import com.demonwav.mcdev.util.decapitalize
import com.demonwav.mcdev.util.doBindItem
import com.demonwav.mcdev.util.doBindText
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.regexValidator
import com.demonwav.mcdev.util.toJavaIdentifier
import com.demonwav.mcdev.util.toRegexOrDefault
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.icons.AllIcons
import com.intellij.ide.util.SuperMethodWarningUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.ui.EnumComboBoxModel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.xmlb.annotations.Attribute
import java.util.function.Supplier
import javax.swing.JComponent

class AddedMembersNameFormatInspection : AbstractAddedMembersInspection() {
    @Attribute(converter = RegexConverter::class)
    @JvmField
    var validNameFormat = ".+[_$].+".toRegex()

    @Attribute(converter = RegexConverter::class)
    @JvmField
    var validNameFixSearch = "^.+$".toRegex()

    @JvmField
    var validNameFixReplace = $$"MOD_ID\\$$0"

    @JvmField
    var reportFields = ReportMode.NOT_ON_FABRIC
    @JvmField
    var reportMethods = ReportMode.NOT_ON_FABRIC
    @JvmField
    var reportInheritedMethods = ReportMode.ALWAYS

    @JvmField
    var ignoreInheritedWithProjectTypeInDescriptor = true
    @JvmField
    var ignoreInheritedFromTransitivelyInjectedInterfaces = true
    @JvmField
    var ignoreInheritedFromApiInterfaces = true

    @Attribute(converter = RegexConverter::class)
    @JvmField
    var apiPackageFormat = DEFAULT_API_PACKAGE_FORMAT.toRegex()

    override fun visitAddedField(holder: ProblemsHolder, field: PsiField) {
        if (field !is PsiEnumConstant && reportFields.shouldReport(field.findModule())) {
            visitAdded(holder, field)
        }
    }

    override fun visitAddedMethod(holder: ProblemsHolder, method: PsiMethod, isInherited: Boolean) {
        if (shouldReportMethod(method, isInherited)) {
            visitAdded(holder, method)
        }
    }

    private fun shouldReportMethod(method: PsiMethod, isInherited: Boolean): Boolean {
        val module = method.findModule()

        if (isInherited) {
            if (!reportInheritedMethods.shouldReport(module)) {
                return false
            }

            val writableInterfaces = method.findDeepestSuperMethods().mapNotNull { superMethod ->
                superMethod.findContainingClass()?.takeIf { it.isInterface && it.containingFile?.isWritable == true }
            }
            if (writableInterfaces.isEmpty()) {
                return false
            }

            if (ignoreInheritedWithProjectTypeInDescriptor && hasProjectTypeInDescriptor(method)) {
                return false
            }

            return writableInterfaces.any { !isIgnoredInterface(it) }
        } else {
            return reportMethods.shouldReport(module)
        }
    }

    private fun hasProjectTypeInDescriptor(method: PsiMethod): Boolean {
        val fileIndex = ProjectFileIndex.getInstance(method.project)
        val types = method.parameterList.parameters.map { it.type } + listOfNotNull(method.returnType)
        return types.any { type ->
            val erased = TypeConversionUtil.erasure(type).deepComponentType as? PsiClassType ?: return@any false
            val file = erased.resolve()?.containingFile?.virtualFile ?: return@any false
            fileIndex.isInSourceContent(file)
        }
    }

    private fun isIgnoredInterface(iface: PsiClass): Boolean {
        if (ignoreInheritedFromApiInterfaces) {
            val packageName = PsiUtil.getPackageName(iface)
            if (packageName != null && apiPackageFormat.matches(packageName)) {
                return true
            }
        }

        if (ignoreInheritedFromTransitivelyInjectedInterfaces && CtInterfaceInjections.isTransitivelyInjected(iface)) {
            return true
        }

        return false
    }

    private fun visitAdded(holder: ProblemsHolder, added: PsiNameIdentifierOwner) {
        val name = added.name ?: return
        if (validNameFormat.matches(name)) {
            return
        }

        // try to get a quick fix
        val fixed = try {
            validNameFixSearch.replace(name, validNameFixReplace)
                .replace("MOD_ID", getAppropriatePrefix(holder.project))
        } catch (_: RuntimeException) {
            null
        }

        if (fixed != null && StringUtil.isJavaIdentifier(fixed) && validNameFormat.matches(fixed)) {
            holder.registerProblem(
                added.nameIdentifier ?: return,
                MCDevBundle("inspection.added_mixin_members_name_pattern.message", validNameFormat.pattern),
                RenameWithInheritanceFix(added, fixed)
            )
        } else {
            holder.registerProblem(
                added.nameIdentifier ?: return,
                MCDevBundle("inspection.added_mixin_members_name_pattern.message", validNameFormat.pattern),
            )
        }
    }

    private fun getAppropriatePrefix(project: Project): String {
        return StringUtil.capitalizeWords(project.name, true)
            .decapitalize()
            .replace(" ", "")
            .toJavaIdentifier(allowDollars = false)
    }

    override fun createOptionsPanel(): JComponent {
        return panel {
            row(MCDevBundle("inspection.added_mixin_members_name_pattern.option.format")) {
                textField()
                    .doBindText({ validNameFormat.pattern }, { validNameFormat = it.toRegexOrDefault(".+[_$].+") })
                    .columns(COLUMNS_SHORT)
                    .regexValidator()
            }
            row(MCDevBundle("inspection.added_mixin_members_name_pattern.option.fix_search")) {
                textField()
                    .doBindText({ validNameFixSearch.pattern }, { validNameFixSearch = it.toRegexOrDefault(".+") })
                    .columns(COLUMNS_SHORT)
                    .regexValidator()
            }
            row {
                layout(RowLayout.LABEL_ALIGNED)
                val toolTip = MCDevBundle("inspection.added_mixin_members_name_pattern.option.fix_replace.tooltip")
                label(MCDevBundle("inspection.added_mixin_members_name_pattern.option.fix_replace"))
                    .applyToComponent { horizontalTextPosition = JBLabel.LEFT }
                    .applyToComponent { icon = AllIcons.General.ContextHelp }
                    .applyToComponent { toolTipText = toolTip }
                textField().doBindText(::validNameFixReplace).columns(COLUMNS_SHORT)
            }

            separator()

            row(MCDevBundle("inspection.added_mixin_members_name_pattern.option.report_fields")) {
                comboBox(EnumComboBoxModel(ReportMode::class.java)).doBindItem(::reportFields)
            }
            row(MCDevBundle("inspection.added_mixin_members_name_pattern.option.report_methods")) {
                comboBox(EnumComboBoxModel(ReportMode::class.java)).doBindItem(::reportMethods)
            }
            row {
                layout(RowLayout.LABEL_ALIGNED)
                label(MCDevBundle("inspection.added_mixin_members_name_pattern.option.report_inherited"))
                    .applyToComponent { horizontalTextPosition = JBLabel.LEFT }
                    .applyToComponent { icon = AllIcons.General.ContextHelp }
                    .applyToComponent { toolTipText = MCDevBundle(
                        "inspection.added_mixin_members_name_pattern.option.report_inherited.tooltip",
                    ) }
                comboBox(EnumComboBoxModel(ReportMode::class.java)).doBindItem(::reportInheritedMethods)
            }
            indent {
                row {
                    checkBox(MCDevBundle("inspection.added_mixin_members_name_pattern.option.ignore_project_types"))
                        .bindSelected(::ignoreInheritedWithProjectTypeInDescriptor)
                }
                row {
                    checkBox(MCDevBundle("inspection.added_mixin_members_name_pattern.option.ignore_class_tweaker"))
                        .bindSelected(::ignoreInheritedFromTransitivelyInjectedInterfaces)
                }
                row {
                    checkBox(MCDevBundle("inspection.added_mixin_members_name_pattern.option.ignore_api_package"))
                        .bindSelected(::ignoreInheritedFromApiInterfaces)
                    textField()
                        .doBindText(
                            { apiPackageFormat.pattern },
                            { apiPackageFormat = it.toRegexOrDefault(DEFAULT_API_PACKAGE_FORMAT) }
                        )
                        .columns(COLUMNS_SHORT)
                        .regexValidator()
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_API_PACKAGE_FORMAT = "(.+\\.)?api(\\..+)?"
    }

    enum class ReportMode(private val displayName: Supplier<String>) {
        ALWAYS(MCDevBundle.pointer("inspection.added_mixin_members_name_pattern.report_mode.always")),
        ON_FABRIC(MCDevBundle.pointer("inspection.added_mixin_members_name_pattern.report_mode.on_fabric")),
        NOT_ON_FABRIC(MCDevBundle.pointer("inspection.added_mixin_members_name_pattern.report_mode.not_on_fabric")),
        NEVER(MCDevBundle.pointer("inspection.added_mixin_members_name_pattern.report_mode.never"));

        override fun toString() = displayName.get()

        fun shouldReport(module: Module?): Boolean {
            if (this == NEVER) {
                return false
            }
            if (this == ALWAYS) {
                return true
            }
            val isFabric = module?.let { MinecraftFacet.getInstance(module, FabricModuleType) != null } ?: false
            return (this == ON_FABRIC) == isFabric
        }
    }
}

private class RenameWithInheritanceFix(
    element: PsiNamedElement,
    private val newName: String
) : LocalQuickFixAndIntentionActionOnPsiElement(element) {
    private val isMethod = element is PsiMethod
    private val text = CodeInsightBundle.message("rename.named.element.text", element.name, newName)

    override fun getFamilyName() = CodeInsightBundle.message("rename.element.family")

    override fun getText() = text

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (isMethod) {
            val method = startElement as? PsiMethod ?: return
            if (editor != null) {
                SuperMethodWarningUtil.checkSuperMethod(method, { md ->
                    RenameProcessor(project, md, newName, false, false).run()
                    true
                }, editor)
            } else {
                val superMethod = method.findDeepestSuperMethods().firstOrNull()
                for (md in listOfNotNull(superMethod, method)) {
                    RenameProcessor(project, md, newName, false, false).run()
                }
            }
        } else {
            if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
                return
            }
            RenameProcessor(project, startElement, newName, false, false).run()
        }
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        val element = PsiTreeUtil.findSameElementInCopy(startElement, file)
        val target = element as? PsiNamedElement ?: return IntentionPreviewInfo.EMPTY
        target.setName(newName)
        return IntentionPreviewInfo.DIFF
    }

    override fun startInWriteAction() = false
}
