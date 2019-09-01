/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.platform.sponge.util.resolveSpongeGetterTarget
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix

class SpongeWrongGetterTypeInspection : BaseInspection() {

    override fun getDisplayName() = "Parameter's type is not assignable to its @Getter method return type"

    override fun buildErrorString(vararg infos: Any?) = staticDescription

    override fun getStaticDescription() =
        "@Getter requires the parameter's type to be assignable from the annotation's target method return type"

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitMethod(method: PsiMethod) {
                if (method.parameters.size < 2 || !method.isValidSpongeListener()) {
                    return
                }

                // We start at 1 because the first parameter is the event
                for (i in 1 until method.parameters.size) {
                    val parameter = method.parameterList.parameters[i]
                    val getterAnnotation =
                        parameter.getAnnotation(SpongeConstants.GETTER_ANNOTATION) ?: continue

                    val getterMethod = getterAnnotation.resolveSpongeGetterTarget() ?: continue
                    val getterReturnType = getterMethod.returnType ?: continue
                    val parameterType = parameter.type
                    if (getterReturnType.isAssignableFrom(parameterType)) {
                        continue
                    }

                    if (isOptional(getterReturnType)) {
                        val getterOptionalType = getterMethod.returnTypeElement?.let(::getFirstGenericType)
                        if (getterOptionalType != null && areInSameHierarchy(getterOptionalType, parameterType)) {
                            continue
                        }

                        if (getterOptionalType != null && isOptional(parameterType)) {
                            val paramOptionalType = parameter.typeElement?.let(::getFirstGenericType)
                            if (paramOptionalType != null
                                && areInSameHierarchy(getterOptionalType, paramOptionalType)
                            ) {
                                continue
                            }
                        }
                    }

                    registerError(parameter.typeElement ?: parameter, parameter, getterMethod.returnTypeElement!!)
                }
            }
        }
    }

    private fun areInSameHierarchy(aType: PsiType, otherType: PsiType): Boolean =
        aType.isAssignableFrom(otherType) || otherType.isAssignableFrom(aType)

    private fun isOptional(type: PsiType): Boolean {
        if (type is PsiPrimitiveType) {
            return false
        }

        val typeClass = (type as? JvmReferenceType)?.resolve() as? PsiClass ?: return false
        return typeClass.qualifiedName == SpongeConstants.OPTIONAL && typeClass.hasTypeParameters()
    }

    private fun getFirstGenericType(typeElement: PsiTypeElement): PsiType? {
        val paramRefType = typeElement.type as? JvmReferenceType ?: return null
        return paramRefType.typeArguments().firstOrNull() as? PsiType
    }

    override fun buildFixes(vararg infos: Any?): Array<out InspectionGadgetsFix> {
        val param = infos[0] as PsiParameter
        if (!param.isWritable) {
            return InspectionGadgetsFix.EMPTY_ARRAY
        }

        val newTypeElement = infos[1] as PsiTypeElement
        val newType = newTypeElement.type
        if (newType is PsiPrimitiveType || newType is PsiClassType && newType.hasParameters() && newType.fullQualifiedName != SpongeConstants.OPTIONAL) {
            return arrayOf(createFix(param, newTypeElement))
        }

        val elementFactory = JavaPsiFacade.getElementFactory(param.project)

        val newTypeRef = newTypeElement.type as? JvmReferenceType
        val newClassType = (newTypeRef?.resolve() as? PsiClass)?.let {
            if (it.qualifiedName == SpongeConstants.OPTIONAL && newTypeRef.typeArguments().count() > 0) {
                val wrappedType = newTypeRef.typeArguments().first()
                val resolveResult = (wrappedType as? PsiClassType)?.resolveGenerics() ?: return@let null
                val element = resolveResult.element ?: return@let null
                return@let elementFactory.createType(element, resolveResult.substitutor)
            }

            return@let elementFactory.createType(it)
        } ?: return InspectionGadgetsFix.EMPTY_ARRAY

        val unwrappedNewTypeElement = elementFactory.createTypeElement(newClassType)
        return arrayOf(
            createFix(param, newTypeElement),
            createFix(param, unwrappedNewTypeElement)
        )
    }

    private fun createFix(parameter: PsiParameter, typeElement: PsiTypeElement): InspectionGadgetsFix =
        UseGetterReturnTypeInspectionGadgetsFix(
            parameter,
            typeElement,
            "Set parameter type to ${typeElement.type.presentableText}"
        )
}
