/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2020 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.sideonly

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.fabric.util.FabricConstants
import com.demonwav.mcdev.platform.forge.util.ForgeConstants
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.packageName
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.dataFlow.StandardDataFlowRunner
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiField
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiModifierListOwner
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiResolveHelper
import com.intellij.psi.PsiType
import com.intellij.psi.util.parentOfType

object SideOnlyUtil {
    const val MCDEV_SIDEONLY_ANNOTATION = "com.demonwav.mcdev.annotations.CheckEnv"
    const val MCDEV_SIDE = "com.demonwav.mcdev.annotations.Env"

    internal fun getExplicitAnnotation(element: PsiModifierListOwner, hardness: SideHardness): SideInstance? {
        if (hardness != SideHardness.HARD) {
            val mcDevAnnotation = element.findAnnotation(MCDEV_SIDEONLY_ANNOTATION)
            if (mcDevAnnotation != null) {
                val side = mcDevAnnotation.findAttributeValue("value") as? PsiReferenceExpression
                    ?: return null
                val sideConstant = side.resolve() as? PsiEnumConstant ?: return null
                return when (sideConstant.name) {
                    "CLIENT" -> SideInstance.createMcDev(Side.CLIENT, element)
                    "SERVER" -> SideInstance.createMcDev(Side.SERVER, element)
                    else -> null
                }
            }
        }

        if (hardness != SideHardness.SOFT) {
            val sideOnlyAnnotation = element.findAnnotation(ForgeConstants.SIDE_ONLY_ANNOTATION)
            if (sideOnlyAnnotation != null) {
                val side = sideOnlyAnnotation.findAttributeValue("value") as? PsiReferenceExpression
                    ?: return null
                val sideConstant = side.resolve() as? PsiEnumConstant ?: return null
                return when (sideConstant.name) {
                    "CLIENT" -> SideInstance.createSideOnly(Side.CLIENT, element)
                    "SERVER" -> SideInstance.createSideOnly(Side.SERVER, element)
                    else -> null
                }
            }

            val environmentAnnotation = element.findAnnotation(FabricConstants.ENVIRONMENT_ANNOTATION)
            if (environmentAnnotation != null) {
                val env = environmentAnnotation.findAttributeValue("value") as? PsiReferenceExpression
                    ?: return null
                val envConstant = env.resolve() as? PsiEnumConstant ?: return null
                return when (envConstant.name) {
                    "CLIENT" -> SideInstance.createEnvironment(Side.CLIENT, element)
                    "SERVER" -> SideInstance.createEnvironment(Side.SERVER, element)
                    else -> null
                }
            }

            val onlyInAnnotation = element.annotations.firstOrNull {
                it.hasQualifiedName(ForgeConstants.ONLY_IN_ANNOTATION) &&
                    it.findAttributeValue("_interface") == null
            }
            if (onlyInAnnotation != null) {
                val dist = onlyInAnnotation.findAttributeValue("value") as? PsiReferenceExpression
                    ?: return null
                val distConstant = dist.resolve() as? PsiEnumConstant ?: return null
                return when (distConstant.name) {
                    "CLIENT" -> SideInstance.createOnlyIn(Side.CLIENT, element)
                    "DEDICATED_SERVER" -> SideInstance.createOnlyIn(Side.SERVER, element)
                    else -> null
                }
            }
        }

        return null
    }

    internal fun getExplicitOrInferredAnnotation(element: PsiModifierListOwner, hardness: SideHardness): SideInstance? {
        val explicitAnnotation = getExplicitAnnotation(element, hardness)
        if (explicitAnnotation != null) {
            return explicitAnnotation
        }

        return when (element) {
            is PsiClass -> getInferredClassAnnotation(element, hardness)
            is PsiMethod -> getInferredMethodAnnotation(element, hardness)
            else -> null
        }
    }

    private fun getInferredClassAnnotation(cls: PsiClass, hardness: SideHardness): SideInstance? {
        // If mixing into a client class, the mixin is also clientside
        if (cls.isMixin) {
            val side = PsiResolveHelper.ourGraphGuard.doPreventingRecursion(cls, true) preventRecursion@{
                var ret = Side.BOTH
                for (target in cls.mixinTargets) {
                    val newSide = getExplicitOrInferredAnnotation(target, hardness)?.side ?: Side.BOTH
                    if (newSide != Side.BOTH) {
                        if (ret != Side.BOTH && newSide != ret) {
                            return@preventRecursion Side.BOTH
                        }
                        ret = newSide
                    }
                }
                return@preventRecursion ret
            } ?: Side.BOTH
            return if (side == Side.BOTH) {
                null
            } else {
                SideInstance.createImplicitMcDev(side, cls)
            }
        }

        // Inherit side from superclass
        val superSide = PsiResolveHelper.ourGraphGuard.doPreventingRecursion(cls, true) {
            cls.superClass?.let { getExplicitOrInferredAnnotation(it, hardness) }
        }?.side ?: Side.BOTH
        if (superSide != Side.BOTH) {
            return SideInstance.createImplicitMcDev(superSide, cls)
        }

        // Inner classes inherit their side from outer classes
        val parentSide = PsiResolveHelper.ourGraphGuard.doPreventingRecursion(cls, true) {
            cls.parentOfType<PsiClass>()?.let { getExplicitOrInferredAnnotation(it, hardness) }
        }?.side ?: Side.BOTH
        if (parentSide != Side.BOTH) {
            return SideInstance.createImplicitMcDev(parentSide, cls)
        }

        if (hardness != SideHardness.HARD) {
            // Inherit from the side of the whole mod
            val fabricModule = cls.findModule()?.let { module ->
                MinecraftFacet.getInstance(module)?.getModuleOfType(FabricModuleType)
            }
            if (fabricModule != null) {
                val fabricModJson = fabricModule.fabricJson?.let {
                    PsiManager.getInstance(cls.project).findFile(it)
                } as? JsonFile
                val jsonObj = fabricModJson?.topLevelValue as? JsonObject
                val side = (jsonObj?.findProperty("environment")?.value as? JsonStringLiteral)?.value ?: "*"
                if (side == "client") {
                    return SideInstance.createImplicitMcDev(Side.CLIENT, cls)
                } else if (side == "server") {
                    return SideInstance.createImplicitMcDev(Side.SERVER, cls)
                }
            }
        }

        return null
    }

    private fun getInferredMethodAnnotation(method: PsiMethod, hardness: SideHardness): SideInstance? {
        if (hardness == SideHardness.HARD) {
            return null
        }

        // Inherit side from super method
        val superSide = PsiResolveHelper.ourGraphGuard.doPreventingRecursion(method, true) preventRecursion@{
            var side = Side.BOTH
            for (superMethod in method.findSuperMethods()) {
                val newSide = getExplicitOrInferredAnnotation(superMethod, SideHardness.EITHER)?.side ?: Side.BOTH
                if (newSide != Side.BOTH) {
                    if (side != Side.BOTH && newSide != side) {
                        return@preventRecursion Side.BOTH
                    }
                    side = newSide
                }
            }
            return@preventRecursion side
        } ?: Side.BOTH
        if (superSide != Side.BOTH) {
            return SideInstance.createImplicitMcDev(superSide, method)
        }

        return null
    }

    private fun getDist(expression: PsiExpression): Side {
        if (expression.type?.equalsToText(ForgeConstants.DIST_ANNOTATION) != true) {
            return Side.BOTH
        }
        val field = (expression as? PsiReferenceExpression)?.resolve() as? PsiField ?: return Side.BOTH
        if (field.containingClass?.qualifiedName == ForgeConstants.DIST_ANNOTATION) {
            return when (field.name) {
                "CLIENT" -> Side.CLIENT
                "DEDICATED_SERVER" -> Side.SERVER
                else -> Side.BOTH
            }
        }
        return Side.BOTH
    }

    private fun createDistExecutorSide(side: Side, lambda: PsiLambdaExpression): SideInstance? {
        return when (side) {
            Side.BOTH -> null
            else -> SideInstance.createDistExecutor(side, lambda)
        }
    }

    private fun getDistExecutorSide(
        methodCall: PsiMethodCallExpression,
        method: PsiMethod,
        lambda: PsiLambdaExpression
    ): SideInstance? {
        val args = methodCall.argumentList.expressions
        val lambdaIndex = args.indexOf(lambda)
        if (lambdaIndex == -1) {
            return null
        }
        return when (method.name) {
            "callWhenOn",
            "unsafeCallWhenOn",
            "safeCallWhenOn",
            "runWhenOn",
            "unsafeRunWhenOn",
            "safeRunWhenOn" -> createDistExecutorSide(getDist(args[0]), lambda)
            "runForDist",
            "unsafeRunForDist",
            "safeRunForDist" -> {
                val side = if (lambdaIndex == 0) {
                    Side.CLIENT
                } else {
                    Side.SERVER
                }
                createDistExecutorSide(side, lambda)
            }
            else -> null
        }
    }

    private fun getPackageSide(cls: PsiClass, hardness: SideHardness): SideInstance? {
        // hard side annotations can't be put on packages
        if (hardness == SideHardness.HARD) {
            return null
        }

        val packageName = cls.packageName ?: return null
        var pkg = JavaPsiFacade.getInstance(cls.project).findPackage(packageName)
        while (pkg != null) {
            val side = getExplicitAnnotation(pkg, SideHardness.SOFT)
            if (side != null) {
                return side
            }
            pkg = pkg.parentPackage
        }

        return null
    }

    private fun getLambdaContextSide(element: PsiLambdaExpression, hardness: SideHardness): SideInstance? {
        val methodCall = element.parentOfType<PsiMethodCallExpression>()
        val method = methodCall?.resolveMethod()
        if (method?.hasModifierProperty(PsiModifier.STATIC) == true &&
            method.containingClass?.qualifiedName == ForgeConstants.DIST_EXECUTOR
        ) {
            val distExecutorSide = getDistExecutorSide(methodCall, method, element)
            if (distExecutorSide != null) {
                return distExecutorSide
            }
        }
        if (hardness != SideHardness.HARD) {
            val surroundingMethod = element.parentOfType(PsiMethod::class, PsiClass::class)
            if (surroundingMethod is PsiMethod) {
                val side = getContextSide(surroundingMethod, SideHardness.EITHER)
                if (side != null) {
                    return SideInstance.createImplicitMcDev(side.side, element)
                }
            }
        }
        return getContextSide(element.parentOfType<PsiClass>(), hardness)
    }

    fun getContextSide(element: PsiElement?, hardness: SideHardness): SideInstance? {
        element ?: return null
        return when (element) {
            is PsiLambdaExpression -> getLambdaContextSide(element, hardness)
            is PsiMethod ->
                getExplicitOrInferredAnnotation(element, hardness) ?: getContextSide(element.parent, hardness)
            is PsiClass -> getExplicitOrInferredAnnotation(element, hardness) ?: getPackageSide(element, hardness)
            else -> getContextSide(element.parent, hardness)
        }
    }

    fun getSidedInterfaces(clazz: PsiClass): Map<String, Side> {
        val interfaces = mutableMapOf<String, Side>()
        for (annotation in clazz.annotations) {
            if (annotation.hasQualifiedName(FabricConstants.ENVIRONMENT_INTERFACE_ANNOTATION)) {
                val value = annotation.findAttributeValue("value") as? PsiReferenceExpression
                    ?: continue
                val itf = annotation.findAttributeValue("itf") as? PsiClassObjectAccessExpression
                    ?: continue
                val itfFqn = (itf.operand.type as? PsiClassType)?.resolve()?.qualifiedName ?: continue
                val envConstant = value.resolve() as? PsiEnumConstant ?: continue
                when (envConstant.name) {
                    "CLIENT" -> interfaces[itfFqn] = Side.CLIENT
                    "SERVER" -> interfaces[itfFqn] = Side.SERVER
                }
            } else if (annotation.hasQualifiedName(ForgeConstants.ONLY_IN_ANNOTATION)) {
                val value = annotation.findAttributeValue("value") as? PsiReferenceExpression
                    ?: continue
                val itf = annotation.findAttributeValue("_interface") as? PsiClassObjectAccessExpression
                    ?: continue
                val itfFqn = (itf.operand.type as? PsiClassType)?.resolve()?.qualifiedName ?: continue
                val sideConstant = value.resolve() as? PsiEnumConstant ?: continue
                when (sideConstant.name) {
                    "CLIENT" -> interfaces[itfFqn] = Side.CLIENT
                    "DEDICATED_SERVER" -> interfaces[itfFqn] = Side.SERVER
                }
            }
        }
        return interfaces
    }

    fun analyzeBodyForSoftSideProblems(body: PsiElement, problems: ProblemsHolder) {
        val runner = StandardDataFlowRunner()
        val factory = runner.factory
        val visitor = SoftSideOnlyInstructionVisitor(body, factory, problems)
        runner.analyzeMethod(body, visitor)
    }

    fun createInspectionMessage(from: SideInstance?, to: SideInstance): String {
        val toType = getElementTypeName(to.element)
        if (from == null) {
            return "Cannot access $toType ${to.reason} from common code"
        }
        val fromType = getElementTypeName(from.element)
        return "Cannot access $toType ${to.reason} from $fromType ${from.reason}"
    }

    private fun getElementTypeName(element: PsiElement): String {
        return when (element) {
            is PsiPackage -> "package"
            is PsiClass -> when {
                element.isInterface -> "interface"
                element.isEnum -> "enum"
                element.isAnnotationType -> "@interface"
                else -> "class"
            }
            is PsiField -> "field"
            is PsiMethod -> when {
                element.isConstructor -> "constructor"
                else -> "method"
            }
            is PsiLambdaExpression -> "lambda"
            else -> "element"
        }
    }

    fun getClassInType(type: PsiType): PsiClass? {
        return when (type) {
            is PsiClassType -> type.resolve()
            is PsiArrayType -> getClassInType(type.deepComponentType)
            else -> null
        }
    }
}
