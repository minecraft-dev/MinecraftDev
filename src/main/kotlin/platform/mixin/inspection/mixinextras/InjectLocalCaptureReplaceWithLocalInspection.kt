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

package com.demonwav.mcdev.platform.mixin.inspection.mixinextras

import com.demonwav.mcdev.platform.mixin.handlers.InsnInjectorAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.inspection.fix.AnnotationAttributeFix
import com.demonwav.mcdev.platform.mixin.inspection.injector.UnusedLocalCaptureInspection
import com.demonwav.mcdev.platform.mixin.util.LocalVariables
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.hasNamedLocalVariables
import com.demonwav.mcdev.util.addAnnotation
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

class InjectLocalCaptureReplaceWithLocalInspection : MixinInspection() {
    override fun getStaticDescription() =
        "Reports when @Inject local capture can be replaced with @Local, which is less brittle"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor {
        val localClass = JavaPsiFacade.getInstance(holder.project)
            .findClass(MixinConstants.MixinExtras.LOCAL, GlobalSearchScope.allScope(holder.project))
        if (localClass == null) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : JavaElementVisitor() {
            override fun visitAnnotation(annotation: PsiAnnotation) {
                if (!annotation.hasQualifiedName(MixinConstants.Annotations.INJECT)) {
                    return
                }

                // check that we are capturing locals
                val localsValue =
                    PsiUtil.skipParenthesizedExprDown(
                        annotation.findDeclaredAttributeValue("locals") as? PsiExpression
                    ) as? PsiReferenceExpression ?: return
                if (localsValue.referenceName != "CAPTURE_FAILHARD") {
                    return
                }
                val enumName = (localsValue.resolve() as? PsiEnumConstant)?.containingClass?.qualifiedName
                if (enumName != MixinConstants.Classes.LOCAL_CAPTURE) {
                    return
                }

                val method = annotation.findContainingMethod() ?: return

                if (OverridingMethodsSearch.search(method).any()) {
                    return
                }

                // find the start of the locals in the parameter list
                val parameters = method.parameterList.parameters
                val callbackInfoIndex = UnusedLocalCaptureInspection.Util.findCallbackInfoParam(parameters)
                if (callbackInfoIndex == -1) {
                    return
                }

                // resolve the local variables at the targets
                val handler = MixinAnnotationHandler.forMixinAnnotation(MixinConstants.Annotations.INJECT)
                    as InsnInjectorAnnotationHandler
                val module = annotation.findModule() ?: return
                val localsAndParamCountsAtTargets = handler.resolveInstructions(annotation).map { result ->
                    val locals = LocalVariables.getLocals(
                        module,
                        result.method.clazz,
                        result.method.method,
                        result.result.insn
                    ) ?: return
                    var paramCount = Type.getMethodType(result.method.method.desc).argumentTypes.size
                    if (!result.method.method.hasAccess(Opcodes.ACC_STATIC)) {
                        paramCount++
                    }
                    locals to paramCount
                }

                val hasNamedLocalVariables = MixinAnnotationHandler.resolveTarget(annotation).all {
                    it is MethodTargetMember &&
                        annotation.hasNamedLocalVariables(it.classAndMethod.clazz.name.replace('/', '.'))
                }

                // based on the resolved local variables, figure out what @Local specifiers to use
                val localSpecifiers = parameters.drop(callbackInfoIndex + 1).withIndex().map { (index, param) ->
                    val isLocalUsed = ReferencesSearch.search(param).anyMatch {
                        !it.isSoft && !PsiTreeUtil.isAncestor(param, it.element, false)
                    }
                    if (!isLocalUsed) {
                        return@map UnusedSpecifier
                    }

                    if (hasNamedLocalVariables) {
                        var isUnnamed = false
                        var name: String? = null
                        for ((localsAtTarget, paramCount) in localsAndParamCountsAtTargets) {
                            val local = localsAtTarget.filterNotNull().getOrNull(index + paramCount) ?: run {
                                isUnnamed = true
                                break
                            }

                            if (!local.isNamed) {
                                isUnnamed = true
                                break
                            }

                            if (name != null && local.name != name) {
                                isUnnamed = true
                                break
                            }

                            name = local.name
                        }

                        if (!isUnnamed && name != null) {
                            return@map NameSpecifier(name)
                        }
                    }

                    val localType = param.type.descriptor
                    val canBeImplicit = localsAndParamCountsAtTargets.all { (localsAtTarget, _) ->
                        localsAtTarget.singleOrNull { it?.desc == localType } != null
                    }
                    if (canBeImplicit) {
                        return@map ImplicitSpecifier
                    }

                    val ordinals = localsAndParamCountsAtTargets.map { (localsAtTarget, paramCount) ->
                        localsAtTarget.filterNotNull().take(index + paramCount).count { it.desc == localType }
                    }
                    if (ordinals.isEmpty()) {
                        return
                    }
                    if (ordinals.all { it == ordinals.first() }) {
                        return@map OrdinalSpecifier(ordinals.first())
                    }

                    val indexes = localsAndParamCountsAtTargets.map { (localsAtTarget, paramCount) ->
                        localsAtTarget.filterNotNull().getOrNull(index + paramCount)?.index ?: return
                    }
                    if (indexes.isEmpty()) {
                        return
                    }
                    if (indexes.all { it == indexes.first() }) {
                        return@map IndexSpecifier(indexes.first())
                    }

                    return
                }

                if (localSpecifiers.isEmpty() || localSpecifiers.all { it is UnusedSpecifier }) {
                    // this is reported by the redundant local capture inspection
                    return
                }

                holder.registerProblem(
                    localsValue,
                    "@Inject local capture can be replaced by @Local",
                    ReplaceWithLocalFix(annotation, callbackInfoIndex, localSpecifiers)
                )
            }
        }
    }

    private sealed interface LocalSpecifier
    private data class OrdinalSpecifier(val ordinal: Int) : LocalSpecifier
    private data class IndexSpecifier(val index: Int) : LocalSpecifier
    private data class NameSpecifier(val name: String) : LocalSpecifier
    private data object ImplicitSpecifier : LocalSpecifier
    private data object UnusedSpecifier : LocalSpecifier

    private class ReplaceWithLocalFix(
        injectAnnotation: PsiAnnotation,
        private val callbackInfoIndex: Int,
        @SafeFieldForPreview private val localSpecifiers: List<LocalSpecifier>
    ) : LocalQuickFixOnPsiElement(injectAnnotation) {
        override fun getFamilyName() = "Replace @Inject local capture with @Local"

        override fun getText() = "Replace @Inject local capture with @Local"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val injectAnnotation = startElement as? PsiAnnotation ?: return
            val method = injectAnnotation.findContainingMethod() ?: return
            val paramsForLocals = method.parameterList.parameters.asSequence().drop(callbackInfoIndex + 1)
            for ((param, specifier) in paramsForLocals.zip(localSpecifiers.asSequence())) {
                val localAnnotationText = when (specifier) {
                    ImplicitSpecifier -> "@${MixinConstants.MixinExtras.LOCAL}"
                    is IndexSpecifier -> "@${MixinConstants.MixinExtras.LOCAL}(index = ${specifier.index})"
                    is OrdinalSpecifier -> "@${MixinConstants.MixinExtras.LOCAL}(ordinal = ${specifier.ordinal})"
                    is NameSpecifier -> "@${MixinConstants.MixinExtras.LOCAL}(name = \"${StringUtil.escapeStringCharacters(specifier.name)}\")"
                    is UnusedSpecifier -> {
                        param.delete()
                        continue
                    }
                }
                param.addAnnotation(localAnnotationText)
            }
            AnnotationAttributeFix(injectAnnotation, "locals" to null).applyFix()
        }
    }
}
