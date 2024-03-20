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

import com.demonwav.mcdev.platform.mixin.expression.MEExpressionMatchUtil.insnOrNull
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArrayAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEAssignStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MECapturingExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MECastExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEClassConstantExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MELitExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMemberAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEName
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENameExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MENewExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEParenthesizedExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStatementItem
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEStaticMethodCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MESuperCallExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.METype
import com.demonwav.mcdev.platform.mixin.expression.psi.MEMatchableElement
import com.demonwav.mcdev.platform.mixin.expression.psi.MEPsiUtil
import com.demonwav.mcdev.platform.mixin.expression.psi.MERecursiveWalkingVisitor
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil.notInTypePosition
import com.demonwav.mcdev.platform.mixin.expression.psi.METypeUtil.validType
import com.demonwav.mcdev.platform.mixin.handlers.MixinAnnotationHandler
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.canonicalName
import com.demonwav.mcdev.platform.mixin.util.isPrimitive
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.textify
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingModifierList
import com.demonwav.mcdev.util.findContainingNameValuePair
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.findMultiInjectionHost
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.packageName
import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifierList
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents
import com.intellij.util.PlatformIcons
import com.intellij.util.text.CharArrayUtil
import com.llamalad7.mixinextras.expression.impl.flow.ComplexFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool
import com.llamalad7.mixinextras.utils.Decorations
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

object MEExpressionCompletionUtil {
    private const val DEBUG_COMPLETION = false

    private val NORMAL_ELEMENT = PlatformPatterns.psiElement()
        .inside(MEStatement::class.java)
        .andNot(PlatformPatterns.psiElement().inside(MELitExpression::class.java))
        .notInTypePosition()
    private val TYPE_PATTERN = PlatformPatterns.psiElement()
        .inside(MEStatement::class.java)
        .validType()
    private val AFTER_END_EXPRESSION_PATTERN = PlatformPatterns.psiElement().afterLeaf(
        PlatformPatterns.psiElement().withElementType(
            TokenSet.create(
                MEExpressionTypes.TOKEN_IDENTIFIER,
                MEExpressionTypes.TOKEN_WILDCARD,
                MEExpressionTypes.TOKEN_RIGHT_PAREN,
                MEExpressionTypes.TOKEN_RIGHT_BRACKET,
                MEExpressionTypes.TOKEN_RIGHT_BRACE,
                MEExpressionTypes.TOKEN_BOOL_LIT,
                MEExpressionTypes.TOKEN_CLASS,
                MEExpressionTypes.TOKEN_INT_LIT,
                MEExpressionTypes.TOKEN_DEC_LIT,
                MEExpressionTypes.TOKEN_NULL_LIT,
                MEExpressionTypes.TOKEN_STRING_TERMINATOR,
            )
        )
    )

    val STATEMENT_KEYWORD_PLACE = PlatformPatterns.psiElement().afterLeaf(
        PlatformPatterns.psiElement().withText("{").withParent(MEStatementItem::class.java)
    )
    val VALUE_KEYWORD_PLACE = StandardPatterns.and(
        NORMAL_ELEMENT,
        StandardPatterns.not(AFTER_END_EXPRESSION_PATTERN),
        StandardPatterns.not(PlatformPatterns.psiElement().afterLeaf(".")),
    )
    val CLASS_PLACE = StandardPatterns.and(
        NORMAL_ELEMENT,
        PlatformPatterns.psiElement()
            .afterLeaf(
                PlatformPatterns.psiElement().withText(".")
                    .withParent(PlatformPatterns.psiElement().withFirstChild(TYPE_PATTERN))
            ),
    )
    val INSTANCEOF_PLACE = StandardPatterns.and(
        NORMAL_ELEMENT,
        AFTER_END_EXPRESSION_PATTERN,
    )
    val FROM_BYTECODE_PLACE = PlatformPatterns.psiElement()
        .inside(MEStatement::class.java)
        .andNot(PlatformPatterns.psiElement().inside(MELitExpression::class.java))

    private val DOT_CLASS_TAIL = object : TailType() {
        override fun processTail(editor: Editor, tailOffset: Int): Int {
            editor.document.insertString(tailOffset, ".class")
            return moveCaret(editor, tailOffset, 6)
        }

        override fun isApplicable(context: InsertionContext): Boolean {
            val chars = context.document.charsSequence
            val dotOffset = CharArrayUtil.shiftForward(chars, context.tailOffset, " \n\t")
            if (!CharArrayUtil.regionMatches(chars, dotOffset, ".")) {
                return true
            }
            val classOffset = CharArrayUtil.shiftForward(chars, dotOffset + 1, " \n\t")
            return !CharArrayUtil.regionMatches(chars, classOffset, "class")
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

        val cursorOffset = contextElement.textRange.startOffset - statement.textRange.startOffset

        return mixinClass.mixinTargets.flatMap { targetClass ->
            val poolFactory = MEExpressionMatchUtil.createIdentifierPoolFactory(module, targetClass, modifierList)
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
        if (DEBUG_COMPLETION) {
            println("======")
        }

        if (targetMethod.instructions == null) {
            return emptyList()
        }

        val cursorOffset = MutableInt(cursorOffsetIn)
        val pool = poolFactory(targetMethod)
        val flows = MEExpressionMatchUtil.getFlowMap(project, targetClass, targetMethod) ?: return emptyList()

        removeExplicitCaptures(statement, cursorOffset)
        replaceUnknownNamesWithWildcards(project, statement, cursorOffset, pool)

        val elementAtCursor = statement.findElementAt(cursorOffset.toInt()) ?: return emptyList()

        val wildcardReplacedStatement = statement.copy() as MEStatement
        replaceCursorInputWithWildcard(project, wildcardReplacedStatement, cursorOffset.toInt())

        var matchingFlows = mutableListOf<FlowValue>()
        for (statementToMatch in getStatementVariants(project.meExpressionElementFactory, wildcardReplacedStatement)) {
            if (DEBUG_COMPLETION) {
                println("Matching against statement ${statementToMatch.text}")
            }

            val meStatement = MEExpressionMatchUtil.createExpression(statementToMatch.text) ?: continue
            MEExpressionMatchUtil.findMatchingInstructions(
                targetClass,
                targetMethod,
                pool,
                flows,
                meStatement,
                targetMethod.instructions,
                true
            ) { match ->
                matchingFlows += match.flow
                if (DEBUG_COMPLETION) {
                    println("Matched ${match.flow.insnOrNull?.textify()}")
                }
            }
        }
        if (matchingFlows.isEmpty()) {
            return emptyList()
        }

        var subExpr: MEMatchableElement = statement
        while (true) {
            val inputExprOnCursor = subExpr.getInputExprs().firstOrNull { it.textRange.contains(cursorOffset.toInt()) }
                ?: break
            val wildcardReplacedExpr = inputExprOnCursor.copy() as MEExpression
            cursorOffset.setValue(cursorOffset.toInt() - inputExprOnCursor.textRange.startOffset)
            replaceCursorInputWithWildcard(project, wildcardReplacedExpr, cursorOffset.toInt())

            val newMatchingFlows = mutableSetOf<FlowValue>()
            for (exprToMatch in getExpressionVariants(project.meExpressionElementFactory, wildcardReplacedExpr)) {
                if (DEBUG_COMPLETION) {
                    println("Matching against expression ${exprToMatch.text}")
                }

                val meExpression = MEExpressionMatchUtil.createExpression(exprToMatch.text) ?: continue

                val flattenedInstructions = mutableSetOf<AbstractInsnNode>()
                for (flow in matchingFlows) {
                    getInstructionsInFlowTree(
                        findFlowTreeRoot(flow),
                        flattenedInstructions,
                        subExpr !is MEExpressionStatement && subExpr !is MEParenthesizedExpression
                    )
                }

                MEExpressionMatchUtil.findMatchingInstructions(
                    targetClass,
                    targetMethod,
                    pool,
                    flows,
                    meExpression,
                    flattenedInstructions,
                    true
                ) { match ->
                    newMatchingFlows += match.flow
                    if (DEBUG_COMPLETION) {
                        println("Matched ${match.flow.insnOrNull?.textify()}")
                    }
                }
            }

            if (newMatchingFlows.isEmpty()) {
                return emptyList()
            }
            matchingFlows = newMatchingFlows.toMutableList()

            subExpr = inputExprOnCursor
        }

        val cursorInstructions = mutableSetOf<AbstractInsnNode>()
        for (flow in matchingFlows) {
            getInstructionsInFlowTree(findFlowTreeRoot(flow), cursorInstructions, false)
        }

        if (DEBUG_COMPLETION) {
            println("Found ${cursorInstructions.size} matching instructions:")
            for (insn in cursorInstructions) {
                println("- ${insn.textify()}")
            }
        }

        val isInsideMeType = PsiTreeUtil.getParentOfType(
            elementAtCursor,
            METype::class.java,
            false,
            MEExpression::class.java
        ) != null
        val isInsideNewExpr = PsiTreeUtil.getParentOfType(
            elementAtCursor,
            MENewExpression::class.java,
            false,
            MEExpression::class.java
        ) != null
        val cursorExprInTypePosition = !isInsideMeType &&
            elementAtCursor.parentOfType<MEExpression>()?.let(METypeUtil::isExpressionInTypePosition) == true
        val inTypePosition = isInsideMeType || isInsideNewExpr || cursorExprInTypePosition
        val isPossiblyIncompleteCast = !inTypePosition &&
            elementAtCursor.parentOfType<MEExpression>()
                ?.parents(false)
                ?.dropWhile { it is MEArrayAccessExpression && it.indexExpr == null } is MEParenthesizedExpression
        val canCompleteExprs = !inTypePosition
        val canCompleteTypes = inTypePosition || isPossiblyIncompleteCast

        return cursorInstructions.mapNotNull { insn ->
            getCompletionForInstruction(insn, flows, mixinClass, canCompleteExprs, canCompleteTypes)
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

            override fun visitNewExpression(o: MENewExpression) {
                val name = o.type
                if (name != null && !name.isWildcard && !pool.typeExists(name.text)) {
                    unknownNames += name
                }
                super.visitNewExpression(o)
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

    private fun findFlowTreeRoot(flow: FlowValue): FlowValue {
        val insn = flow.insnOrNull ?: return flow
        return if (insn.opcode == Opcodes.NEW) {
            flow.next.firstOrNull {
                val nextInsn = it.left.insnOrNull ?: return@firstOrNull false
                it.right == 0 &&
                    nextInsn.opcode == Opcodes.INVOKESPECIAL &&
                    (nextInsn as MethodInsnNode).name == "<init>"
            }?.left ?: flow
        } else {
            flow
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
        flows: FlowMap,
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
                                override fun computeTailType(context: InsertionContext?) = DOT_CLASS_TAIL
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
                    return object : TailTypeDecorator<LookupElement>(lookup) {
                        override fun computeTailType(context: InsertionContext?) =
                            ParenthesesTailType(!insn.desc.startsWith("()"))
                    }
                }
            }
            is TypeInsnNode -> {
                val type = Type.getObjectType(insn.desc)
                if (canCompleteTypes && type.isAccessibleFrom(mixinClass)) {
                    val lookup = createTypeLookup(type)
                    when (insn.opcode) {
                        Opcodes.ANEWARRAY -> {
                            return object : TailTypeDecorator<LookupElement>(lookup) {
                                override fun computeTailType(context: InsertionContext?) =
                                    BracketsTailType(
                                        1,
                                        flows[insn]?.hasDecoration(Decorations.ARRAY_CREATION_INFO) == true,
                                    )
                            }
                        }
                        Opcodes.NEW -> {
                            val initCall = flows[insn]?.next?.firstOrNull {
                                val nextInsn = it.left.insnOrNull ?: return@firstOrNull false
                                it.right == 0 &&
                                    nextInsn.opcode == Opcodes.INVOKESPECIAL &&
                                    (nextInsn as MethodInsnNode).name == "<init>"
                            }?.left?.insn as MethodInsnNode?
                            return object : TailTypeDecorator<LookupElement>(lookup) {
                                override fun computeTailType(context: InsertionContext?) =
                                    ParenthesesTailType(
                                        initCall?.desc?.startsWith("()") == false
                                    )
                            }
                        }
                        else -> return lookup
                    }
                }
            }
            is IntInsnNode -> {
                if (insn.opcode == Opcodes.NEWARRAY) {
                    if (canCompleteTypes) {
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
                                BracketsTailType(
                                    1,
                                    flows[insn]?.hasDecoration(Decorations.ARRAY_CREATION_INFO) == true,
                                )
                        }
                    }
                }
            }
            is MultiANewArrayInsnNode -> {
                if (canCompleteTypes) {
                    val type = Type.getType(insn.desc)
                    return object : TailTypeDecorator<LookupElement>(
                        createTypeLookup(type.elementType)
                    ) {
                        override fun computeTailType(context: InsertionContext?) =
                            BracketsTailType(
                                type.dimensions,
                                flows[insn]?.hasDecoration(Decorations.ARRAY_CREATION_INFO) == true,
                            )
                    }
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
        context.laterRunnable = Runnable {
            context.commitDocument()
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    addDefinition(context, id, at)
                }
            }
        }
    }

    private fun addDefinition(context: InsertionContext, id: String, at: String) {
        val contextElement = context.file.findElementAt(context.startOffset) ?: return
        val injectionHost = contextElement.findMultiInjectionHost() ?: return
        val expressionAnnotation = injectionHost.parentOfType<PsiAnnotation>() ?: return
        if (!expressionAnnotation.hasQualifiedName(MixinConstants.MixinExtras.EXPRESSION)) {
            return
        }
        val modifierList = expressionAnnotation.findContainingModifierList() ?: return

        // look for an existing definition with this id, skip if it exists
        for (annotation in modifierList.annotations) {
            if (annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION) &&
                annotation.findDeclaredAttributeValue("id")?.constantStringValue == id
            ) {
                return
            }
        }

        // create and add the new @Definition annotation
        var newAnnotation = JavaPsiFacade.getElementFactory(context.project).createAnnotationFromText(
            "@${MixinConstants.MixinExtras.DEFINITION}(id = \"$id\", $at)",
            modifierList,
        )
        newAnnotation = modifierList.addAfter(
            newAnnotation,
            modifierList.annotations.lastOrNull { it.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION) }
        ) as PsiAnnotation

        // add imports and reformat
        newAnnotation =
            JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(newAnnotation) as PsiAnnotation
        JavaCodeStyleManager.getInstance(context.project).optimizeImports(modifierList.containingFile)
        val annotationIndex = modifierList.annotations.indexOf(newAnnotation)
        val formattedModifierList =
            CodeStyleManager.getInstance(context.project).reformat(modifierList) as PsiModifierList
        newAnnotation = formattedModifierList.annotations.getOrNull(annotationIndex) ?: return

        // fold @At.target
        val foldingModel = context.editor.foldingModel
        val regionsToFold = mutableListOf<FoldRegion>()
        val annotationRange = newAnnotation.textRange
        for (foldRegion in foldingModel.allFoldRegions) {
            if (!annotationRange.contains(foldRegion.textRange)) {
                continue
            }
            val nameValuePair = newAnnotation.findElementAt(foldRegion.startOffset - annotationRange.startOffset)
                ?.findContainingNameValuePair() ?: continue
            if (nameValuePair.name == "target" &&
                nameValuePair.parentOfType<PsiAnnotation>()?.hasQualifiedName(MixinConstants.Annotations.AT) == true
            ) {
                regionsToFold += foldRegion
            }
        }

        foldingModel.runBatchFoldingOperation {
            for (foldRegion in regionsToFold) {
                foldRegion.isExpanded = false
            }
        }
    }

    private fun getStatementVariants(
        factory: MEExpressionElementFactory,
        statement: MEStatement
    ): List<MEMatchableElement> {
        return if (statement is MEExpressionStatement) {
            getExpressionVariants(factory, statement.expression)
        } else {
            listOf(statement)
        }
    }

    private fun getExpressionVariants(
        factory: MEExpressionElementFactory,
        expression: MEExpression
    ): List<MEMatchableElement> {
        val variants = mutableListOf<MEMatchableElement>(expression)

        val assignmentStatement = factory.createStatement("? = ?") as MEAssignStatement
        assignmentStatement.targetExpr.replace(expression.copy())
        variants += assignmentStatement

        when (expression) {
            is MEParenthesizedExpression -> {
                val castExpr = factory.createExpression("(?) ?") as MECastExpression
                castExpr.castTypeExpr!!.replace(expression.copy())
                variants += castExpr
            }
            is MENameExpression -> {
                val callExpr = factory.createExpression("?()") as MEStaticMethodCallExpression
                callExpr.memberName.replace(expression.meName)
                variants += callExpr

                val classExpr = factory.createExpression("${expression.text}.class") as MEClassConstantExpression
                variants += classExpr
            }
            is MEMemberAccessExpression -> {
                val callExpr = factory.createExpression("?.?()") as MEMethodCallExpression
                callExpr.receiverExpr.replace(expression.receiverExpr)
                callExpr.memberName.replace(expression.memberName)
                variants += callExpr
            }
            is MENewExpression -> {
                val type = expression.type
                if (type != null && !expression.hasConstructorArguments && !expression.isArrayCreation) {
                    val fixedNewExpr = factory.createExpression("new ?()") as MENewExpression
                    fixedNewExpr.type!!.replace(type)
                    variants += fixedNewExpr

                    val fixedNewArrayExpr = factory.createExpression("new ?[?]") as MENewExpression
                    fixedNewArrayExpr.type!!.replace(type)
                    variants += fixedNewArrayExpr
                }
            }
        }

        return variants
    }

    private class ParenthesesTailType(private val hasParameters: Boolean) : TailType() {
        override fun processTail(editor: Editor, tailOffset: Int): Int {
            editor.document.insertString(tailOffset, "()")
            return moveCaret(editor, tailOffset, if (hasParameters) 1 else 2)
        }

        override fun isApplicable(context: InsertionContext): Boolean {
            val chars = context.document.charsSequence
            val offset = CharArrayUtil.shiftForward(chars, context.tailOffset, " \n\t")
            return !CharArrayUtil.regionMatches(chars, offset, "(")
        }
    }

    private class BracketsTailType(private val dimensions: Int, private val hasInitializer: Boolean) : TailType() {
        override fun processTail(editor: Editor, tailOffset: Int): Int {
            editor.document.insertString(tailOffset, "[]".repeat(dimensions) + if (hasInitializer) "{}" else "")
            return moveCaret(editor, tailOffset, if (hasInitializer) 2 * dimensions + 1 else 1)
        }

        override fun isApplicable(context: InsertionContext): Boolean {
            val chars = context.document.charsSequence
            val offset = CharArrayUtil.shiftForward(chars, context.tailOffset, " \n\t")
            return !CharArrayUtil.regionMatches(chars, offset, "[")
        }
    }
}
