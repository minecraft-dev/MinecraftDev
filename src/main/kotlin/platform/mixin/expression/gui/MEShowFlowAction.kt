/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.expression.gui

import com.demonwav.mcdev.platform.mixin.handlers.InjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.reference.MethodReference
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByPsiClass
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.shortDescString
import com.demonwav.mcdev.platform.mixin.util.shortName
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.ifEmpty
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNameValuePair
import com.intellij.psi.util.isAncestor
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodNode

class MEShowFlowAction : AnAction() {

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = resolve(e).isNotEmpty()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val results = resolve(e).ifEmpty { return }

        fun navigate(resolved: Resolved) {
            project.service<MEFlowWindowService>().showDiagram(resolved.clazz, resolved.method, resolved.action)
        }

        results.singleOrNull()?.let { return navigate(it) }

        val step = object : BaseListPopupStep<Resolved>("Choose Target Method", results) {
            override fun onChosen(selectedValue: Resolved, finalChoice: Boolean): PopupStep<*>? {
                return doFinalStep {
                    navigate(selectedValue)
                }
            }
        }

        JBPopupFactory.getInstance().createListPopup(step).showInBestPositionFor(e.dataContext)
    }

    private fun resolve(e: AnActionEvent): List<Resolved> {
        val project = e.project ?: return emptyList()
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return emptyList()
        if (file.language != JavaLanguage.INSTANCE) {
            return emptyList()
        }
        val caret = e.getData(CommonDataKeys.CARET) ?: return emptyList()
        val element = file.findElementAt(caret.offset) ?: return emptyList()
        val psiClass = element.parentOfType<PsiClass>() ?: return emptyList()

        fun resolveMixinMethodString(): Sequence<Resolved> {
            val string = element.parentOfType<PsiLiteralExpression>() ?: return emptySequence()
            return MethodReference.resolve(string)?.map { (clazz, method) ->
                Resolved(clazz, method)
            }.orEmpty()
        }

        fun resolvePsiMethod(): Sequence<Resolved> {
            val identifier = element as? PsiIdentifier ?: return emptySequence()
            val psiMethod = identifier.parent as? PsiMethod ?: return emptySequence()
            val clazz = findClassNodeByPsiClass(psiClass) ?: return emptySequence()
            val desc = psiMethod.descriptor ?: return emptySequence()
            return clazz.methods.asSequence()
                .filter { it.name == psiMethod.name && it.desc == desc }
                .map { Resolved(clazz, it) }
        }

        fun resolveMethodByLine(): Sequence<Resolved> {
            val clazz = findClassNodeByPsiClass(psiClass) ?: return emptySequence()
            val lineNumber = caret.logicalPosition.line + 1
            val methods = clazz.methods.asSequence().filter { method ->
                method.instructions.asSequence()
                    .filterIsInstance<LineNumberNode>()
                    .any { it.line == lineNumber }
            }
            return methods.map { method ->
                Resolved(clazz, method) {
                    it.ui.scrollToLine(lineNumber)
                }
            }
        }

        fun resolveExpressionTarget(): Sequence<Resolved> {
            val module = e.getData(LangDataKeys.MODULE) ?: return emptySequence()
            val string = findExpressionString(element) ?: return emptySequence()
            val modifierList = string.parentOfType<PsiMethod>()?.modifierList ?: return emptySequence()
            val (injectorAnnotation, injector) =
                modifierList.annotations.firstNotNullOfOrNull { ann ->
                    (MixinAnnotationHandler.forMixinAnnotation(ann, project) as? InjectorAnnotationHandler)
                        ?.let { ann to it }
                } ?: return emptySequence()
            return psiClass.mixinTargets.asSequence()
                .flatMap { injector.resolveTarget(injectorAnnotation, it) }
                .filterIsInstance<MethodTargetMember>()
                .map { target ->
                    Resolved(target.classAndMethod.clazz, target.classAndMethod.method) {
                        it.populateMatchStatuses(module, string, modifierList)
                    }
                }
        }

        return buildList {
            if (psiClass.isMixin) {
                addAll(resolveExpressionTarget())
                addAll(resolveMixinMethodString())
            } else {
                addAll(resolvePsiMethod())
                addAll(resolveMethodByLine())
            }
        }
    }

    private fun findExpressionString(anchor: PsiElement): PsiElement? {
        val nameValue = anchor.parentOfType<PsiNameValuePair>() ?: return null
        if (nameValue.name != "value" && nameValue.name != null) {
            // Wrong attribute
            return null
        }
        if (anchor.parentOfType<PsiMethod>()?.modifierList?.hasAnnotation(MixinConstants.MixinExtras.EXPRESSION) != true) {
            // Not an Expression
            return null
        }
        return when (val value = nameValue.value) {
            null -> null
            is PsiArrayInitializerMemberValue -> value.initializers.firstOrNull { it.isAncestor(anchor) }
            else -> value.takeIf { it.isAncestor(anchor) }
        }
    }

    private data class Resolved(val clazz: ClassNode, val method: MethodNode, val action: (FlowDiagram) -> Unit = {}) {
        override fun toString() = "${clazz.shortName}::${method.name}${shortDescString(method.desc)}"
    }
}
