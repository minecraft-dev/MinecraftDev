/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.platform.sponge.util.resolveSpongeGetterTarget
import com.demonwav.mcdev.util.isJavaOptional
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.IntentionAndQuickFixAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.jvm.JvmMethod
import com.intellij.lang.jvm.actions.createChangeParametersActions
import com.intellij.lang.jvm.actions.expectedParameter
import com.intellij.lang.jvm.actions.updateMethodParametersRequest
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiType
import com.intellij.psi.util.TypeConversionUtil
import com.siyeh.ig.InspectionGadgetsFix
import java.util.function.Supplier
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass

class SpongeWrongGetterTypeInspection : AbstractBaseUastLocalInspectionTool() {

    override fun getDisplayName() = "Parameter's type is not assignable to its @Getter method return type"

    override fun getStaticDescription() =
        "@Getter requires the parameter's type to be assignable from the annotation's target method return type"

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val parameters = method.uastParameters
        if (parameters.size < 2 || !method.isValidSpongeListener()) {
            return null
        }

        val eventClassType = parameters.first().type as? PsiClassType
        val resolveEventClassTypeGenerics = eventClassType?.resolveGenerics()
        val eventTypeSubstitutor = resolveEventClassTypeGenerics?.substitutor ?: PsiSubstitutor.EMPTY
        resolveEventClassTypeGenerics?.element?.let { eventTypeSubstitutor.putAll(it, eventClassType.parameters) }

        val problems = mutableListOf<ProblemDescriptor>()
        // We start at 1 because the first parameter is the event
        for (i in 1 until parameters.size) {
            val parameter = parameters[i]
            val getterAnnotation = parameter.findAnnotation(SpongeConstants.GETTER_ANNOTATION) ?: continue

            val getterMethod = getterAnnotation.resolveSpongeGetterTarget() ?: continue

            val getterClass = getterMethod.getContainingUClass() ?: continue
            val eventClass = eventClassType?.resolve() ?: continue
            val getterSubst = TypeConversionUtil.getSuperClassSubstitutor(
                getterClass.javaPsi,
                eventClass,
                eventTypeSubstitutor
            )
            val getterReturnType = getterMethod.returnType?.let(getterSubst::substitute) ?: continue
            val parameterType = parameter.type
            if (getterReturnType.isAssignableFrom(parameterType)) {
                continue
            }

            if (isOptional(getterReturnType)) {
                val getterOptionalType = getFirstGenericType(getterReturnType)
                if (getterOptionalType != null && areInSameHierarchy(getterOptionalType, parameterType)) {
                    continue
                }

                if (getterOptionalType != null && isOptional(parameterType)) {
                    val paramOptionalType = getFirstGenericType(parameterType)
                    if (paramOptionalType != null && areInSameHierarchy(getterOptionalType, paramOptionalType)) {
                        continue
                    }
                }
            }

            // Prefer highlighting the type, but if type is absent use the whole parameter instead
            val typeReference = parameter.typeReference ?: continue
            val location = typeReference.sourcePsi?.takeUnless { it.textRange.isEmpty } ?: continue
            val methodJava = method.javaPsi as JvmMethod
            val fixes = this.createFixes(methodJava, getterReturnType, i, manager.project)
            problems += manager.createProblemDescriptor(
                location,
                this.staticDescription,
                isOnTheFly,
                fixes,
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        }
        return problems.toTypedArray()
    }

    private fun areInSameHierarchy(aType: PsiType, otherType: PsiType): Boolean =
        aType.isAssignableFrom(otherType) || otherType.isAssignableFrom(aType)

    private fun isOptional(type: PsiType): Boolean {
        val typeClass = type as? PsiClassType ?: return false
        return typeClass.isJavaOptional() && typeClass.hasParameters()
    }

    private fun getFirstGenericType(typeElement: PsiType): PsiType? =
        (typeElement as? PsiClassType)?.parameters?.firstOrNull()

    private fun createFixes(
        method: JvmMethod,
        expectedType: PsiType,
        paramIndex: Int,
        project: Project
    ): Array<out LocalQuickFix> {
        if (expectedType is PsiPrimitiveType ||
            expectedType is PsiClassType && !isOptional(expectedType)
        ) {
            // The getter does not return an Optional, simply suggest the return type
            return arrayOf(Fix(method, paramIndex, expectedType))
        }

        val elementFactory = JavaPsiFacade.getElementFactory(project)

        val expectedClassType = expectedType as? PsiClassType
            ?: return InspectionGadgetsFix.EMPTY_ARRAY
        val fixedClassType = if (isOptional(expectedClassType)) {
            val wrappedType = expectedClassType.parameters.first()
            val resolveResult = (wrappedType as? PsiClassType)?.resolveGenerics()
                ?: return InspectionGadgetsFix.EMPTY_ARRAY
            val element = resolveResult.element
                ?: return InspectionGadgetsFix.EMPTY_ARRAY
            elementFactory.createType(element, resolveResult.substitutor)
        } else {
            val resolvedClass = expectedClassType.resolve()
                ?: return InspectionGadgetsFix.EMPTY_ARRAY
            elementFactory.createType(resolvedClass)
        }

        // Suggest a non-Optional version too
        return arrayOf(
            Fix(method, paramIndex, expectedType),
            Fix(method, paramIndex, fixedClassType)
        )
    }

    private class Fix(
        method: JvmMethod,
        val paramIndex: Int,
        val expectedType: PsiType,
    ) : IntentionAndQuickFixAction() {

        private val myText: String = "Set parameter type to ${expectedType.presentableText}"
        private val methodPointer = Supplier<JvmMethod?> { method }

        override fun applyFix(project: Project, file: PsiFile, editor: Editor?) {
            val changeParamsRequest = updateMethodParametersRequest(methodPointer) { actualParams ->
                val existingParam = actualParams[paramIndex]
                val newParam = expectedParameter(
                    expectedType,
                    existingParam.semanticNames.first(),
                    existingParam.expectedAnnotations
                )
                actualParams.toMutableList().also { it[paramIndex] = newParam }
            }
            val waw = createChangeParametersActions(methodPointer.get()!!, changeParamsRequest).firstOrNull() ?: return
            waw.invoke(project, editor, file)
        }

        override fun getName(): String = myText
        override fun getFamilyName(): String = myText
    }
}
