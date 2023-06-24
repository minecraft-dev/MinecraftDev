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

package com.demonwav.mcdev.platform.mixin.inspection.addedMembers

import com.demonwav.mcdev.util.decapitalize
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.onShown
import com.demonwav.mcdev.util.toJavaIdentifier
import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.ide.util.SuperMethodWarningUtil
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.COLUMNS_SHORT
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.annotations.Attribute
import java.util.function.Supplier
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import kotlin.reflect.KMutableProperty0
import org.intellij.lang.annotations.Language

class AddedMembersNameFormatInspection : AbstractAddedMembersInspection() {
    @Attribute(converter = RegexConverter::class)
    @JvmField
    var validNameFormat = ".+[_$].+".toRegex()

    @Attribute(converter = RegexConverter::class)
    @JvmField
    var validNameFixSearch = "^.+$".toRegex()

    @JvmField
    var validNameFixReplace = "MOD_ID\\\$\$0"

    @JvmField
    var ignoreFields = false
    @JvmField
    var ignoreMethods = false
    @JvmField
    var ignoreInheritedInterfaceMethods = false

    override fun getStaticDescription() = "Reports added members not matching the correct name format"

    override fun visitAddedField(holder: ProblemsHolder, field: PsiField) {
        if (!ignoreFields) {
            visitAdded(holder, field)
        }
    }

    override fun visitAddedMethod(holder: ProblemsHolder, method: PsiMethod, isInherited: Boolean) {
        if (!shouldIgnoreMethod(method, isInherited)) {
            visitAdded(holder, method)
        }
    }

    private fun shouldIgnoreMethod(method: PsiMethod, isInherited: Boolean): Boolean {
        if (ignoreMethods) {
            return true
        }

        if (isInherited) {
            if (ignoreInheritedInterfaceMethods) {
                return true
            }

            val superMethods = method.findDeepestSuperMethods()
            val isInterfaceMethod = superMethods.any {
                val clazz = it.findContainingClass() ?: return@any false
                clazz.isInterface && clazz.containingFile?.isWritable == true
            }
            return !isInterfaceMethod
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
        } catch (e: RuntimeException) {
            null
        }

        if (fixed != null && StringUtil.isJavaIdentifier(fixed) && validNameFormat.matches(fixed)) {
            holder.registerProblem(
                added.nameIdentifier ?: return,
                "Name does not match the pattern for added mixin members: \"${validNameFormat.pattern}\"",
                RenameWithInheritanceFix(added, fixed)
            )
        } else {
            holder.registerProblem(
                added.nameIdentifier ?: return,
                "Name does not match the pattern for added mixin members: \"${validNameFormat.pattern}\"",
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
            row("Valid name format:") {
                textField()
                    .doBindText({ validNameFormat.pattern }, { validNameFormat = it.toRegexOrDefault(".+[_$].+") })
                    .columns(COLUMNS_SHORT)
                    .regexValidator()
            }
            row("Valid name fix search:") {
                textField()
                    .doBindText({ validNameFixSearch.pattern }, { validNameFixSearch = it.toRegexOrDefault(".+") })
                    .columns(COLUMNS_SHORT)
                    .regexValidator()
            }
            row("Valid name fix replace:") {
                textField().doBindText(::validNameFixReplace).columns(COLUMNS_SHORT)
            }

            separator()

            var ignoreFields: Cell<JBCheckBox>? = null
            row {
                ignoreFields = checkBox("Ignore fields").doBindSelected(::ignoreFields)
            }
            var ignoreMethods: Cell<JBCheckBox>? = null
            row {
                ignoreMethods = checkBox("Ignore methods").doBindSelected(::ignoreMethods)
            }
            // make sure ignore fields and ignore methods can't be selected at the same time
            ignoreFields!!.component.addActionListener {
                if (ignoreFields!!.component.isSelected) {
                    ignoreMethods!!.component.isSelected = false
                }
            }
            ignoreMethods!!.component.addActionListener {
                if (ignoreMethods!!.component.isSelected) {
                    ignoreFields!!.component.isSelected = false
                }
            }

            row {
                checkBox("Ignore inherited interface methods").doBindSelected(::ignoreInheritedInterfaceMethods)
            }
        }
    }
}

private fun String.toRegexOrDefault(@Language("RegExp") default: String): Regex {
    return try {
        this.toRegex()
    } catch (e: PatternSyntaxException) {
        default.toRegex()
    }
}

private fun Cell<JBTextField>.doBindText(property: KMutableProperty0<String>): Cell<JBTextField> {
    return doBindText(property.getter, property.setter)
}

private fun Cell<JBTextField>.doBindText(getter: () -> String, setter: (String) -> Unit): Cell<JBTextField> {
    component.text = getter()
    component.document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) {
            setter(component.text)
        }
    })
    return this
}

private fun Cell<JBCheckBox>.doBindSelected(property: KMutableProperty0<Boolean>): Cell<JBCheckBox> {
    component.isSelected = property.get()
    component.addActionListener {
        property.set(component.isSelected)
    }
    return this
}

private fun Cell<JBTextField>.regexValidator(): Cell<JBTextField> {
    var hasRegisteredValidator = false
    component.onShown {
        if (!hasRegisteredValidator) {
            hasRegisteredValidator = true
            val disposable = DialogWrapper.findInstance(component)?.disposable ?: return@onShown
            ComponentValidator(disposable).withValidator(
                Supplier {
                    try {
                        Pattern.compile(component.text)
                        null
                    } catch (e: PatternSyntaxException) {
                        ValidationInfoBuilder(component).error("Invalid regex")
                    }
                }
            ).andRegisterOnDocumentListener(component).installOn(component)
        }
    }
    return this
}

private class RegexConverter : Converter<Regex>() {
    override fun toString(value: Regex) = value.pattern

    override fun fromString(value: String) = runCatching { value.toRegex() }.getOrNull()
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

    override fun startInWriteAction() = isMethod
}
