/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
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

package com.demonwav.mcdev.platform.mixin.handlers.mixinextras

import com.demonwav.mcdev.platform.mixin.expression.MESourceMatchContext
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MECapturingExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatement
import com.demonwav.mcdev.platform.mixin.expression.meExpressionElementFactory
import com.demonwav.mcdev.platform.mixin.expression.psi.MEExpressionFile
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.InjectionPoint
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.NavigationVisitor
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findContainingModifierList
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.ifEmpty
import com.demonwav.mcdev.util.parseArray
import com.demonwav.mcdev.util.resolveType
import com.demonwav.mcdev.util.resolveTypeArray
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiModifierList
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.llamalad7.mixinextras.expression.impl.ExpressionParserFacade
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool
import java.util.Collections
import java.util.IdentityHashMap
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

private typealias IdentifierPoolFactory = (MethodNode) -> IdentifierPool
private typealias SourceMatchContextFactory = (ClassNode, MethodNode) -> MESourceMatchContext

class ExpressionInjectionPoint : InjectionPoint<PsiElement>() {
    override fun onCompleted(editor: Editor, reference: PsiLiteral) {
        val modifierList = reference.findContainingModifierList() ?: return
        if (modifierList.hasAnnotation(MixinConstants.MixinExtras.EXPRESSION)) {
            return
        }

        val project = reference.project

        val exprAnnotation = modifierList.addAfter(
            JavaPsiFacade.getElementFactory(project)
                .createAnnotationFromText("@${MixinConstants.MixinExtras.EXPRESSION}(\"\")", reference),
            null
        )

        // add imports and reformat
        JavaCodeStyleManager.getInstance(project).shortenClassReferences(exprAnnotation)
        JavaCodeStyleManager.getInstance(project).optimizeImports(modifierList.containingFile)
        val formattedModifierList = CodeStyleManager.getInstance(project).reformat(modifierList) as PsiModifierList

        // move the caret to @Expression("<caret>")
        val formattedExprAnnotation = formattedModifierList.findAnnotation(MixinConstants.MixinExtras.EXPRESSION)
            ?: return
        val exprLiteral = formattedExprAnnotation.findDeclaredAttributeValue(null) ?: return
        editor.caretModel.moveToOffset(exprLiteral.textRange.startOffset + 1)
    }

    override fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass
    ): NavigationVisitor? {
        val project = at.project

        val atId = at.findDeclaredAttributeValue("id")?.constantStringValue ?: ""

        val injectorAnnotation = AtResolver.findInjectorAnnotation(at) ?: return null
        val modifierList = injectorAnnotation.parent as? PsiModifierList ?: return null
        val parsedExprs = parseExpressions(project, modifierList, atId)
        parsedExprs.ifEmpty { return null }

        val sourceMatchContextFactory = createSourceMatchContextFactory(project, modifierList)

        return MyNavigationVisitor(parsedExprs.map { it.second }, sourceMatchContextFactory)
    }

    private fun createSourceMatchContextFactory(
        project: Project,
        modifierList: PsiModifierList
    ): SourceMatchContextFactory = { targetClass, targetMethod ->
        val matchContext = MESourceMatchContext(project)

        for (annotation in modifierList.annotations) {
            if (!annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)) {
                continue
            }

            val definitionId = annotation.findDeclaredAttributeValue("id")?.constantStringValue ?: ""

            val ats = annotation.findDeclaredAttributeValue("at")?.findAnnotations() ?: emptyList()
            for (at in ats) {
                val matches = RecursionManager.doPreventingRecursion(at, true) {
                    AtResolver(at, targetClass, targetMethod).resolveNavigationTargets()
                } ?: continue
                for (target in matches) {
                    matchContext.addTargetedElement(definitionId, target)
                }
            }

            val types = annotation.findDeclaredAttributeValue("type")?.resolveTypeArray() ?: emptyList()
            for (type in types) {
                matchContext.addType(definitionId, type.descriptor)
            }

            val locals = annotation.findDeclaredAttributeValue("local")?.findAnnotations() ?: emptyList()
            for (localAnnotation in locals) {
                val localType = annotation.findDeclaredAttributeValue("type")?.resolveType()
                val localInfo = LocalInfo.fromAnnotation(localType, localAnnotation)
                matchContext.addLocalInfo(definitionId, localInfo)
            }
        }

        matchContext
    }

    override fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode
    ): CollectVisitor<PsiElement>? {
        val project = at.project

        val atId = at.findDeclaredAttributeValue("id")?.constantStringValue ?: ""

        val injectorAnnotation = AtResolver.findInjectorAnnotation(at) ?: return null
        val modifierList = injectorAnnotation.parent as? PsiModifierList ?: return null
        val parsedExprs = parseExpressions(project, modifierList, atId)
        parsedExprs.ifEmpty { return null }

        val module = at.findModule() ?: return null

        val poolFactory = createIdentifierPoolFactory(module, targetClass, modifierList)

        return MyCollectVisitor(mode, targetClass, parsedExprs, poolFactory)
    }

    private fun parseExpressions(
        project: Project,
        modifierList: PsiModifierList,
        atId: String
    ): List<Pair<Expression, MEStatement>> {
        return modifierList.annotations.asSequence()
            .filter { exprAnnotation ->
                exprAnnotation.hasQualifiedName(MixinConstants.MixinExtras.EXPRESSION) &&
                    (exprAnnotation.findDeclaredAttributeValue("id")?.constantStringValue ?: "") == atId
            }
            .flatMap { exprAnnotation ->
                val expressionElements = exprAnnotation.findDeclaredAttributeValue("value")?.parseArray { it }
                    ?: return@flatMap emptySequence<Pair<Expression, MEStatement>>()
                expressionElements.asSequence().mapNotNull { expressionElement ->
                    val text = expressionElement.constantStringValue ?: return@mapNotNull null
                    val rootStatementPsi = InjectedLanguageManager.getInstance(project)
                        .getInjectedPsiFiles(expressionElement)?.firstOrNull()
                        ?.let { (it.first as? MEExpressionFile)?.statement }
                        ?: project.meExpressionElementFactory.createFile(text).statement
                        ?: project.meExpressionElementFactory.createStatement("empty")
                    try {
                        ExpressionParserFacade.parse(text) to rootStatementPsi
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            .toList()
    }

    private fun createIdentifierPoolFactory(
        module: Module,
        targetClass: ClassNode,
        modifierList: PsiModifierList,
    ): IdentifierPoolFactory = { targetMethod ->
        val pool = IdentifierPool()

        for (annotation in modifierList.annotations) {
            if (!annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)) {
                continue
            }

            val definitionId = annotation.findDeclaredAttributeValue("id")?.constantStringValue ?: ""

            val ats = annotation.findDeclaredAttributeValue("at")?.findAnnotations() ?: emptyList()
            for (at in ats) {
                val matchingInsns = RecursionManager.doPreventingRecursion(at, true) {
                    AtResolver(at, targetClass, targetMethod)
                        .resolveInstructions()
                        .mapTo(Collections.newSetFromMap(IdentityHashMap())) { it.insn }
                } ?: emptySet()
                pool.addMember(definitionId) { it in matchingInsns }
            }

            val types = annotation.findDeclaredAttributeValue("type")?.resolveTypeArray() ?: emptyList()
            for (type in types) {
                val asmType = Type.getType(type.descriptor)
                pool.addType(definitionId) { it == asmType }
            }

            val locals = annotation.findDeclaredAttributeValue("local")?.findAnnotations() ?: emptyList()
            for (localAnnotation in locals) {
                val localType = annotation.findDeclaredAttributeValue("type")?.resolveType()
                val localInfo = LocalInfo.fromAnnotation(localType, localAnnotation)
                pool.addMember(definitionId) { insn ->
                    if (insn !is VarInsnNode) {
                        return@addMember false
                    }
                    val actualInsn = if (insn.opcode >= Opcodes.ISTORE && insn.opcode <= Opcodes.ASTORE) {
                        insn.next ?: return@addMember false
                    } else {
                        insn
                    }

                    val unfilteredLocals = localInfo.getLocals(module, targetClass, targetMethod, actualInsn)
                        ?: return@addMember false
                    val filteredLocals = localInfo.matchLocals(unfilteredLocals, CollectVisitor.Mode.MATCH_ALL)
                    filteredLocals.any { it.index == insn.`var` }
                }
            }
        }

        pool
    }

    override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<PsiElement>
    ): LookupElementBuilder? {
        return null
    }

    private class MyCollectVisitor(
        mode: Mode,
        private val targetClass: ClassNode,
        private val expressions: List<Pair<Expression, PsiElement>>,
        private val poolFactory: IdentifierPoolFactory,
    ) : CollectVisitor<PsiElement>(mode) {
        override fun accept(methodNode: MethodNode) {
            val insns = methodNode.instructions ?: return

            val pool = poolFactory(methodNode)
            val flows = try {
                FlowInterpreter.analyze(targetClass, methodNode)
            } catch (e: RuntimeException) {
                return
            }

            val result = IdentityHashMap<AbstractInsnNode, PsiElement>()

            for ((expr, psiExpr) in expressions) {
                insns.iterator().forEachRemaining { insn ->
                    val genericDecorations = IdentityHashMap<AbstractInsnNode, MutableMap<String, Any?>>()
                    val injectorSpecificDecorations = IdentityHashMap<AbstractInsnNode, MutableMap<String, Any?>>()
                    val captured = mutableListOf<AbstractInsnNode>()

                    val sink = object : Expression.OutputSink {
                        override fun capture(node: FlowValue) {
                            captured += node.insn
                        }

                        override fun decorate(insn: AbstractInsnNode, key: String, value: Any?) {
                            genericDecorations.computeIfAbsent(insn) { mutableMapOf() }[key] = value
                        }

                        override fun decorateInjectorSpecific(insn: AbstractInsnNode, key: String, value: Any?) {
                            injectorSpecificDecorations.computeIfAbsent(insn) { mutableMapOf() }[key] = value
                        }
                    }

                    val flow = flows[insn] ?: return@forEachRemaining
                    try {
                        if (expr.matches(flow, ExpressionContext(pool, sink, targetClass, methodNode))) {
                            // TODO: for now we're assuming there's only one capture in the ME expression.
                            val capturedExpr =
                                PsiTreeUtil.findChildOfType(psiExpr, MECapturingExpression::class.java, false)
                                    ?.expression ?: psiExpr

                            for (capturedInsn in captured) {
                                result.putIfAbsent(capturedInsn, capturedExpr)
                            }
                        }
                    } catch (e: ProcessCanceledException) {
                        throw e
                    } catch (ignored: Exception) {
                        // MixinExtras throws lots of different exceptions
                    }
                }
            }

            if (result.isEmpty()) {
                return
            }

            insns.iterator().forEachRemaining { insn ->
                val element = result[insn]
                if (element != null) {
                    addResult(insn, element)
                }
            }
        }
    }

    private class MyNavigationVisitor(
        private val statements: List<MEStatement>,
        private val matchContextFactory: SourceMatchContextFactory
    ) : NavigationVisitor() {
        private lateinit var matchContext: MESourceMatchContext

        override fun configureBytecodeTarget(classNode: ClassNode, methodNode: MethodNode) {
            matchContext = matchContextFactory(classNode, methodNode)
        }

        override fun visitElement(element: PsiElement) {
            for (statement in statements) {
                if (statement.matchesJava(element, matchContext)) {
                    if (matchContext.captures.isNotEmpty()) {
                        for (capture in matchContext.captures) {
                            addResult(capture)
                        }
                    } else {
                        addResult(element)
                    }
                }
                matchContext.reset()
            }

            super.visitElement(element)
        }
    }
}
