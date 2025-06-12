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

package com.demonwav.mcdev.platform.mixin.handlers.desugar

import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.mapToArray
import com.demonwav.mcdev.util.normalize
import com.demonwav.mcdev.util.removeWildcards
import com.demonwav.mcdev.util.signature
import com.demonwav.mcdev.util.toObjectType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.CommonClassNames
import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.LambdaUtil
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiArrayInitializerExpression
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassInitializer
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiCodeBlock
import com.intellij.psi.PsiConditionalExpression
import com.intellij.psi.PsiDiamondType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFunctionalExpression
import com.intellij.psi.PsiIfStatement
import com.intellij.psi.PsiIntersectionType
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiResolveHelper
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.PsiSwitchExpression
import com.intellij.psi.PsiSwitchLabelStatement
import com.intellij.psi.PsiSwitchStatement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.infos.MethodCandidateInfo
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.psi.util.PsiSuperMethodUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.TypeConversionUtil
import com.intellij.psi.util.parentOfType
import com.siyeh.ig.psiutils.SwitchUtils
import com.siyeh.ig.psiutils.VariableNameGenerator
import java.lang.invoke.LambdaMetafactory
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

object LambdaDesugarer : Desugarer() {
    private const val METAFACTORY_DESC = "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
    private const val ALT_METAFACTORY_DESC = "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"

    override fun desugar(project: Project, file: PsiJavaFile, context: DesugarContext) {
        val expressionsToDesugar = mutableListOf<PsiExpression>()
        val lambdaNames = mutableMapOf<PsiLambdaExpression, String>()
        val lambdaCounts = mutableMapOf<Pair<PsiClass, String>, Int>()

        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitLambdaExpression(expression: PsiLambdaExpression) {
                super.visitLambdaExpression(expression)

                val containingMethod = PsiTreeUtil.getParentOfType(expression, PsiMethod::class.java, PsiClassInitializer::class.java)
                val containingMethodName = when (containingMethod) {
                    is PsiMethod -> if (containingMethod.isConstructor) "new" else containingMethod.name
                    is PsiClassInitializer -> if (containingMethod.hasModifierProperty(PsiModifier.STATIC)) "static" else "new"
                    else -> return
                }
                val containingClass = containingMethod.containingClass ?: return

                val functionalInterfaceType = getFunctionalInterfaceType(expression)
                val serializableLambda = functionalInterfaceType != null && isSerializable(project, functionalInterfaceType)

                var lambdaMethodName = "lambda$$containingMethodName"
                if (serializableLambda) {
                    val lambdaDisambiguation = getLambdaDisambiguation(expression, functionalInterfaceType)
                    lambdaMethodName += "$${Integer.toHexString(lambdaDisambiguation.hashCode())}"
                }

                // for some reason serializable lambda counters start at 1 while others start at 0
                val lambdaCount = lambdaCounts[containingClass to lambdaMethodName] ?: if (serializableLambda) 1 else 0
                lambdaCounts[containingClass to lambdaMethodName] = lambdaCount + 1

                lambdaNames[expression] = "$lambdaMethodName$$lambdaCount"
                expressionsToDesugar += expression
            }

            override fun elementFinished(element: PsiElement) {
                if (element is PsiMethodReferenceExpression) {
                    expressionsToDesugar += element
                }
            }
        })

        val serializationDatas = mutableMapOf<PsiClass, MutableMap<String, MutableList<LambdaSerializationData>>>()
        for (expr in expressionsToDesugar.asReversed()) {
            val containingClass = expr.findContainingClass() ?: continue
            val serializationData = when (expr) {
                is PsiLambdaExpression -> desugarLambda(project, expr, lambdaNames[expr]!!)
                is PsiMethodReferenceExpression -> desugarMethodReference(project, expr, context)
                else -> throw IllegalStateException("Can only desugar lambdas and method references")
            }
            if (serializationData != null) {
                serializationDatas.getOrPut(containingClass) { linkedMapOf() }
                    .getOrPut(serializationData.implMethodName) { mutableListOf() } += serializationData
            }
        }

        for ((containingClass, datas) in serializationDatas) {
            if (datas.isNotEmpty()) {
                containingClass.add(createDeserializeLambdaMethod(project, datas))
            }
        }
    }

    private fun getLambdaDisambiguation(lambda: PsiLambdaExpression, functionalInterfaceType: PsiType): String {
        val functionalInterface =
            (normalizeFunctionalInterfaceType(functionalInterfaceType) as? PsiClassType)?.resolve() ?: return ""
        val originalLambda = DesugarUtil.getOriginalElement(lambda) ?: lambda
        val method = originalLambda.findContainingMethod()

        val builder = StringBuilder()
        if (method != null) {
            // the throws declaration seems to be missing from the lambda disambiguation signature for some reason
            builder.append(method.signature.substringBefore('^')).append(':')
        } else {
            // class initializers, field initializers, etc, seem to have this signature, even when the only constructor is otherwise
            builder.append("()V:")
        }

        builder.append(functionalInterface.fullQualifiedName).append(' ')

        val varDecl = PsiTreeUtil.getParentOfType(lambda, PsiVariable::class.java, true, PsiClass::class.java)
        if (varDecl != null) {
            builder.append(varDecl.name).append('=')
        }

        val capturedVariables = getCapturedVariables(lambda, getMethodReferenceQualifier(lambda))
        for (capturedVar in capturedVariables) {
            builder.append(capturedVar.type.signature).append(' ').append(capturedVar.name).append(',')
        }

        return builder.toString()
    }

    private fun desugarLambda(project: Project, lambda: PsiLambdaExpression, name: String): LambdaSerializationData? {
        val lambdaBody = lambda.body ?: return null
        val containingClass = lambda.findContainingClass() ?: return null
        val functionalInterfaceType = getFunctionalInterfaceType(lambda) ?: return null
        val functionalInterfaceMethods = getFunctionalInterfaceMethods(functionalInterfaceType).ifEmpty { return null }
        val (functionalInterfaceMethod, functionalInterfaceSignature) = functionalInterfaceMethods.first()
        val functionalReturnType =
            functionalInterfaceSignature.substitutor.substitute(functionalInterfaceMethod.returnType) ?: return null

        val methodReferenceQualifier = getMethodReferenceQualifier(lambda)

        val capturesThis = DesugarUtil.elementNeedsThis(containingClass, lambda)
        val capturedVariables = getCapturedVariables(lambda, methodReferenceQualifier)
        val extraTypesToCheckForTypeParams = mutableListOf<PsiType>()

        val paramsToAdd = mutableListOf<PsiParameter>()

        val factory = JavaPsiFacade.getElementFactory(project)

        if (methodReferenceQualifier != null) {
            val selfType = methodReferenceQualifier.type ?: return null
            val selfName = VariableNameGenerator(lambdaBody, VariableKind.PARAMETER)
                .byName("self")
                .generate(true)
            val newParam = factory.createParameter(selfName, selfType)
            DesugarUtil.setUnnamedVariable(newParam, true)
            paramsToAdd += newParam
        }

        for (capturedVar in capturedVariables) {
            val name = capturedVar.name ?: continue
            paramsToAdd += factory.createParameter(name, capturedVar.type)
        }

        if (lambda.hasFormalParameterTypes()) {
            paramsToAdd += lambda.parameterList.parameters
            for (param in lambda.parameterList.parameters) {
                extraTypesToCheckForTypeParams += param.type
            }
        } else {
            for ((param, paramType) in lambda.parameterList.parameters.zip(functionalInterfaceSignature.parameterTypes)) {
                extraTypesToCheckForTypeParams += paramType
                val newParam = factory.createParameter(param.name, paramType.removeWildcards())
                DesugarUtil.setOriginalElement(newParam, DesugarUtil.getOriginalElement(param))
                DesugarUtil.setUnnamedVariable(newParam, DesugarUtil.isUnnamedVariable(param))
                paramsToAdd += newParam
            }
        }
        val typeParametersToCopy = DesugarUtil.getTypeParametersToCopy(
            lambda,
            containingClass,
            capturedVariables,
            extraTypesToCheckForTypeParams
        )

        val lambdaMethodText = buildString {
            append("private ")
            if (!capturesThis) {
                append("static ")
            }
            if (typeParametersToCopy.isNotEmpty()) {
                append("<T> ")
            }
            append("void ")
            append(name)
            append("() {}")
        }

        val lambdaMethod = containingClass.add(factory.createMethodFromText(lambdaMethodText, null))
            as PsiMethod
        DesugarUtil.setOriginalElement(lambdaMethod, DesugarUtil.getOriginalElement(lambda))
        lambdaMethod.returnTypeElement!!.replace(factory.createTypeElement(functionalReturnType.removeWildcards()))

        if (typeParametersToCopy.isNotEmpty()) {
            val typeParamList = lambdaMethod.typeParameterList!!
            for (typeParam in typeParametersToCopy) {
                typeParamList.add(typeParam)
            }
            typeParamList.typeParameters.first().delete() // delete the T
        }

        val paramList = lambdaMethod.parameterList
        for (param in paramsToAdd) {
            paramList.add(param)
        }

        val capturedTypes = mutableListOf<PsiType>()
        if (capturesThis) {
            capturedTypes += factory.createType(containingClass)
        } else {
            methodReferenceQualifier?.type?.let { capturedTypes += it }
        }
        capturedVariables.mapTo(capturedTypes) { it.type }
        val (indyCall, serializationData) = createIndyCall(
            project,
            containingClass,
            lambda,
            capturedTypes,
            lambdaMethod
        ) ?: return null
        if (capturesThis) {
            indyCall.argumentList.add(factory.createExpressionFromText("this", lambda))
        } else if (methodReferenceQualifier != null) {
            indyCall.argumentList.add(methodReferenceQualifier)
        }
        for (variable in capturedVariables) {
            val varName = variable.name ?: continue
            indyCall.argumentList.add(factory.createExpressionFromText(varName, lambda))
        }

        methodReferenceQualifier?.replace(factory.createExpressionFromText(lambdaMethod.parameterList.parameters.first().name, null))
        when (lambdaBody) {
            is PsiCodeBlock -> lambdaMethod.body!!.replace(lambdaBody)
            is PsiExpression -> {
                val statementToAdd = if (functionalReturnType == PsiTypes.voidType()) {
                    val stmt = factory.createStatementFromText("foo();", null) as PsiExpressionStatement
                    stmt.expression.replace(lambdaBody)
                    stmt
                } else {
                    val stmt = factory.createStatementFromText("return x;", null) as PsiReturnStatement
                    stmt.returnValue!!.replace(lambdaBody)
                    stmt
                }
                lambdaMethod.body!!.add(statementToAdd)
            }
            else -> throw IllegalStateException("Lambda body must be PsiCodeBlock or PsiExpression")
        }

        lambda.replace(indyCall)

        return serializationData
    }

    private fun getCapturedVariables(
        lambda: PsiLambdaExpression,
        methodReferenceQualifier: PsiExpression?
    ): Collection<PsiVariable> {
        val capturedVariables = linkedSetOf<PsiVariable>()

        lambda.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                doVisitReferenceElement(reference)
            }

            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                doVisitReferenceElement(expression)
            }

            fun doVisitReferenceElement(element: PsiJavaCodeReferenceElement) {
                val variable = element.resolve() as? PsiVariable ?: return
                if (variable is PsiField) {
                    return
                }
                if (PsiTreeUtil.isAncestor(methodReferenceQualifier, element, false)) {
                    return
                }
                if (PsiTreeUtil.isAncestor(lambda, variable, true)) {
                    return
                }
                capturedVariables += variable
            }
        })

        return capturedVariables
    }

    private fun getMethodReferenceQualifier(lambda: PsiLambdaExpression): PsiExpression? {
        if (!MethodReferenceToLambdaDesugarer.isQualifiedMethodReference(lambda)) {
            return null
        }

        return (LambdaUtil.extractSingleExpressionFromBody(lambda.body) as? PsiMethodCallExpression)
            ?.methodExpression
            ?.qualifierExpression
            ?.takeUnless { it is PsiSuperExpression }
    }

    private fun desugarMethodReference(
        project: Project,
        methodReference: PsiMethodReferenceExpression,
        context: DesugarContext
    ): LambdaSerializationData? {
        val qualifier = methodReference.qualifier ?: return null
        val boundInstance = (qualifier as? PsiExpression)?.takeIf {
            it !is PsiReferenceExpression || it.resolve() !is PsiClass
        }
        val calledMethod = methodReference.resolve() ?: return null
        val containingClass = methodReference.findContainingClass() ?: return null

        val (indyCall, serializationData) = createIndyCall(
            project,
            containingClass,
            methodReference,
            listOfNotNull(boundInstance?.type),
            calledMethod
        ) ?: return null
        if (boundInstance != null) {
            indyCall.argumentList.add(DesugarUtil.createNullCheck(project, boundInstance, context.classVersion))
        }
        methodReference.replace(indyCall)

        return serializationData
    }

    private fun createIndyCall(
        project: Project,
        containingClass: PsiClass,
        functionalExpr: PsiFunctionalExpression,
        capturedTypes: List<PsiType>,
        implElement: PsiElement,
    ): Pair<PsiMethodCallExpression, LambdaSerializationData?>? {
        val containingClassName = containingClass.name ?: return null
        val implClass = implElement.parentOfType<PsiClass>(withSelf = true) ?: return null
        val implMethod = implElement as? PsiMethod
        val implMethodDesc = implMethod?.descriptor ?: "()V"
        val functionalInterfaceType = getFunctionalInterfaceType(functionalExpr) ?: return null
        val functionalInterfaceMethods = getFunctionalInterfaceMethods(functionalInterfaceType).ifEmpty { return null }
        val normalizedFunctionalInterfaceType = normalizeFunctionalInterfaceType(functionalInterfaceType)
        val (functionalInterfaceMethod, functionalInterfaceMethodSignature) = functionalInterfaceMethods.first()
        val functionalInterfaceMethodDesc = functionalInterfaceMethod.descriptor ?: return null
        val instantiatedMethodDesc = Type.getMethodType(
            Type.getType(functionalInterfaceMethodSignature.substitutor.substitute(functionalInterfaceMethod.returnType).descriptor),
            *functionalInterfaceMethodSignature.parameterTypes.mapToArray { Type.getType(it.descriptor) }
        )

        val serializableLambda = isSerializable(project, functionalInterfaceType)
        val needsAltMetafactory = functionalInterfaceType is PsiIntersectionType || functionalInterfaceMethods.size > 1 || serializableLambda

        val implMethodHandle = Handle(
            when {
                implMethod?.isConstructor != false -> Opcodes.H_NEWINVOKESPECIAL
                implMethod.hasModifierProperty(PsiModifier.STATIC) -> Opcodes.H_INVOKESTATIC
                implClass.isInterface -> Opcodes.H_INVOKEINTERFACE
                else -> Opcodes.H_INVOKEVIRTUAL
            },
            implClass.fullQualifiedName?.replace('.', '/') ?: return null,
            if (implMethod?.isConstructor != false) "<init>" else implMethod.name,
            implMethodDesc,
            implClass.isInterface
        )
        val bsmArgs = mutableListOf<Any>(
            Type.getMethodType(functionalInterfaceMethodDesc),
            implMethodHandle,
            instantiatedMethodDesc
        )

        if (needsAltMetafactory) {
            val intersectionTypes = if (functionalInterfaceType is PsiIntersectionType) {
                functionalInterfaceType.conjuncts.filter { PsiUtil.resolveClassInType(it)?.qualifiedName != "java.io.Serializable" }
            } else {
                listOf(functionalInterfaceType)
            }
            val markerInterfaces = intersectionTypes.filter { it != normalizedFunctionalInterfaceType }

            var flags = LambdaMetafactory.FLAG_BRIDGES // bridges is always set for some reason
            if (serializableLambda) {
                flags = flags or LambdaMetafactory.FLAG_SERIALIZABLE
            }
            if (markerInterfaces.isNotEmpty()) {
                flags = flags or LambdaMetafactory.FLAG_MARKERS
            }
            bsmArgs += flags

            if (markerInterfaces.isNotEmpty()) {
                bsmArgs += markerInterfaces.size
                markerInterfaces.mapTo(bsmArgs) { Type.getType(it.descriptor) }
            }

            bsmArgs += functionalInterfaceMethods.size - 1
            for ((method, _) in functionalInterfaceMethods.drop(1)) {
                val desc = method.descriptor ?: continue
                bsmArgs += Type.getType(desc)
            }
        }

        val methodDesc =
            "(${capturedTypes.joinToString("") { it.descriptor }})${normalizedFunctionalInterfaceType.descriptor}"
        val indyData = DesugarUtil.IndyData(
            functionalInterfaceMethod.name,
            methodDesc,
            Handle(
                Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory",
                if (needsAltMetafactory) "altMetafactory" else "metafactory",
                if (needsAltMetafactory) ALT_METAFACTORY_DESC else METAFACTORY_DESC,
                false
            ),
            *bsmArgs.toTypedArray()
        )

        val factory = JavaPsiFacade.getElementFactory(project)
        val methodName = DesugarUtil.generateMethodName(project, containingClass, capturedTypes)

        val createdMethod = factory.createMethodFromText("private static void $methodName() {}", null)
        DesugarUtil.setFake(createdMethod, true)
        createdMethod.returnTypeElement!!.replace(factory.createTypeElement(normalizedFunctionalInterfaceType.normalize()))
        for ((i, paramType) in capturedTypes.withIndex()) {
            createdMethod.parameterList.add(factory.createParameter("param${i + 1}", paramType.normalize()))
        }
        containingClass.add(createdMethod)

        val indyCall = factory.createExpressionFromText("$containingClassName.$methodName()", functionalExpr)
            as PsiMethodCallExpression
        DesugarUtil.setOriginalElement(indyCall, DesugarUtil.getOriginalElement(functionalExpr))
        DesugarUtil.setIndyData(indyCall, indyData)

        val serializationData = if (serializableLambda) {
            LambdaSerializationData(
                implMethodHandle.name,
                implMethodHandle.tag,
                Type.getType(normalizedFunctionalInterfaceType.descriptor).internalName,
                functionalInterfaceMethod.name,
                functionalInterfaceMethodDesc,
                implMethodHandle.owner,
                implMethodHandle.desc,
                methodName,
                capturedTypes,
                indyData,
            )
        } else {
            null
        }

        return indyCall to serializationData
    }

    private fun getFunctionalInterfaceType(expression: PsiElement): PsiType? {
        var parent = expression.parent
        var element = expression

        while (parent is PsiParenthesizedExpression || parent is PsiConditionalExpression) {
            if (parent is PsiConditionalExpression && parent.condition == element) {
                return PsiTypes.booleanType()
            }
            element = parent
            parent = parent.parent
        }

        when (parent) {
            is PsiArrayInitializerExpression -> {
                val type = parent.type
                if (type is PsiArrayType) {
                    return type.componentType
                }
            }
            is PsiTypeCastExpression -> return parent.castType?.type
            is PsiVariable -> return parent.type
            is PsiAssignmentExpression -> {
                if (expression is PsiExpression && !PsiUtil.isOnAssignmentLeftHand(expression)) {
                    return parent.lExpression.type
                }
            }
            is PsiExpressionList -> {
                val lambdaIdx = LambdaUtil.getLambdaIdx(parent, expression)
                if (lambdaIdx >= 0) {
                    var granny = parent.parent
                    if (granny is PsiAnonymousClass) {
                        granny = granny.parent
                    }
                    if (granny is PsiCall) {
                        val resolveResult = PsiDiamondType.getDiamondsAwareResolveResult(granny)
                        val resolved = resolveResult.element
                        if (resolved is PsiMethod) {
                            val parameters = resolved.parameterList.parameters
                            val finalLambdaIdx = adjustLambdaIdx(lambdaIdx, resolved, parameters)
                            if (finalLambdaIdx < parameters.size) {
                                return PsiResolveHelper.ourGraphGuard.doPreventingRecursion(expression, !MethodCandidateInfo.isOverloadCheck()) {
                                    resolveResult.substitutor.substitute(parameters[finalLambdaIdx].type)
                                }
                            }
                        }
                    }
                }
            }
            is PsiReturnStatement -> return PsiTypesUtil.getMethodReturnType(parent)
            is PsiLambdaExpression -> return LambdaUtil.getFunctionalInterfaceType(expression, true)
            else -> {
                val switchExpression = element.parentOfType<PsiSwitchExpression>()
                if (switchExpression != null && expression in PsiUtil.getSwitchResultExpressions(switchExpression)) {
                    return getFunctionalInterfaceType(switchExpression)
                }
            }
        }

        return null
    }

    private fun adjustLambdaIdx(lambdaIdx: Int, resolved: PsiMethod, parameters: Array<PsiParameter>): Int {
        return if (resolved.isVarArgs && lambdaIdx >= parameters.size) {
            parameters.size - 1
        } else {
            lambdaIdx
        }
    }

    private fun normalizeFunctionalInterfaceType(functionalInterfaceType: PsiType): PsiType {
        return if (functionalInterfaceType is PsiIntersectionType) {
            functionalInterfaceType.conjuncts.firstOrNull {
                getFunctionalInterfaceMethods(it).isNotEmpty()
            } ?: functionalInterfaceType
        } else {
            functionalInterfaceType
        }
    }

    // see com.sun.tools.javac.code.Types.functionalInterfaceBridges
    private fun getFunctionalInterfaceMethods(functionalInterfaceType: PsiType): List<Pair<PsiMethod, MethodSignature>> {
        val types = if (functionalInterfaceType is PsiIntersectionType) {
            functionalInterfaceType.conjuncts
        } else {
            arrayOf(functionalInterfaceType)
        }

        val result = mutableListOf<Pair<PsiMethod, MethodSignature>>()
        for (type in types) {
            val resolveResult = PsiUtil.resolveGenericsClassInType(type)
            val clazz = resolveResult.element ?: continue

            outer@
            for (method in getAllAbstractMethods(clazz)) {
                val substitutor = LambdaUtil.getSubstitutor(method, resolveResult)
                val erasedSignature = method.getSignature(PsiSubstitutor.EMPTY)
                val signature = method.getSignature(substitutor)
                for ((existingMethod, existingSignature) in result) {
                    if (existingMethod.name != method.name) {
                        return emptyList()
                    }
                    if (!MethodSignatureUtil.areErasedParametersEqual(existingSignature, signature)) {
                        return emptyList()
                    }

                    val erasedReturnType = method.returnType?.let(TypeConversionUtil::erasure)
                    val existingErasedReturnType = existingMethod.returnType?.let(TypeConversionUtil::erasure)
                    val existingErasedSignature = existingMethod.getSignature(PsiSubstitutor.EMPTY)
                    if (erasedReturnType == existingErasedReturnType && MethodSignatureUtil.areErasedParametersEqual(existingErasedSignature, erasedSignature)) {
                        continue@outer
                    }
                }

                result += method to signature
            }
        }

        return result
    }

    private fun getAllAbstractMethods(clazz: PsiClass): List<PsiMethod> {
        if (!clazz.isInterface || clazz.isAnnotationType) {
            return emptyList()
        }

        return RecursionManager.doPreventingRecursion(clazz, true) {
            val abstractMethods = mutableListOf<PsiMethod>()
            val defaultMethods = mutableListOf<PsiMethod>()
            for (method in clazz.methods) {
                if (method.hasModifierProperty(PsiModifier.STATIC)) {
                    continue
                }
                if (method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                    if (!overridesPublicObjectMethod(method.hierarchicalMethodSignature)) {
                        abstractMethods += method
                    }
                } else {
                    defaultMethods += method
                }
            }

            for (superInterface in clazz.interfaces) {
                getAllAbstractMethods(superInterface).filterTo(abstractMethods) { method ->
                    defaultMethods.none { defaultMethod ->
                        defaultMethod.name == method.name && PsiSuperMethodUtil.isSuperMethod(defaultMethod, method)
                    }
                }
            }

            abstractMethods
        } ?: emptyList()
    }

    private fun overridesPublicObjectMethod(methodSig: HierarchicalMethodSignature): Boolean {
        val superSigs = methodSig.superSignatures

        return if (superSigs.isEmpty()) {
            val method = methodSig.method
            if (method.containingClass?.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT) {
                if (method.hasModifierProperty(PsiModifier.PUBLIC)) {
                    return true
                }
            }

            false
        } else {
            superSigs.any(::overridesPublicObjectMethod)
        }
    }

    private fun isSerializable(project: Project, type: PsiType): Boolean {
        return JavaPsiFacade.getElementFactory(project)
            .createTypeByFQClassName("java.io.Serializable")
            .isAssignableFrom(type)
    }

    private fun createDeserializeLambdaMethod(
        project: Project,
        serializationDatas: Map<String, List<LambdaSerializationData>>
    ): PsiMethod {
        val methodText = buildString {
            append("private static java.lang.Object \$deserializeLambda$(java.lang.invoke.SerializedLambda serializedLambda) {")
            append("switch (serializedLambda.getImplMethodName()) {")
            // we collected the serialization datas in reverse, so iterate in reverse again here so the cases appear in
            // the correct order.
            for (implMethodName in serializationDatas.keys.toList().asReversed()) {
                append("case \"")
                append(StringUtil.escapeStringCharacters(implMethodName))
                append("\":")
                append("break;")
            }
            append("}")
            append("throw new java.lang.IllegalArgumentException(\"Invalid lambda deserialization\");")
            append("}")
        }

        val factory = JavaPsiFacade.getElementFactory(project)
        val method = factory.createMethodFromText(methodText, null)
        DesugarUtil.setUnnamedVariable(method.parameterList.parameters.first(), true)
        val switchStatement = method.body!!.statements.first() as PsiSwitchStatement
        for (switchBranch in SwitchUtils.getSwitchBranches(switchStatement)) {
            val branchValue = (switchBranch as PsiLiteralExpression).value as String
            // PsiLiteralExpression -> PsiCaseLabelElementList -> PsiSwitchLabelStatement
            val caseLabel = switchBranch.parent.parent as PsiSwitchLabelStatement
            // don't iterate in reverse here like we do above; we are adding the if statements *after* the case label,
            // which already has the effect of reversing them.
            for (data in serializationDatas[branchValue]!!) {
                val ifStatementText = buildString {
                    append("if (serializedLambda.getImplMethodKind() == ")
                    append(data.implMethodKind)
                    append(" && serializedLambda.getFunctionalInterfaceClass().equals(\"")
                    append(StringUtil.escapeStringCharacters(data.functionalInterfaceClass))
                    append("\") && serializedLambda.getFunctionalInterfaceMethodName().equals(\"")
                    append(StringUtil.escapeStringCharacters(data.functionalInterfaceMethodName))
                    append("\") && serializedLambda.getFunctionalInterfaceMethodSignature().equals(\"")
                    append(StringUtil.escapeStringCharacters(data.functionalInterfaceMethodSignature))
                    append("\") && serializedLambda.getImplClass().equals(\"")
                    append(StringUtil.escapeStringCharacters(data.implClass))
                    append("\") && serializedLambda.getImplMethodSignature().equals(\"")
                    append(StringUtil.escapeStringCharacters(data.implMethodSignature))
                    append("\"))")
                    append(data.indyMethodName)
                    append("();")
                }
                val ifStatement = factory.createStatementFromText(ifStatementText, caseLabel) as PsiIfStatement
                val indyCall = (ifStatement.thenBranch as PsiExpressionStatement).expression as PsiMethodCallExpression
                DesugarUtil.setIndyData(indyCall, data.indyData)
                for ((i, captureType) in data.captureTypes.withIndex()) {
                    val captureExpr = if ((captureType as? PsiClassType)?.resolve()?.qualifiedName == "java.lang.Object") {
                        factory.createExpressionFromText("serializedLambda.getCapturedArg($i)", caseLabel)
                    } else {
                        val castExpr = factory.createExpressionFromText("(x) serializedLambda.getCapturedArg($i)", caseLabel)
                            as PsiTypeCastExpression
                        castExpr.castType!!.replace(factory.createTypeElement(captureType.toObjectType(project)))
                        castExpr
                    }
                    indyCall.argumentList.add(captureExpr)
                }
                switchStatement.body!!.addAfter(ifStatement, caseLabel)
            }
        }

        return method
    }

    private class LambdaSerializationData(
        val implMethodName: String,
        val implMethodKind: Int,
        val functionalInterfaceClass: String,
        val functionalInterfaceMethodName: String,
        val functionalInterfaceMethodSignature: String,
        val implClass: String,
        val implMethodSignature: String,
        val indyMethodName: String,
        val captureTypes: List<PsiType>,
        val indyData: DesugarUtil.IndyData,
    )
}
