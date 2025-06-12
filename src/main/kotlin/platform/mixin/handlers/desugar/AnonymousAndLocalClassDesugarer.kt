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

import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.fullQualifiedName
import com.intellij.codeInsight.ChangeContextUtil
import com.intellij.codeInsight.daemon.impl.quickfix.AddDefaultConstructorFix
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.CommonClassNames
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiCall
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiDiamondType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiExpressionList
import com.intellij.psi.PsiExpressionStatement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNewExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiReferenceParameterList
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiVariable
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.impl.PsiDiamondTypeUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parents
import com.intellij.refactoring.anonymousToInner.AnonymousToInnerHandler
import com.intellij.refactoring.util.RefactoringChangeUtil
import com.intellij.util.CommonJavaRefactoringUtil
import com.intellij.util.JavaPsiConstructorUtil
import com.siyeh.ig.psiutils.VariableNameGenerator
import org.objectweb.asm.Opcodes

object AnonymousAndLocalClassDesugarer : Desugarer() {
    private val VARIABLE_INFOS_KEY = Key.create<Array<VariableInfo>>("mcdev.desugar.variableInfos")

    override fun desugar(project: Project, file: PsiJavaFile, context: DesugarContext) {
        for (clazz in file.classes) {
            extractRecursive(project, context, clazz)
        }
    }

    private fun extractRecursive(project: Project, context: DesugarContext, clazz: PsiClass) {
        for (innerClass in DesugarUtil.allClassesShallow(clazz)) {
            extractRecursive(project, context, innerClass)
        }

        if (PsiUtil.isLocalOrAnonymousClass(clazz)) {
            extractLocalOrAnonymousClass(project, context, clazz)
        }
    }

    private fun extractLocalOrAnonymousClass(project: Project, context: DesugarContext, localClass: PsiClass) {
        val targetClass = AnonymousToInnerHandler.findTargetContainer(localClass) as? PsiClass ?: return
        val newClassName = "$" + (localClass.fullQualifiedName?.substringAfterLast("$") ?: return)

        val variableInfos = if (localClass.hasModifierProperty(PsiModifier.STATIC)) {
            emptyArray()
        } else {
            collectUsedVariables(localClass)
        }

        val typeParametersToCreate =
            DesugarUtil.getTypeParametersToCopy(localClass, targetClass, variableInfos.map { it.variable })
        ChangeContextUtil.encodeContextInfo(localClass, false)
        renameReferences(project, context, localClass, variableInfos)
        updateLocalClassConstructors(project, localClass, variableInfos)
        val newClass = targetClass.add(createClass(project, localClass, targetClass, variableInfos, typeParametersToCreate, newClassName)) as PsiClass
        ChangeContextUtil.decodeContextInfo(newClass, targetClass, RefactoringChangeUtil.createThisExpression(targetClass.manager, targetClass))
        newClass.putCopyableUserData(VARIABLE_INFOS_KEY, variableInfos)

        if (localClass is PsiAnonymousClass) {
            migrateAnonymousClassCreation(project, newClassName, localClass, variableInfos, typeParametersToCreate)
        } else {
            migrateLocalClassCreation(localClass, newClass)
        }
    }

    private fun collectUsedVariables(localClass: PsiClass): Array<VariableInfo> {
        val originalAnonymousParams = mutableListOf<VariableInfo>()
        val variableInfoMap = linkedMapOf<PsiVariable, VariableInfo>()

        if (localClass is PsiAnonymousClass) {
            val resolvedCtor = (localClass.parent as? PsiNewExpression)?.resolveConstructor()
            if (resolvedCtor != null) {
                for (param in resolvedCtor.parameterList.parameters) {
                    val paramName = VariableNameGenerator(localClass, VariableKind.PARAMETER)
                        .byName(param.name)
                        .skipNames(originalAnonymousParams.map { it.paramName })
                        .generate(false)
                    originalAnonymousParams += VariableInfo(param, paramName, null, passToSuper = true)
                }

                val parentVariableInfos = resolvedCtor.containingClass?.getCopyableUserData(VARIABLE_INFOS_KEY)
                if (parentVariableInfos != null) {
                    repeat(parentVariableInfos.size.coerceAtMost(originalAnonymousParams.size)) {
                        originalAnonymousParams.removeLast()
                    }
                    for (parentInfo in parentVariableInfos) {
                        val paramName = VariableNameGenerator(localClass, VariableKind.PARAMETER)
                            .byName(parentInfo.variable.name)
                            .skipNames(originalAnonymousParams.map { it.paramName })
                            .generate(false)
                        variableInfoMap[parentInfo.variable] =
                            VariableInfo(parentInfo.variable, paramName, parentInfo.fieldName, passToSuper = true)
                    }
                }
            }
        }

        localClass.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitReferenceExpression(expression: PsiReferenceExpression) {
                if (expression.qualifierExpression == null) {
                    val resolved = expression.resolve()
                    if (resolved is PsiVariable && resolved !is PsiField) {
                        val containingClass = resolved.findContainingClass()
                        if (PsiTreeUtil.isAncestor(containingClass, localClass, true)) {
                            saveVariable(localClass, variableInfoMap, resolved, expression)
                        }
                    }
                }
                super.visitReferenceExpression(expression)
            }
        })

        return (originalAnonymousParams + variableInfoMap.values).toTypedArray()
    }

    private fun saveVariable(
        localClass: PsiClass,
        variableInfoMap: MutableMap<PsiVariable, VariableInfo>,
        variable: PsiVariable,
        usage: PsiReferenceExpression
    ) {
        if (usage.isInsideAnonymousClassParameter(localClass)) {
            return
        }

        variableInfoMap.getOrPut(variable) {
            val variableName = variable.name ?: return
            VariableInfo(variable, variableName, "val$$variableName")
        }
    }

    private fun updateLocalClassConstructors(
        project: Project,
        localClass: PsiClass,
        variableInfos: Array<VariableInfo>
    ) {
        if (localClass is PsiAnonymousClass || variableInfos.isEmpty()) {
            return
        }

        var constructors = localClass.constructors
        if (constructors.isEmpty()) {
            constructors = arrayOf(AddDefaultConstructorFix.addDefaultConstructor(localClass))
        }

        val constructorCalls = mutableMapOf<PsiMethod, MutableList<PsiElement>>()

        if (variableInfos.isNotEmpty()) {
            for (constructor in constructors) {
                for (reference in DesugarUtil.findReferencesInFile(constructor)) {
                    var refElement = reference.element
                    if (refElement is PsiMethod || (refElement is PsiClass && refElement !is PsiAnonymousClass)) {
                        constructorCalls.getOrPut(constructor) { mutableListOf() } += refElement
                    } else {
                        refElement = refElement.parent
                        if (refElement is PsiAnonymousClass) {
                            refElement = refElement.parent
                        }
                        if (refElement is PsiCall) {
                            constructorCalls.getOrPut(constructor) { mutableListOf() } += refElement
                        }
                    }
                }
            }
        }

        val factory = JavaPsiFacade.getElementFactory(project)

        for (constructor in constructors) {
            val callSites = constructorCalls[constructor] ?: emptyList()

            fillParameterList(project, constructor, variableInfos)

            val constructorHasThisCall = JavaPsiConstructorUtil.isChainedConstructorCall(
                JavaPsiConstructorUtil.findThisOrSuperCallInConstructor(constructor)
            )
            if (!constructorHasThisCall) {
                createAssignmentStatements(constructor, variableInfos)
            }

            for (callSite in callSites) {
                val argumentList = when (callSite) {
                    is PsiCall -> callSite.argumentList ?: continue
                    is PsiMethod -> {
                        if (!callSite.isConstructor) {
                            continue
                        }
                        val body = callSite.body ?: continue
                        val superCall = body.addAfter(factory.createStatementFromText("super();", null), body.lBrace)
                            as PsiExpressionStatement
                        (superCall.expression as PsiMethodCallExpression).argumentList
                    }
                    is PsiClass -> {
                        val ctor = AddDefaultConstructorFix.addDefaultConstructor(callSite)
                        val body = ctor.body ?: continue
                        val superCall = body.addAfter(factory.createStatementFromText("super();", null), body.lBrace)
                            as PsiExpressionStatement
                        (superCall.expression as PsiMethodCallExpression).argumentList
                    }
                    else -> continue
                }
                for (info in variableInfos) {
                    argumentList.add(factory.createExpressionFromText(info.paramName, argumentList))
                }
            }
        }
    }

    private fun fillParameterList(project: Project, constructor: PsiMethod, variableInfos: Array<VariableInfo>) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val parameterList = constructor.parameterList
        for (info in variableInfos) {
            val parameter = factory.createParameter(info.paramName, info.variable.type)
            DesugarUtil.setUnnamedVariable(parameter, true)
            parameterList.add(parameter)
        }
    }

    private fun createAssignmentStatements(constructor: PsiMethod, variableInfos: Array<VariableInfo>) {
        val constructorBody = constructor.body ?: return
        val factory = PsiElementFactory.getInstance(constructor.project)

        if (JavaPsiConstructorUtil.findThisOrSuperCallInConstructor(constructor) == null) {
            val superCall = factory.createStatementFromText("super();", constructorBody)
            constructorBody.addAfter(superCall, constructorBody.lBrace)
        }

        for (i in variableInfos.lastIndex downTo 0) {
            val info = variableInfos[i]
            if (info.fieldName != null) {
                val statement = factory.createStatementFromText("this.${info.fieldName} = ${info.paramName};", constructorBody)
                constructorBody.addAfter(statement, constructorBody.lBrace)
            }
        }
    }

    private fun createClass(
        project: Project,
        localClass: PsiClass,
        targetClass: PsiClass,
        variableInfos: Array<VariableInfo>,
        typeParametersToCreate: Collection<PsiTypeParameter>,
        name: String,
    ): PsiClass {
        updateSelfReferences(project, localClass, typeParametersToCreate, name)

        val newClass = if (localClass is PsiAnonymousClass) {
            createInnerFromAnonymous(project, name, localClass)
        } else {
            createInnerFromLocal(project, name, localClass)
        }

        if (!localClass.isInterface && !localClass.isEnum && !localClass.isRecord) {
            if (isInStaticContext(localClass, targetClass)) {
                PsiUtil.setModifierProperty(newClass, PsiModifier.STATIC, true)
            }
        }

        val typeParameterList = newClass.typeParameterList!!
        for (parameter in typeParametersToCreate) {
            typeParameterList.add(parameter)
        }

        if (variableInfos.isNotEmpty()) {
            createFields(project, newClass, variableInfos)
        }

        if (localClass is PsiAnonymousClass) {
            createAnonymousClassConstructor(project, newClass, localClass, variableInfos)
        }

        val lastChild = newClass.lastChild
        if (PsiUtil.isJavaToken(lastChild, JavaTokenType.SEMICOLON)) {
            lastChild.delete()
        }

        return newClass
    }

    private fun renameReferences(project: Project, context: DesugarContext, localClass: PsiClass, variableInfos: Array<VariableInfo>) {
        val factory = JavaPsiFacade.getElementFactory(project)
        for (info in variableInfos) {
            for (reference in DesugarUtil.findReferencesInFile(info.variable)) {
                val element = reference.element as? PsiJavaCodeReferenceElement ?: continue
                if (element.isInsideAnonymousClassParameter(localClass)) {
                    continue
                }
                if (!PsiTreeUtil.isAncestor(localClass, element, false)) {
                    continue
                }
                val identifier = element.referenceNameElement as? PsiIdentifier ?: continue
                val shouldRenameToField = info.fieldName != null && renameReferenceToField(element, context)
                val newNameIdentifier = factory.createIdentifier(if (shouldRenameToField) info.fieldName else info.paramName)
                DesugarUtil.setOriginalElement(newNameIdentifier, DesugarUtil.getOriginalElement(identifier))
                identifier.replace(newNameIdentifier)
            }
        }
    }

    private fun renameReferenceToField(element: PsiJavaCodeReferenceElement, context: DesugarContext): Boolean {
        val constructor = element.findContainingMethod() ?: return true
        if (!constructor.isConstructor) {
            return true
        }

        if (context.classVersion >= Opcodes.V22) {
            return false
        }

        val thisOrSuperCall = JavaPsiConstructorUtil.findThisOrSuperCallInConstructor(constructor) ?: return true
        return element.textRange.startOffset >= thisOrSuperCall.textRange.endOffset
    }

    private fun updateSelfReferences(
        project: Project,
        localClass: PsiClass,
        typeParametersToCreate: Collection<PsiTypeParameter>,
        name: String,
    ) {
        if (localClass is PsiAnonymousClass) {
            return
        }
        if (name == localClass.name && typeParametersToCreate.isEmpty()) {
            return
        }

        val factory = JavaPsiFacade.getElementFactory(project)
        val origCount = localClass.typeParameters.size

        for (reference in DesugarUtil.findReferencesInFile(localClass)) {
            val originalReference = DesugarUtil.getOriginalElement(reference.element)
            val renamedReference = reference.handleElementRename(name)
            DesugarUtil.setOriginalElement(renamedReference, originalReference)
            if (typeParametersToCreate.isNotEmpty()) {
                val refElement = reference.element as? PsiJavaCodeReferenceElement ?: continue
                if ((refElement.parent as? PsiTypeElement)?.parent is PsiClassObjectAccessExpression) {
                    continue
                }
                val referenceParameterList = refElement.childrenOfType<PsiReferenceParameterList>().firstOrNull()
                // Do not modify broken or already raw parameter lists
                if (referenceParameterList != null && referenceParameterList.typeArgumentCount == origCount) {
                    for (parameter in typeParametersToCreate) {
                        val element = factory.createTypeElement(factory.createType(parameter))
                        referenceParameterList.add(element)
                    }
                }
            }
        }
    }

    private fun createInnerFromLocal(project: Project, name: String, localClass: PsiClass): PsiClass {
        val factory = JavaPsiFacade.getElementFactory(project)
        val newClass = localClass.copy() as PsiClass
        val identifier = factory.createIdentifier(name)
        val originalNameIdentifier = localClass.nameIdentifier?.let(DesugarUtil::getOriginalElement)
        newClass.nameIdentifier?.replace(identifier)?.let {
            DesugarUtil.setOriginalElement(it, originalNameIdentifier)
        }
        for (constructor in newClass.methods) {
            if (constructor.isConstructor) {
                val originalCtorNameIdentifier = constructor.nameIdentifier?.let(DesugarUtil::getOriginalElement)
                constructor.nameIdentifier?.replace(identifier)?.let {
                    DesugarUtil.setOriginalElement(it, originalCtorNameIdentifier)
                }
            }
        }
        return newClass
    }

    private fun createInnerFromAnonymous(project: Project, name: String, localClass: PsiAnonymousClass): PsiClass {
        val factory = JavaPsiFacade.getElementFactory(project)
        val newClass = factory.createClass(name)
        PsiUtil.setModifierProperty(newClass, PsiModifier.PACKAGE_LOCAL, true)
        var baseClassRef = localClass.baseClassReference
        val parameterList = baseClassRef.parameterList
        if (parameterList != null) {
           val parameterElement = parameterList.typeParameterElements.singleOrNull()
           if (parameterElement?.type is PsiDiamondType) {
               val originalBaseClassRef = DesugarUtil.getOriginalElement(baseClassRef)
               baseClassRef = PsiDiamondTypeUtil.replaceDiamondWithExplicitTypes(parameterList)
                   as PsiJavaCodeReferenceElement
               DesugarUtil.setOriginalElement(baseClassRef, originalBaseClassRef)
           }
        }
        val baseClass = baseClassRef.resolve() as PsiClass?
        if (baseClass?.qualifiedName != CommonClassNames.JAVA_LANG_OBJECT) {
            val refList = if (baseClass?.isInterface == true) {
                newClass.implementsList
            } else {
                newClass.extendsList
            }
            refList?.add(baseClassRef)
        }
        val lBrace = localClass.lBrace
        val rBrace = localClass.rBrace
        if (lBrace != null) {
            newClass.addRange(lBrace.nextSibling, rBrace?.prevSibling ?: localClass.lastChild)
        }
        DesugarUtil.setOriginalElement(newClass, DesugarUtil.getOriginalElement(localClass))
        return newClass
    }

    private fun createFields(project: Project, localClass: PsiClass, variableInfos: Array<VariableInfo>) {
        val factory = JavaPsiFacade.getElementFactory(project)
        for (info in variableInfos) {
            if (info.fieldName != null) {
                val field = factory.createField(info.fieldName, info.variable.type)
                PsiUtil.setModifierProperty(field, PsiModifier.PACKAGE_LOCAL, true)
                PsiUtil.setModifierProperty(field, PsiModifier.FINAL, true)
                localClass.add(field)
            }
        }
    }

    private fun createAnonymousClassConstructor(project: Project, newClass: PsiClass, localClass: PsiAnonymousClass, variableInfos: Array<VariableInfo>) {
        val factory = JavaPsiFacade.getElementFactory(project)
        val newExpression = localClass.parent as? PsiNewExpression ?: return
        val argList = newExpression.argumentList ?: return
        val originalExpressions = argList.expressions
        val superConstructor = newExpression.resolveConstructor()
        val superConstructorThrowsList = superConstructor?.throwsList?.takeIf { it.referencedTypes.isNotEmpty() }
        if (variableInfos.isNotEmpty() || originalExpressions.isNotEmpty() || superConstructorThrowsList != null) {
            val constructor = factory.createConstructor()
            PsiUtil.setModifierProperty(constructor, PsiModifier.PACKAGE_LOCAL, true)
            if (superConstructorThrowsList != null) {
                constructor.throwsList.replace(superConstructorThrowsList)
            }
            if (variableInfos.any { it.passToSuper }) {
                createSuperStatement(project, constructor, variableInfos)
            }
            if (variableInfos.isNotEmpty()) {
                fillParameterList(project, constructor, variableInfos)
                createAssignmentStatements(constructor, variableInfos)
            }
            newClass.add(constructor)
        }
    }

    private fun createSuperStatement(project: Project, constructor: PsiMethod, variableInfos: Array<VariableInfo>) {
        val body = constructor.body ?: return
        val factory = JavaPsiFacade.getElementFactory(project)

        val statementText = buildString {
            append("super(")
            for ((index, info) in variableInfos.withIndex()) {
                if (info.passToSuper) {
                    if (index > 0) {
                        append(", ")
                    }
                    append(info.paramName)
                }
            }
            append(");")
        }

        body.add(factory.createStatementFromText(statementText, constructor))
    }

    private fun migrateAnonymousClassCreation(
        project: Project,
        newClassName: String,
        anonymousClass: PsiAnonymousClass,
        variableInfos: Array<VariableInfo>,
        typeParametersToCreate: Collection<PsiTypeParameter>,
    ) {
        val newExpr = anonymousClass.parent as? PsiNewExpression ?: return
        val argumentList = newExpr.argumentList ?: return

        val newNewExprText = buildString {
            append("new ")
            append(newClassName)
            if (typeParametersToCreate.isNotEmpty()) {
                append('<')
                for ((index, typeParam) in typeParametersToCreate.withIndex()) {
                    if (index > 0) {
                        append(", ")
                    }
                    append(typeParam.name)
                }
                append('>')
            }
            append("()")
        }

        val factory = JavaPsiFacade.getElementFactory(project)
        val newNewExpr = factory.createExpressionFromText(newNewExprText, null)
            as PsiNewExpression
        val newArgumentList = newNewExpr.argumentList!!.replace(argumentList) as PsiExpressionList
        // skip the already existing arguments
        for (info in variableInfos.drop(newArgumentList.expressionCount)) {
            val varName = info.variable.name ?: continue
            newArgumentList.add(factory.createExpressionFromText(varName, null))
        }

        DesugarUtil.setOriginalElement(newNewExpr, DesugarUtil.getOriginalElement(newExpr))

        newExpr.replace(newNewExpr)
    }

    private fun migrateLocalClassCreation(localClass: PsiClass, newClass: PsiClass) {
        val refs = DesugarUtil.findReferencesInFile(localClass)
        localClass.delete()
        for (ref in refs) {
            val element = ref.element
            if (element.isValid) {
                ref.bindToElement(newClass)
            }
        }
    }

    private fun isInStaticContext(localClass: PsiClass, targetClass: PsiClass): Boolean {
        val containingMethod = localClass.parent?.findContainingMethod()
        if (containingMethod != null && containingMethod.isConstructor) {
            val delegateCtorCall = JavaPsiConstructorUtil.findThisOrSuperCallInConstructor(containingMethod)
                ?: return false
            return localClass.textRange.startOffset < delegateCtorCall.textRange.endOffset
        }

        val localParent = localClass.parent ?: return true
        return CommonJavaRefactoringUtil.isInStaticContext(localParent, targetClass)
    }

    private fun PsiElement.isInsideAnonymousClassParameter(anonymousClass: PsiClass): Boolean {
        return this.parents(false)
            .takeWhile { it !== anonymousClass }
            .any { it is PsiExpressionList && it.parent === anonymousClass }
    }

    private class VariableInfo(
        val variable: PsiVariable,
        val paramName: String,
        val fieldName: String?,
        val passToSuper: Boolean = false,
    )
}
