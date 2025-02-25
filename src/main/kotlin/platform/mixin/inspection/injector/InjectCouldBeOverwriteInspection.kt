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

package com.demonwav.mcdev.platform.mixin.inspection.injector

import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.inspection.MixinInspection
import com.demonwav.mcdev.platform.mixin.util.ClassAndMethodNode
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.platform.mixin.util.isClinit
import com.demonwav.mcdev.platform.mixin.util.isConstructor
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.findAnnotation
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.hasImplicitReturnStatement
import com.intellij.analysis.AnalysisScope
import com.intellij.codeInsight.intention.AddAnnotationFix
import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview
import com.intellij.codeInspection.CleanupLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.JoinDeclarationAndAssignmentJavaInspection
import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.dataFlow.interpreter.RunnerResult
import com.intellij.codeInspection.dataFlow.interpreter.StandardDataFlowInterpreter
import com.intellij.codeInspection.dataFlow.java.ControlFlowAnalyzer
import com.intellij.codeInspection.dataFlow.java.inst.MethodCallInstruction
import com.intellij.codeInspection.dataFlow.jvm.JvmDfaMemoryStateImpl
import com.intellij.codeInspection.dataFlow.jvm.descriptors.PlainDescriptor
import com.intellij.codeInspection.dataFlow.lang.DfaListener
import com.intellij.codeInspection.dataFlow.lang.ir.DfaInstructionState
import com.intellij.codeInspection.dataFlow.lang.ir.ReturnInstruction
import com.intellij.codeInspection.dataFlow.types.DfTypes
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory
import com.intellij.codeInspection.ex.GlobalInspectionContextBase
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper
import com.intellij.codeInspection.ex.createSimple
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAssignmentExpression
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiDeclarationStatement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiParameterList
import com.intellij.psi.PsiReturnStatement
import com.intellij.psi.PsiTypes
import com.intellij.psi.codeStyle.VariableKind
import com.intellij.psi.impl.light.LightParameter
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.util.createSmartPointer
import com.siyeh.ig.dataflow.UnnecessaryLocalVariableInspection
import com.siyeh.ig.psiutils.VariableNameGenerator
import org.objectweb.asm.Type

class InjectCouldBeOverwriteInspection : MixinInspection() {
    override fun getStaticDescription() = "Reports when an @Inject is better written as an @Overwrite, " +
        "because the @Inject always cancels and could cause silent mod incompatibilities"

    override fun buildVisitor(holder: ProblemsHolder) = object : JavaElementVisitor() {
        override fun visitMethod(method: PsiMethod) {
            val injectAnnotation = method.findAnnotation(MixinConstants.Annotations.INJECT) ?: return

            // check the inject is cancellable
            val cancellable = injectAnnotation.findAttributeValue("cancellable")?.constantValue as? Boolean
            if (cancellable != true) {
                return
            }

            // check the inject is not optional
            val require = injectAnnotation.findAttributeValue("require")?.constantValue as? Int
            if (require == 0) {
                return
            }

            // check the inject is at HEAD
            val at = injectAnnotation.findAttributeValue("at")?.findAnnotations()?.singleOrNull() ?: return
            if (at.findAttributeValue("value")?.constantStringValue != "HEAD") {
                return
            }

            // check there is only one target
            val targetMethod =
                (MixinAnnotationHandler.resolveTarget(injectAnnotation).singleOrNull() as? MethodTargetMember)
                    ?.classAndMethod ?: return

            // can't overwrite constructors / static initializers
            if (targetMethod.method.isConstructor || targetMethod.method.isClinit) {
                return
            }

            if (!isDefinitelyCancelled(holder.project, method)) {
                return
            }

            holder.registerProblem(
                method.nameIdentifier ?: return,
                "@Inject could be @Overwrite",
                ReplaceInjectWithOverwriteQuickFix(method, targetMethod)
            )
        }
    }

    private fun isDefinitelyCancelled(project: Project, method: PsiMethod): Boolean {
        val methodBody = method.body ?: return false
        val ciParam = method.parameterList.parameters.firstOrNull(::isCallbackInfoParam) ?: return false
        val ciClass = (ciParam.type as? PsiClassType)?.resolve() ?: return false

        val factory = DfaValueFactory(project)
        val flow = ControlFlowAnalyzer.buildFlow(methodBody, factory, true) ?: return false

        val falseValue = factory.fromDfType(DfTypes.FALSE)
        val trueValue = factory.fromDfType(DfTypes.TRUE)

        val memState = JvmDfaMemoryStateImpl(factory)
        val stableCiVar = PlainDescriptor.createVariableValue(
            factory,
            LightParameter("stableCi", ciParam.type, methodBody)
        )
        val ciVar = PlainDescriptor.createVariableValue(factory, ciParam)
        memState.applyCondition(ciVar.eq(stableCiVar))
        val isCancelledVar = PlainDescriptor.createVariableValue(
            factory,
            LightParameter("isCancelled", PsiTypes.booleanType(), methodBody)
        )
        memState.setVarValue(isCancelledVar, falseValue)

        val cancelMethodName =
            if (ciClass.qualifiedName == MixinConstants.Classes.CALLBACK_INFO) "cancel" else "setReturnValue"
        val cancelMethod = ciClass.findMethodsByName(cancelMethodName, false).singleOrNull() ?: return false

        val interpreter = object : StandardDataFlowInterpreter(flow, DfaListener.EMPTY) {
            var definitelyCancelled = true

            override fun acceptInstruction(instructionState: DfaInstructionState): Array<DfaInstructionState> {
                val instruction = instructionState.instruction
                val memoryState = instructionState.memoryState

                when (instruction) {
                    is MethodCallInstruction -> {
                        if (instruction.targetMethod != cancelMethod) {
                            return super.acceptInstruction(instructionState)
                        }
                        if (!memoryState.areEqual(ciVar, stableCiVar)) {
                            return super.acceptInstruction(instructionState)
                        }
                        memoryState.setVarValue(isCancelledVar, trueValue)
                    }

                    is ReturnInstruction -> {
                        if (!memoryState.areEqual(isCancelledVar, trueValue)) {
                            definitelyCancelled = false
                        }
                    }
                }

                return super.acceptInstruction(instructionState)
            }
        }

        if (interpreter.interpret(memState) != RunnerResult.OK) {
            return false
        }

        return interpreter.definitelyCancelled
    }

    private class ReplaceInjectWithOverwriteQuickFix(
        method: PsiMethod,
        @SafeFieldForPreview private val targetMethod: ClassAndMethodNode
    ) : LocalQuickFixOnPsiElement(method) {
        override fun getFamilyName() = "Replace @Inject with @Overwrite"
        override fun getText() = "Replace @Inject with @Overwrite"

        override fun invoke(project: Project, file: PsiFile, startElement: PsiElement, endElement: PsiElement) {
            val oldMethod = startElement as? PsiMethod ?: return

            val templateMethod = targetMethod.method.findOrConstructSourceMethod(targetMethod.clazz, project)

            val oldBody = oldMethod.body ?: return

            val targetReturnType = Type.getReturnType(targetMethod.method.desc)
            val isTargetVoidMethod = targetReturnType == Type.VOID_TYPE
            val cancelMethod = if (isTargetVoidMethod) {
                JavaPsiFacade.getInstance(project)
                    .findClass(MixinConstants.Classes.CALLBACK_INFO, oldMethod.resolveScope)
                    ?.findMethodsByName("cancel", false)
                    ?.singleOrNull()
            } else {
                JavaPsiFacade.getInstance(project)
                    .findClass(MixinConstants.Classes.CALLBACK_INFO_RETURNABLE, oldMethod.resolveScope)
                    ?.findMethodsByName("setReturnValue", false)
                    ?.singleOrNull()
            }

            // if non-void, create return variable
            val elementFactory = JavaPsiFacade.getElementFactory(project)
            var retVariableName: String? = null
            if (!isTargetVoidMethod) {
                retVariableName = VariableNameGenerator(oldBody, VariableKind.LOCAL_VARIABLE)
                    .byName("ret")
                    .generate(true)

                val hasImplicitReturnStatement = hasImplicitReturnStatement(oldBody)

                val elementToAdd = elementFactory.createStatementFromText(
                    "Object $retVariableName;",
                    oldBody
                ) as PsiDeclarationStatement
                val localVariable = elementToAdd.declaredElements[0] as PsiLocalVariable
                localVariable.typeElement.replace(elementFactory.createTypeElement(templateMethod.returnType ?: return))
                oldBody.addAfter(elementToAdd, oldBody.lBrace)

                if (hasImplicitReturnStatement) {
                    oldBody.addBefore(
                        elementFactory.createStatementFromText("return $retVariableName;", oldBody),
                        oldBody.rBrace
                    )
                }
            }

            // delete all cancellation statements and if non-void, replace them with assignments to the return variable
            val cancelCalls = mutableListOf<PsiMethodCallExpression>()
            val returnStatements = mutableListOf<PsiReturnStatement>()
            oldBody.accept(object : JavaRecursiveElementWalkingVisitor() {
                override fun visitClass(clazz: PsiClass) {
                    // don't recurse into nested classes
                }

                override fun visitMethod(method: PsiMethod) {
                    // don't recurse into nested methods
                }

                override fun visitLambdaExpression(expression: PsiLambdaExpression) {
                    // don't recurse into lambdas
                }

                override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                    if (expression.resolveMethod() == cancelMethod) {
                        cancelCalls += expression
                    }
                }

                override fun visitReturnStatement(statement: PsiReturnStatement) {
                    returnStatements += statement
                }
            })

            for (cancelCall in cancelCalls) {
                if (isTargetVoidMethod) {
                    cancelCall.delete()
                } else {
                    val argument = cancelCall.argumentList.expressions.firstOrNull()
                    if (argument != null) {
                        val newExpression = elementFactory.createExpressionFromText(
                            "$retVariableName = argument",
                            cancelCall
                        ) as PsiAssignmentExpression
                        newExpression.rExpression!!.replace(argument)
                        cancelCall.replace(newExpression)
                    }
                }
            }

            if (!isTargetVoidMethod) {
                for (returnStatement in returnStatements) {
                    returnStatement.replace(
                        elementFactory.createStatementFromText("return $retVariableName;", returnStatement)
                    )
                }
            }

            // delete parameters not before the callback info parameter
            val paramsToDelete = oldMethod.parameterList.parameters.asSequence()
                .dropWhile { !isCallbackInfoParam(it) }
                .map { it.createSmartPointer(project) }
                .toList()
            for (param in paramsToDelete) {
                param.element?.delete()
            }

            // replace the method with a template overwrite method
            val newBody = oldBody.copy()
            val newParameterList = oldMethod.parameterList.copy() as PsiParameterList
            val newMethod = oldMethod.replace(templateMethod) as PsiMethod

            // add the @Overwrite annotation
            AddAnnotationFix(MixinConstants.Annotations.OVERWRITE, newMethod).applyFix()

            // if the old method includes the parameters of the target method, use those
            if (!newParameterList.isEmpty) {
                newMethod.parameterList.replace(newParameterList)
            }

            // replace the method body
            newMethod.body?.replace(newBody)

            if (!isTargetVoidMethod) {
                val inspectionManager = InspectionManager.getInstance(project)
                val globalContext = inspectionManager.createNewGlobalContext() as GlobalInspectionContextBase
                val scope = AnalysisScope(LocalSearchScope(newMethod), project)

                // join declarations and assignments
                val joinDeclarationAndAssignmentsProfile = createSimple(
                    "join declarations and assignments",
                    project,
                    listOf(LocalInspectionToolWrapper(CleanupJoinDeclarationAndAssignmentInspection()))
                )
                globalContext.codeCleanup(scope, joinDeclarationAndAssignmentsProfile, null, {
                    // remove unnecessary local variables
                    val unnecessaryLocalVariableProfile = createSimple(
                        "unnecessary local variable",
                        project,
                        listOf(LocalInspectionToolWrapper(CleanupUnnecessaryLocalVariableInspection()))
                    )
                    globalContext.codeCleanup(scope, unnecessaryLocalVariableProfile, null, null, true)
                }, true)
            }
        }
    }

    private class CleanupJoinDeclarationAndAssignmentInspection :
        JoinDeclarationAndAssignmentJavaInspection(),
        CleanupLocalInspectionTool {
        override fun getDisplayName() = "Join declarations and assignments"
    }

    private class CleanupUnnecessaryLocalVariableInspection :
        UnnecessaryLocalVariableInspection(),
        CleanupLocalInspectionTool {
        override fun getDisplayName() = "Unnecessary local variable"
    }

    companion object {
        private fun isCallbackInfoParam(param: PsiParameter): Boolean {
            val type = (param.type as? PsiClassType)?.resolve() ?: return false
            val qName = type.qualifiedName ?: return false
            return qName == MixinConstants.Classes.CALLBACK_INFO ||
                qName == MixinConstants.Classes.CALLBACK_INFO_RETURNABLE
        }
    }
}
