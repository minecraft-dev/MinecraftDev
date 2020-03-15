/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.translations.intentions

import com.demonwav.mcdev.translations.identification.TranslationFunction
import com.demonwav.mcdev.translations.identification.TranslationFunctionRepository
import com.demonwav.mcdev.util.MemberReference
import com.demonwav.mcdev.util.SemanticVersion
import com.demonwav.mcdev.util.findMcpModule
import com.demonwav.mcdev.util.mcVersion
import com.demonwav.mcdev.util.qualifiedMemberReference
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.lang.jvm.JvmParameter
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.util.IncorrectOperationException

class CreateTranslationFunctionIntention : PsiElementBaseIntentionAction() {
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        val method = findCall(element)?.resolveMethod() ?: findMethod(element)
        if (method == null) {
            HintManager.getInstance().showErrorHint(editor, "Cannot determine method")
            return
        }

        if (method.parameters.isEmpty()) {
            HintManager.getInstance()
                .showErrorHint(
                    editor,
                    "Method does not take any parameters. Must at least accept translation key"
                )
            return
        }

        JBPopupFactory.getInstance().createListPopup(ScopeStep(editor, element, method)).showInBestPositionFor(editor)
    }

    private class ScopeStep(
        val editor: Editor,
        val context: PsiElement,
        val method: PsiMethod
    ) : BaseListPopupStep<Boolean>("Choose the scope of the function", false, true) {
        override fun getTextFor(value: Boolean) = if (value) "Project" else "Global"

        override fun onChosen(projectScope: Boolean, finalChoice: Boolean): PopupStep<*>? {
            val srgManager = context.findMcpModule()?.srgManager
            val srgMember = srgManager?.srgMapNow?.getSrgMethod(method)
            val member = srgMember ?: method.qualifiedMemberReference
            val version = context.mcVersion

            if (version == null) {
                HintManager.getInstance().showErrorHint(editor, "Could not determine Minecraft version for method")
                return ListPopupStep.FINAL_CHOICE
            }

            val currentConfigs = if (projectScope) {
                TranslationFunctionRepository.getProjectConfigFiles(context.project)
            } else {
                TranslationFunctionRepository.getGlobalConfigFiles()
            }
            val alreadyExists = currentConfigs[version]?.entries?.any { it.member == member } == true
            if (alreadyExists) {
                HintManager.getInstance()
                    .showErrorHint(editor, "The method is already configured as translation function")
                return ListPopupStep.FINAL_CHOICE
            }

            return ParameterStep(context, method, version, projectScope, member, srgMember != null)
        }
    }

    private class ParameterStep(
        val context: PsiElement,
        val method: PsiMethod,
        val version: SemanticVersion,
        val projectScope: Boolean,
        val member: MemberReference,
        val srgName: Boolean
    ) : BaseListPopupStep<JvmParameter>("Choose Key Parameter", *method.parameters) {
        override fun getTextFor(value: JvmParameter) = value.name ?: ""

        override fun onChosen(parameter: JvmParameter, finalChoice: Boolean) =
            method.parameters.indexOf(parameter).let {
                if (method.isVarArgs) {
                    FormattingStep(context, version, projectScope, member, srgName, it)
                } else {
                    FoldParametersOnlyStep(context, version, projectScope, member, srgName, it, false)
                }
            }
    }

    private class FormattingStep(
        val context: PsiElement,
        val version: SemanticVersion,
        val projectScope: Boolean,
        val member: MemberReference,
        val srgName: Boolean,
        val paramIndex: Int
    ) : BaseListPopupStep<Boolean>("Does the function support formatting?", true, false) {
        override fun getTextFor(value: Boolean) = if (value) "Yes" else "No"

        override fun onChosen(formatting: Boolean, finalChoice: Boolean) =
            FoldParametersOnlyStep(context, version, projectScope, member, srgName, paramIndex, formatting)
    }

    private class FoldParametersOnlyStep(
        val context: PsiElement,
        val version: SemanticVersion,
        val projectScope: Boolean,
        val member: MemberReference,
        val srgName: Boolean,
        val paramIndex: Int,
        val formatting: Boolean
    ) : BaseListPopupStep<Boolean>("Should only the parameters of the function be folded?", true, false) {
        override fun getTextFor(value: Boolean) = if (value) "Yes" else "No"

        override fun onChosen(foldParametersOnly: Boolean, finalChoice: Boolean) =
            PrefixSuffixStep(
                context,
                version,
                projectScope,
                member,
                srgName,
                paramIndex,
                formatting,
                foldParametersOnly
            )
    }

    private class PrefixSuffixStep(
        val context: PsiElement,
        val version: SemanticVersion,
        val projectScope: Boolean,
        val member: MemberReference,
        val srgName: Boolean,
        val paramIndex: Int,
        val formatting: Boolean,
        val foldParametersOnly: Boolean
    ) : BaseListPopupStep<Boolean>("Does the function add a prefix or suffix to the key?", true, false) {
        override fun getTextFor(value: Boolean) = if (value) "Yes" else "No"

        override fun onChosen(usePrefixSuffix: Boolean, finalChoice: Boolean): PopupStep<*>? {
            return doFinalStep {
                val prefix = if (usePrefixSuffix) {
                    Messages.showInputDialog(
                        "Enter key prefix:",
                        "Key Prefix",
                        Messages.getQuestionIcon()
                    ) ?: return@doFinalStep
                } else {
                    ""
                }
                val suffix = if (usePrefixSuffix) {
                    Messages.showInputDialog(
                        "Enter key suffix:",
                        "Key Suffix",
                        Messages.getQuestionIcon()
                    ) ?: return@doFinalStep
                } else {
                    ""
                }

                val function = TranslationFunction(
                    member,
                    srgName,
                    paramIndex,
                    prefix,
                    suffix,
                    formatting,
                    foldParametersOnly
                )
                if (projectScope) {
                    TranslationFunctionRepository.addToProjectConfig(context.project, version, function)
                } else {
                    TranslationFunctionRepository.addToGlobalConfig(version, function)
                }
            }
        }
    }

    override fun checkFile(file: PsiFile?) = true

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement) =
        (findCall(element) != null || findMethod(element) != null) && element.findMcpModule() != null

    private fun findCall(element: PsiElement): PsiCall? {
        return (element as? PsiCall) ?: findCall(element.parent ?: return null)
    }

    private fun findMethod(element: PsiElement): PsiMethod? {
        if (element is PsiMethod) {
            return element
        }
        if (element.parent is PsiMethod) {
            return element.parent as PsiMethod
        }
        return null
    }

    override fun getFamilyName() = "Create translation function for method"

    override fun getText() = "Create translation function for method"

    override fun startInWriteAction() = false
}
