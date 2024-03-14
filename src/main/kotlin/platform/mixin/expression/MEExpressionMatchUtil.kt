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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArrayAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MECapturingExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEInstantiationExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMemberAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENewArrayExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEParenthesizedExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStaticMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MESuperCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.psi.MEMatchableElement
import com.demonwav.mcdev.platform.mixin.expression.psi.MEPsiUtil
import com.demonwav.mcdev.platform.mixin.expression.psi.MERecursiveWalkingVisitor
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.AtResolver
import com.demonwav.mcdev.platform.mixin.handlers.injectionPoint.CollectVisitor
import com.demonwav.mcdev.platform.mixin.util.LocalInfo
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.cached
import com.demonwav.mcdev.platform.mixin.util.canonicalName
import com.demonwav.mcdev.platform.mixin.util.isPrimitive
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.descriptor
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingModifierList
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.findMultiInjectionHost
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.packageName
import com.demonwav.mcdev.util.resolveType
import com.demonwav.mcdev.util.resolveTypeArray
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents
import com.intellij.util.PlatformIcons
import com.llamalad7.mixinextras.expression.impl.ExpressionParserFacade
import com.llamalad7.mixinextras.expression.impl.ast.expressions.Expression
import com.llamalad7.mixinextras.expression.impl.flow.ComplexFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.FlowInterpreter
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool
import java.util.Collections
import java.util.IdentityHashMap
import org.apache.commons.lang3.mutable.MutableInt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer

typealias IdentifierPoolFactory = (MethodNode) -> IdentifierPool
typealias FlowMap = Map<AbstractInsnNode, FlowValue>

object MEExpressionMatchUtil {
    private val LOGGER = logger<MEExpressionMatchUtil>()

    fun getFlowMap(project: Project, classIn: ClassNode, methodIn: MethodNode): FlowMap? {
        if (methodIn.instructions == null) {
            return null
        }

        return methodIn.cached(classIn, project) { classNode, methodNode ->
            val interpreter = object : FlowInterpreter(classNode, methodNode) {
                override fun newValue(type: Type?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.newValue(type)
                }

                override fun newOperation(insn: AbstractInsnNode?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.newOperation(insn)
                }

                override fun copyOperation(insn: AbstractInsnNode?, value: FlowValue?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.copyOperation(insn, value)
                }

                override fun unaryOperation(insn: AbstractInsnNode?, value: FlowValue?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.unaryOperation(insn, value)
                }

                override fun binaryOperation(
                    insn: AbstractInsnNode?,
                    value1: FlowValue?,
                    value2: FlowValue?
                ): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.binaryOperation(insn, value1, value2)
                }

                override fun ternaryOperation(
                    insn: AbstractInsnNode?,
                    value1: FlowValue?,
                    value2: FlowValue?,
                    value3: FlowValue?
                ): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.ternaryOperation(insn, value1, value2, value3)
                }

                override fun naryOperation(insn: AbstractInsnNode?, values: MutableList<out FlowValue>?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.naryOperation(insn, values)
                }

                override fun returnOperation(insn: AbstractInsnNode?, value: FlowValue?, expected: FlowValue?) {
                    ProgressManager.checkCanceled()
                    super.returnOperation(insn, value, expected)
                }

                override fun merge(value1: FlowValue?, value2: FlowValue?): FlowValue? {
                    ProgressManager.checkCanceled()
                    return super.merge(value1, value2)
                }
            }

            try {
                Analyzer(interpreter).analyze(classNode.name, methodNode)
            } catch (e: RuntimeException) {
                if (e is ProcessCanceledException) {
                    throw e
                }
                LOGGER.warn("MEExpressionMatchUtil.getFlowMap failed", e)
                return@cached null
            }

            interpreter.finish()
        }
    }

    fun createIdentifierPoolFactory(
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

    fun createExpression(text: String): Expression? {
        return try {
            ExpressionParserFacade.parse(text)
        } catch (e: Exception) {
            null
        } catch (e: StackOverflowError) {
            null
        }
    }

    inline fun findMatchingInstructions(
        targetClass: ClassNode,
        targetMethod: MethodNode,
        pool: IdentifierPool,
        flows: FlowMap,
        expr: Expression,
        insns: Iterable<AbstractInsnNode>,
        forCompletion: Boolean,
        callback: (ExpressionMatch) -> Unit
    ) {
        for (insn in insns) {
            val decorations = IdentityHashMap<AbstractInsnNode, MutableMap<String, Any?>>()
            val captured = mutableListOf<Pair<FlowValue, Int>>()

            val sink = object : Expression.OutputSink {
                override fun capture(node: FlowValue, expr: Expression?) {
                    captured += node to (expr?.src?.startIndex ?: 0)
                    decorations.getOrPut(insn, ::mutableMapOf).putAll(node.decorations)
                }

                override fun decorate(insn: AbstractInsnNode, key: String, value: Any?) {
                    decorations.getOrPut(insn, ::mutableMapOf)[key] = value
                }

                override fun decorateInjectorSpecific(insn: AbstractInsnNode, key: String, value: Any?) {
                    // Our maps are per-injector anyway, so this is just a normal decoration.
                    decorations.getOrPut(insn, ::mutableMapOf)[key] = value
                }
            }

            val flow = flows[insn] ?: continue
            try {
                if (expr.matches(flow, ExpressionContext(pool, sink, targetClass, targetMethod, forCompletion))) {
                    for ((capturedFlow, startOffset) in captured) {
                        callback(ExpressionMatch(flow, startOffset, decorations[capturedFlow.insn].orEmpty()))
                    }
                }
            } catch (e: ProcessCanceledException) {
                throw e
            } catch (ignored: Exception) {
                // MixinExtras throws lots of different exceptions
            }
        }
    }

    fun getCompletionVariantsFromBytecode(project: Project, contextElement: PsiElement): List<LookupElement> {
        val statement = contextElement.parentOfType<MEStatement>() ?: return emptyList()

        val expressionAnnotation = contextElement.findMultiInjectionHost()?.parentOfType<PsiAnnotation>()
            ?: return emptyList()
        if (!expressionAnnotation.hasQualifiedName(MixinConstants.MixinExtras.EXPRESSION)) {
            return emptyList()
        }

        val modifierList = expressionAnnotation.findContainingModifierList() ?: return emptyList()
        val module = modifierList.findModule() ?: return emptyList()

        val mixinClass = modifierList.findContainingClass() ?: return emptyList()

        val (handler, handlerAnnotation) = modifierList.annotations.mapFirstNotNull { annotation ->
            val qName = annotation.qualifiedName ?: return@mapFirstNotNull null
            val handler = MixinAnnotationHandler.forMixinAnnotation(qName, project) ?: return@mapFirstNotNull null
            handler to annotation
        } ?: return emptyList()

        val cursorOffset = contextElement.textRange.startOffset

        return mixinClass.mixinTargets.flatMap { targetClass ->
            val poolFactory = createIdentifierPoolFactory(module, targetClass, modifierList)
            handler.resolveTarget(handlerAnnotation, targetClass)
                .filterIsInstance<MethodTargetMember>()
                .flatMap { methodTarget ->

                    getCompletionVariantsFromBytecode(
                        project,
                        mixinClass,
                        cursorOffset,
                        statement.copy() as MEStatement,
                        targetClass,
                        methodTarget.classAndMethod.method,
                        poolFactory,
                    )
                }
        }
    }

    private fun getCompletionVariantsFromBytecode(
        project: Project,
        mixinClass: PsiClass,
        cursorOffsetIn: Int,
        statement: MEStatement,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        poolFactory: IdentifierPoolFactory,
    ): List<LookupElement> {
        if (targetMethod.instructions == null) {
            return emptyList()
        }

        val cursorOffset = MutableInt(cursorOffsetIn)
        val pool = poolFactory(targetMethod)
        val flows = getFlowMap(project, targetClass, targetMethod) ?: return emptyList()

        removeExplicitCaptures(statement, cursorOffset)
        replaceUnknownNamesWithWildcards(project, statement, cursorOffset, pool)

        val statementToMatch = statement.copy() as MEStatement
        replaceCursorInputWithWildcard(project, statementToMatch, cursorOffset.toInt())

        val meStatement = createExpression(statementToMatch.text) ?: return emptyList()
        val matchingFlows = mutableListOf<FlowValue>()
        findMatchingInstructions(
            targetClass,
            targetMethod,
            pool,
            flows,
            meStatement,
            targetMethod.instructions,
            true
        ) { match ->
            matchingFlows += match.flow
        }
        if (matchingFlows.isEmpty()) {
            return emptyList()
        }

        var subExpr: MEMatchableElement = statement
        while (true) {
            val inputExprOnCursor = subExpr.getInputExprs().firstOrNull { it.textRange.contains(cursorOffset.toInt()) }
                ?: break
            val exprToMatch = inputExprOnCursor.copy() as MEExpression
            replaceCursorInputWithWildcard(project, exprToMatch, cursorOffset.toInt())
            val meExpression = createExpression(exprToMatch.text) ?: return emptyList()

            val flattenedInstructions = mutableSetOf<AbstractInsnNode>()
            for (flow in matchingFlows) {
                getInstructionsInFlowTree(
                    flow,
                    flattenedInstructions,
                    subExpr !is MEExpressionStatement && subExpr !is MEParenthesizedExpression
                )
            }

            matchingFlows.clear()
            findMatchingInstructions(
                targetClass,
                targetMethod,
                pool,
                flows,
                meExpression,
                flattenedInstructions,
                true
            ) { match ->
                matchingFlows += match.flow
            }
            if (matchingFlows.isEmpty()) {
                return emptyList()
            }

            subExpr = inputExprOnCursor
        }

        val cursorInstructions = mutableSetOf<AbstractInsnNode>()
        for (flow in matchingFlows) {
            getInstructionsInFlowTree(flow, cursorInstructions, false)
        }

        val elementAtCursor = statement.findElementAt(cursorOffset.toInt()) ?: return emptyList()

        val isInsideMeType = PsiTreeUtil.getParentOfType(
            elementAtCursor,
            METype::class.java,
            false,
            MEExpression::class.java
        ) != null
        val cursorExprInTypePosition = !isInsideMeType &&
            elementAtCursor.parentOfType<MEExpression>()?.let(METypeUtil::isExpressionInTypePosition) == true
        val inTypePosition = isInsideMeType || cursorExprInTypePosition
        val isPossiblyIncompleteCast = !inTypePosition &&
            elementAtCursor.parentOfType<MEExpression>()
                ?.parents(false)
                ?.dropWhile { it is MEArrayAccessExpression && it.indexExpr == null } is MEParenthesizedExpression
        val canCompleteExprs = !inTypePosition
        val canCompleteTypes = inTypePosition || isPossiblyIncompleteCast

        return cursorInstructions.mapNotNull { insn ->
            getCompletionForInstruction(insn, mixinClass, canCompleteExprs, canCompleteTypes)
        }
    }

    private fun replaceUnknownNamesWithWildcards(
        project: Project,
        statement: MEStatement,
        cursorOffset: MutableInt,
        pool: IdentifierPool,
    ) {
        val unknownNames = mutableListOf<MEName>()
        statement.accept(object : MERecursiveWalkingVisitor() {
            override fun visitType(o: METype) {
                val name = o.meName
                if (!name.isWildcard && !pool.typeExists(name.text)) {
                    unknownNames += name
                }
            }

            override fun visitNameExpression(o: MENameExpression) {
                val name = o.meName
                if (!name.isWildcard) {
                    if (METypeUtil.isExpressionDirectlyInTypePosition(o)) {
                        if (!pool.typeExists(name.text)) {
                            unknownNames += name
                        }
                    } else {
                        if (!pool.memberExists(name.text)) {
                            unknownNames += name
                        }
                    }
                }
            }

            override fun visitSuperCallExpression(o: MESuperCallExpression) {
                val name = o.memberName
                if (name != null && !name.isWildcard && !pool.memberExists(name.text)) {
                    unknownNames += name
                }
                super.visitSuperCallExpression(o)
            }

            override fun visitMethodCallExpression(o: MEMethodCallExpression) {
                val name = o.memberName
                if (!name.isWildcard && !pool.memberExists(name.text)) {
                    unknownNames += name
                }
                super.visitMethodCallExpression(o)
            }

            override fun visitStaticMethodCallExpression(o: MEStaticMethodCallExpression) {
                val name = o.memberName
                if (!name.isWildcard && !pool.memberExists(name.text)) {
                    unknownNames += name
                }
                super.visitStaticMethodCallExpression(o)
            }

            override fun visitMemberAccessExpression(o: MEMemberAccessExpression) {
                val name = o.memberName
                if (!name.isWildcard && !pool.memberExists(name.text)) {
                    unknownNames += name
                }
                super.visitMemberAccessExpression(o)
            }

            override fun visitInstantiationExpression(o: MEInstantiationExpression) {
                val name = o.type
                if (!name.isWildcard && !pool.typeExists(name.text)) {
                    unknownNames += name
                }
                super.visitInstantiationExpression(o)
            }

            override fun visitNewArrayExpression(o: MENewArrayExpression) {
                val name = o.elementType
                if (!name.isWildcard && !pool.typeExists(name.text)) {
                    unknownNames += name
                }
                super.visitNewArrayExpression(o)
            }
        })

        for (unknownName in unknownNames) {
            val startOffset = unknownName.textRange.startOffset
            if (cursorOffset.toInt() > startOffset) {
                cursorOffset.setValue(cursorOffset.toInt() - unknownName.textLength + 1)
            }

            unknownName.replace(project.meExpressionElementFactory.createName("?"))
        }
    }

    private fun removeExplicitCaptures(statement: MEStatement, cursorOffset: MutableInt) {
        val captures = mutableListOf<MECapturingExpression>()

        statement.accept(object : MERecursiveWalkingVisitor() {
            override fun elementFinished(element: PsiElement) {
                // do this on elementFinished to ensure that inner captures are replaced before outer captures
                if (element is MECapturingExpression) {
                    captures += element
                }
            }
        })

        for (capture in captures) {
            val innerExpr = capture.expression ?: continue
            val textRange = capture.textRange

            if (cursorOffset.toInt() > textRange.startOffset) {
                cursorOffset.setValue(cursorOffset.toInt() - if (cursorOffset.toInt() >= textRange.endOffset) 3 else 2)
            }

            capture.replace(innerExpr)
        }
    }

    private fun replaceCursorInputWithWildcard(project: Project, element: MEMatchableElement, cursorOffset: Int) {
        for (input in element.getInputExprs()) {
            if (input.textRange.contains(cursorOffset)) {
                input.replace(project.meExpressionElementFactory.createExpression("?"))
                return
            }
        }
    }

    private fun getInstructionsInFlowTree(
        flow: FlowValue,
        outInstructions: MutableSet<AbstractInsnNode>,
        strict: Boolean
    ) {
        if (flow is DummyFlowValue || flow is ComplexFlowValue) {
            return
        }

        if (!strict) {
            outInstructions += flow.insn
        }
        for (i in 0 until flow.inputCount()) {
            getInstructionsInFlowTree(flow.getInput(i), outInstructions, false)
        }
    }

    private fun getCompletionForInstruction(
        insn: AbstractInsnNode,
        mixinClass: PsiClass,
        canCompleteExprs: Boolean,
        canCompleteTypes: Boolean
    ): LookupElement? {
        when (insn) {
            is LdcInsnNode -> {
                when (val cst = insn.cst) {
                    is Type -> {
                        if (canCompleteTypes && cst.isAccessibleFrom(mixinClass)) {
                            return object : TailTypeDecorator<LookupElement>(createTypeLookup(cst)) {
                                override fun computeTailType(context: InsertionContext?) =
                                    MEExpressionCompletionContributor.DOT_CLASS_TAIL
                            }
                        }
                    }
                    // TODO: string literals?
                }
            }
            is VarInsnNode -> {
                // TODO: local variables
            }
            is IincInsnNode -> {
                // TODO: local variables
            }
            is FieldInsnNode -> {
                if (canCompleteExprs) {
                    val at = "at = @${MixinConstants.Annotations.AT}(value = \"FIELD\"," +
                        " target = \"L${insn.owner};${insn.name}:${insn.desc}\")"
                    var lookup = LookupElementBuilder.create(insn.name.toValidIdentifier())
                        .withIcon(PlatformIcons.FIELD_ICON)
                        .withPresentableText(insn.owner.substringAfterLast('/') + "." + insn.name)
                        .withDefinition(insn.name.toValidIdentifier(), at)
                    if (insn.opcode == Opcodes.GETSTATIC || insn.opcode == Opcodes.PUTSTATIC) {
                        lookup = lookup.withLookupString(insn.owner.substringAfterLast('/') + "." + insn.name)
                    }
                    return lookup
                }
            }
            is MethodInsnNode -> {
                if (canCompleteExprs) {
                    val at = "at = @${MixinConstants.Annotations.AT}(value = \"INVOKE\"," +
                        " target = \"L${insn.owner};${insn.name}${insn.desc}\")"
                    var lookup = LookupElementBuilder.create(insn.name.toValidIdentifier())
                        .withIcon(PlatformIcons.METHOD_ICON)
                        .withPresentableText(insn.owner.substringAfterLast('/') + "." + insn.name)
                        .withDefinition(insn.name.toValidIdentifier(), at)
                    if (insn.opcode == Opcodes.INVOKESTATIC) {
                        lookup = lookup.withLookupString(insn.owner.substringAfterLast('/') + "." + insn.name)
                    }
                    return lookup
                }
            }
            is TypeInsnNode -> {
                // TODO: put cursor in right position for array lengths for array creation
                val type = Type.getObjectType(insn.desc)
                if (canCompleteTypes && type.isAccessibleFrom(mixinClass)) {
                    val lookup = createTypeLookup(type)
                    if (insn.opcode == Opcodes.ANEWARRAY) {
                        return object : TailTypeDecorator<LookupElement>(lookup) {
                            override fun computeTailType(context: InsertionContext?) =
                                MEExpressionCompletionContributor.BracketsTailType(1)
                        }
                    } else {
                        return lookup
                    }
                }
            }
            is IntInsnNode -> {
                if (insn.opcode == Opcodes.NEWARRAY) {
                    val type = when (insn.operand) {
                        Opcodes.T_BOOLEAN -> "boolean"
                        Opcodes.T_CHAR -> "char"
                        Opcodes.T_FLOAT -> "float"
                        Opcodes.T_DOUBLE -> "double"
                        Opcodes.T_BYTE -> "byte"
                        Opcodes.T_SHORT -> "short"
                        Opcodes.T_INT -> "int"
                        Opcodes.T_LONG -> "long"
                        else -> "unknown" // wtf?
                    }
                    return object : TailTypeDecorator<LookupElement>(
                        LookupElementBuilder.create(type).withIcon(PlatformIcons.CLASS_ICON)
                    ) {
                        override fun computeTailType(context: InsertionContext?) =
                            MEExpressionCompletionContributor.BracketsTailType(1)
                    }
                }
            }
            is MultiANewArrayInsnNode -> {
                val type = Type.getType(insn.desc)
                return object : TailTypeDecorator<LookupElement>(
                    createTypeLookup(type.elementType)
                ) {
                    override fun computeTailType(context: InsertionContext?) =
                        MEExpressionCompletionContributor.BracketsTailType(type.dimensions)
                }
            }
            is InsnNode -> {
                when (insn.opcode) {
                    Opcodes.ARRAYLENGTH -> {
                        if (canCompleteExprs) {
                            return LookupElementBuilder.create("length")
                                .withIcon(PlatformIcons.FIELD_ICON)
                        }
                    }
                }
            }
        }

        return null
    }

    private fun Type.typeNameToInsert(): String {
        if (sort == Type.ARRAY) {
            return elementType.typeNameToInsert() + "[]".repeat(dimensions)
        }
        if (sort != Type.OBJECT) {
            return className
        }

        val simpleName = internalName.substringAfterLast('/')
        val lastValidCharIndex = (simpleName.length - 1 downTo 0).firstOrNull {
            MEPsiUtil.isIdentifierStart(simpleName[it])
        } ?: return "_" + simpleName.filterInvalidIdentifierChars()

        return simpleName.substring(simpleName.lastIndexOf('$', lastValidCharIndex) + 1).toValidIdentifier()
    }

    private fun String.toValidIdentifier(): String {
        return when {
            isEmpty() -> "_"
            !MEPsiUtil.isIdentifierStart(this[0]) -> "_" + filterInvalidIdentifierChars()
            else -> this[0] + substring(1).filterInvalidIdentifierChars()
        }
    }

    private fun String.filterInvalidIdentifierChars(): String {
        return asSequence().joinToString("") {
            if (MEPsiUtil.isIdentifierPart(it)) it.toString() else "_"
        }
    }

    private fun Type.presentableName(): String = when (sort) {
        Type.ARRAY -> elementType.presentableName() + "[]".repeat(dimensions)
        Type.OBJECT -> internalName.substringAfterLast('/')
        else -> className
    }

    private fun Type.isAccessibleFrom(fromClass: PsiClass): Boolean {
        return when (sort) {
            Type.ARRAY -> elementType.isAccessibleFrom(fromClass)
            Type.OBJECT -> {
                val facade = JavaPsiFacade.getInstance(fromClass.project)
                val clazz = facade.findClass(canonicalName, fromClass.resolveScope) ?: return false
                val pkg = fromClass.packageName?.let(facade::findPackage) ?: return false
                clazz !is PsiAnonymousClass && PsiUtil.isAccessibleFromPackage(clazz, pkg)
            }
            else -> true
        }
    }

    private fun createTypeLookup(type: Type): LookupElement {
        val definitionId = type.typeNameToInsert()

        val lookupElement = LookupElementBuilder.create(definitionId)
            .withIcon(PlatformIcons.CLASS_ICON)
            .withPresentableText(type.presentableName())

        return if (type.isPrimitive) {
            lookupElement
        } else {
            lookupElement.withDefinition(definitionId, "type = ${type.canonicalName}.class")
        }
    }

    private fun LookupElementBuilder.withDefinition(id: String, at: String) = withInsertHandler { context, _ ->
        val contextElement = context.file.findElementAt(context.startOffset) ?: return@withInsertHandler
        val injectionHost = contextElement.findMultiInjectionHost() ?: return@withInsertHandler
        val expressionAnnotation = injectionHost.parentOfType<PsiAnnotation>() ?: return@withInsertHandler
        if (!expressionAnnotation.hasQualifiedName(MixinConstants.MixinExtras.EXPRESSION)) {
            return@withInsertHandler
        }
        val modifierList = expressionAnnotation.findContainingModifierList() ?: return@withInsertHandler

        // look for an existing definition with this id, skip if it exists
        for (annotation in modifierList.annotations) {
            if (annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION) &&
                annotation.findDeclaredAttributeValue("id")?.constantStringValue == id
            ) {
                return@withInsertHandler
            }
        }

        // create and add the new @Definition annotation
        val newAnnotation = JavaPsiFacade.getElementFactory(context.project).createAnnotationFromText(
            "@${MixinConstants.MixinExtras.DEFINITION}(id = \"$id\", $at)",
            modifierList,
        )
        val addedAnnotation = modifierList.addAfter(newAnnotation, modifierList.annotations.lastOrNull())

        // add imports and reformat
        JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(addedAnnotation)
        JavaCodeStyleManager.getInstance(context.project).optimizeImports(modifierList.containingFile)
        CodeStyleManager.getInstance(context.project).reformat(modifierList)
    }

    class ExpressionMatch @PublishedApi internal constructor(
        val flow: FlowValue,
        val startOffset: Int,
        val decorations: Map<String, Any?>,
    )
}
