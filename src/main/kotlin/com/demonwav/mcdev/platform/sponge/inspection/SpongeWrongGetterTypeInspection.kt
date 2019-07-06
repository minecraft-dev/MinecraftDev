/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.inspection

import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.platform.sponge.util.resolveSpongeGetterTarget
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.siyeh.ig.BaseInspection
import com.siyeh.ig.BaseInspectionVisitor
import com.siyeh.ig.InspectionGadgetsFix

class SpongeWrongGetterTypeInspection : BaseInspection() {

    override fun getDisplayName() = "Parameter's type is not assignable to its @Getter method return type"

    override fun buildErrorString(vararg infos: Any?) = staticDescription

    override fun getStaticDescription() = "@Getter requires the parameter's type to be the same as the return type of the annotation's target method"

    override fun buildVisitor(): BaseInspectionVisitor {
        return object : BaseInspectionVisitor() {
            override fun visitMethod(method: PsiMethod) {
                if (method.parameters.size < 2 || !method.isValidSpongeListener()) {
                    return
                }

                for (i in 1 until method.parameters.size) {
                    val parameter = method.parameters[i]
                    val getterAnnotation = parameter.getAnnotation(SpongeConstants.GETTER_ANNOTATION) as? PsiAnnotation ?: continue
                    val getterMethod = getterAnnotation.resolveSpongeGetterTarget() ?: return
                    if (getterMethod.returnType != parameter.type) {
                        val getterMethodReturnClass = (getterMethod.returnType as JvmReferenceType).resolve() as? PsiClass ?: continue
                        val paramType = (parameter.type as JvmReferenceType).resolve() as? PsiClass ?: continue
                        if (getterMethodReturnClass.qualifiedName == SpongeConstants.OPTIONAL && getterMethodReturnClass.hasTypeParameters()) {
                            val getterMethodRetRef = getterMethod.returnTypeElement?.type as JvmReferenceType
                            val optionalParamType = getterMethodRetRef.typeArguments().firstOrNull() as? PsiClassType
                            if (optionalParamType != null && paramType.qualifiedName == optionalParamType.canonicalText) {
                                continue
                            }
                        }

                        val paramRefType = (parameter.type as PsiClassReferenceType)
                        registerError(paramRefType.reference, parameter, getterMethod.returnTypeElement!!)
                    }
                }
            }
        }
    }

    override fun buildFixes(vararg infos: Any?): Array<out InspectionGadgetsFix> {
        val param = infos[0] as PsiParameter
        if (!param.isWritable) {
            return InspectionGadgetsFix.EMPTY_ARRAY
        }

        val newType = infos[1] as PsiTypeElement
        val newTypeRef = newType.type as JvmReferenceType
        val newTypeClass = (newTypeRef.resolve() as? PsiClass)?.let {
            if (it.qualifiedName == SpongeConstants.OPTIONAL && newTypeRef.typeArguments().count() > 0) {
                val wrappedType = newTypeRef.typeArguments().first()
                return@let (wrappedType as? PsiClassType)?.resolve()
            }

            return@let it
        } ?: return InspectionGadgetsFix.EMPTY_ARRAY

        val elementFactory = JavaPsiFacade.getElementFactory(param.project)
        val unwrappedNewType = elementFactory.createTypeElement(elementFactory.createType(newTypeClass))
        return arrayOf(UseGetterReturnTypeInspectionGadgetsFix(param, newType, "Set parameter type to ${newType.type.presentableText}"),
                UseGetterReturnTypeInspectionGadgetsFix(param, unwrappedNewType, "Set parameter type to ${unwrappedNewType.type.presentableText}"))
    }
}
