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

import com.demonwav.mcdev.MinecraftProjectSettings
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
import com.demonwav.mcdev.platform.mixin.util.AsmDfaUtil
import com.demonwav.mcdev.platform.mixin.util.MethodTargetMember
import com.demonwav.mcdev.platform.mixin.util.MixinConstants
import com.demonwav.mcdev.platform.mixin.util.SignatureToPsi
import com.demonwav.mcdev.platform.mixin.util.canonicalName
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isPrimitive
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.platform.mixin.util.textify
import com.demonwav.mcdev.platform.mixin.util.toPsiType
import com.demonwav.mcdev.util.BeforeOrAfter
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findContainingModifierList
import com.demonwav.mcdev.util.findContainingNameValuePair
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.findMultiInjectionHost
import com.demonwav.mcdev.util.invokeLater
import com.demonwav.mcdev.util.mapFirstNotNull
import com.demonwav.mcdev.util.packageName
import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.TailTypeDecorator
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.TextResult
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.FoldRegion
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.StandardPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifierList
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.JavaCodeStyleManager
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageEditorUtil
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.createSmartPointer
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.parents
import com.intellij.util.PlatformIcons
import com.intellij.util.text.CharArrayUtil
import com.llamalad7.mixinextras.expression.impl.flow.ArrayCreationInfo
import com.llamalad7.mixinextras.expression.impl.flow.ComplexFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool
import com.llamalad7.mixinextras.utils.Decorations
import org.apache.commons.lang3.mutable.MutableInt
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
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
            println(targetMethod.textify())
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
        var cursorOffsetInCopyFile =
            cursorOffset.toInt() - statement.textRange.startOffset + wildcardReplacedStatement.textRange.startOffset
        replaceCursorInputWithWildcard(project, wildcardReplacedStatement, cursorOffsetInCopyFile)

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

        var roundNumber = 0
        var subExpr: MEMatchableElement = statement
        while (true) {
            val inputExprOnCursor = subExpr.getInputExprs().firstOrNull { it.textRange.contains(cursorOffset.toInt()) }
                ?: break
            val wildcardReplacedExpr = inputExprOnCursor.copy() as MEExpression
            cursorOffsetInCopyFile = cursorOffset.toInt() -
                inputExprOnCursor.textRange.startOffset + wildcardReplacedExpr.textRange.startOffset

            if (DEBUG_COMPLETION) {
                val exprText = wildcardReplacedExpr.text
                val cursorOffsetInExpr = cursorOffsetInCopyFile - wildcardReplacedExpr.textRange.startOffset
                val exprWithCaretMarker = when {
                    cursorOffsetInExpr < 0 -> "<caret=$cursorOffset>$exprText"
                    cursorOffsetInExpr > exprText.length -> "$exprText<caret=$cursorOffset>"
                    else -> exprText.replaceRange(cursorOffsetInExpr, cursorOffsetInExpr, "<caret>")
                }
                println("=== Round ${++roundNumber}: handling $exprWithCaretMarker")
            }

            replaceCursorInputWithWildcard(project, wildcardReplacedExpr, cursorOffsetInCopyFile)

            val newMatchingFlows = mutableSetOf<FlowValue>()
            for (exprToMatch in getExpressionVariants(project.meExpressionElementFactory, wildcardReplacedExpr)) {
                if (DEBUG_COMPLETION) {
                    println("Matching against expression ${exprToMatch.text}")
                }

                val meExpression = MEExpressionMatchUtil.createExpression(exprToMatch.text) ?: continue

                val flattenedInstructions = mutableSetOf<AbstractInsnNode>()
                for (flow in matchingFlows) {
                    getInstructionsInFlowTree(
                        flow,
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
            getInstructionsInFlowTree(flow, cursorInstructions, false)
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
                ?.dropWhile { it is MEArrayAccessExpression && it.indexExpr == null }
                ?.firstOrNull() is MEParenthesizedExpression
        val canCompleteExprs = !inTypePosition
        val canCompleteTypes = inTypePosition || isPossiblyIncompleteCast

        if (DEBUG_COMPLETION) {
            println("canCompleteExprs = $canCompleteExprs")
            println("canCompleteTypes = $canCompleteTypes")
        }

        val eliminableResults = cursorInstructions.flatMap { insn ->
            getCompletionsForInstruction(
                project,
                targetClass,
                targetMethod,
                insn,
                flows,
                mixinClass,
                canCompleteExprs,
                canCompleteTypes
            )
        }

        // In the case of multiple instructions producing the same lookup, attempt to show only the "best" lookup.
        // For example, if a local variable is only sometimes able to be targeted using implicit ordinals in this
        // expression, prefer specifying the ordinal.
        return eliminableResults.groupBy { it.lookupElement.lookupString }.values.map { it.max().lookupElement }
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

    private fun getFlowInputs(flow: FlowValue): List<FlowValue> {
        val arrayCreationInfo = flow.getDecoration<ArrayCreationInfo>(Decorations.ARRAY_CREATION_INFO)
        if (arrayCreationInfo != null) {
            return arrayCreationInfo.values
        }

        var rootFlow = flow
        val insn = flow.insnOrNull ?: return emptyList()
        if (insn.opcode == Opcodes.NEW) {
            rootFlow = flow.next.firstOrNull {
                val nextInsn = it.left.insnOrNull ?: return@firstOrNull false
                it.right == 0 &&
                    nextInsn.opcode == Opcodes.INVOKESPECIAL &&
                        (nextInsn as MethodInsnNode).name == "<init>"
            }?.left ?: rootFlow
        }

        return (0 until rootFlow.inputCount()).map(rootFlow::getInput)
    }

    private fun getInstructionsInFlowTree(
        flow: FlowValue,
        outInstructions: MutableSet<AbstractInsnNode>,
        strict: Boolean,
    ) {
        if (flow is DummyFlowValue || flow is ComplexFlowValue) {
            return
        }

        if (!strict) {
            if (!outInstructions.add(flow.insn)) {
                return
            }
        }
        for (input in getFlowInputs(flow)) {
            getInstructionsInFlowTree(input, outInstructions, false)
        }
    }

    private fun getCompletionsForInstruction(
        project: Project,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode,
        flows: FlowMap,
        mixinClass: PsiClass,
        canCompleteExprs: Boolean,
        canCompleteTypes: Boolean
    ): List<EliminableLookup> {
        when (insn) {
            is LdcInsnNode -> {
                when (val cst = insn.cst) {
                    is Type -> {
                        if (canCompleteTypes && cst.isAccessibleFrom(mixinClass)) {
                            return listOf(
                                object : TailTypeDecorator<LookupElement>(createTypeLookup(cst)) {
                                    override fun computeTailType(context: InsertionContext?) = DOT_CLASS_TAIL
                                }.createEliminable()
                            )
                        }
                    }
                    // TODO: string literals?
                }
            }
            is VarInsnNode -> return createLocalVariableLookups(
                project,
                targetClass,
                targetMethod,
                insn,
                insn.`var`,
                insn.opcode in Opcodes.ISTORE..Opcodes.ASTORE,
                mixinClass
            )
            is IincInsnNode -> return createLocalVariableLookups(
                project,
                targetClass,
                targetMethod,
                insn,
                insn.`var`,
                false,
                mixinClass
            )
            is FieldInsnNode -> {
                if (canCompleteExprs) {
                    val at = "at = @${MixinConstants.Annotations.AT}(value = \"FIELD\"," +
                        " target = \"L${insn.owner};${insn.name}:${insn.desc}\")"
                    var lookup = LookupElementBuilder.create(insn.name.toValidIdentifier())
                        .withIcon(PlatformIcons.FIELD_ICON)
                        .withPresentableText(insn.owner.substringAfterLast('/') + "." + insn.name)
                        .withTypeText(Type.getType(insn.desc).presentableName())
                        .withDefinitionAndFoldTarget(insn.name.toValidIdentifier(), at)
                    if (insn.opcode == Opcodes.GETSTATIC || insn.opcode == Opcodes.PUTSTATIC) {
                        lookup = lookup.withLookupString(insn.owner.substringAfterLast('/') + "." + insn.name)
                    }
                    return listOf(lookup.createEliminable())
                }
            }
            is MethodInsnNode -> {
                if (canCompleteExprs) {
                    val at = "at = @${MixinConstants.Annotations.AT}(value = \"INVOKE\"," +
                        " target = \"L${insn.owner};${insn.name}${insn.desc}\")"
                    var lookup = LookupElementBuilder.create(insn.name.toValidIdentifier())
                        .withIcon(PlatformIcons.METHOD_ICON)
                        .withPresentableText(insn.owner.substringAfterLast('/') + "." + insn.name)
                        .withTailText(
                            "(" + Type.getArgumentTypes(insn.desc).joinToString { it.presentableName() } + ")"
                        )
                        .withTypeText(Type.getReturnType(insn.desc).presentableName())
                        .withDefinitionAndFoldTarget(insn.name.toValidIdentifier(), at)
                    if (insn.opcode == Opcodes.INVOKESTATIC) {
                        lookup = lookup.withLookupString(insn.owner.substringAfterLast('/') + "." + insn.name)
                    }
                    return listOf(
                        object : TailTypeDecorator<LookupElement>(lookup) {
                            override fun computeTailType(context: InsertionContext?) =
                                ParenthesesTailType(!insn.desc.startsWith("()"))
                        }.createEliminable()
                    )
                }
            }
            is TypeInsnNode -> {
                val type = Type.getObjectType(insn.desc)
                if (canCompleteTypes && type.isAccessibleFrom(mixinClass)) {
                    val lookup = createTypeLookup(type)
                    when (insn.opcode) {
                        Opcodes.ANEWARRAY -> {
                            return listOf(
                                object : TailTypeDecorator<LookupElement>(lookup) {
                                    override fun computeTailType(context: InsertionContext?) =
                                        BracketsTailType(
                                            1,
                                            flows[insn]?.hasDecoration(Decorations.ARRAY_CREATION_INFO) == true,
                                        )
                                }.createEliminable()
                            )
                        }
                        Opcodes.NEW -> {
                            val initCall = flows[insn]?.next?.firstOrNull {
                                val nextInsn = it.left.insnOrNull ?: return@firstOrNull false
                                it.right == 0 &&
                                    nextInsn.opcode == Opcodes.INVOKESPECIAL &&
                                    (nextInsn as MethodInsnNode).name == "<init>"
                            }?.left?.insn as MethodInsnNode?
                            return listOf(
                                object : TailTypeDecorator<LookupElement>(lookup) {
                                    override fun computeTailType(context: InsertionContext?) =
                                        ParenthesesTailType(
                                            initCall?.desc?.startsWith("()") == false
                                        )
                                }.createEliminable()
                            )
                        }
                        else -> return listOf(lookup.createEliminable())
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
                        return listOf(
                            object : TailTypeDecorator<LookupElement>(
                                LookupElementBuilder.create(type).withIcon(PlatformIcons.CLASS_ICON)
                            ) {
                                override fun computeTailType(context: InsertionContext?) =
                                    BracketsTailType(
                                        1,
                                        flows[insn]?.hasDecoration(Decorations.ARRAY_CREATION_INFO) == true,
                                    )
                            }.createEliminable()
                        )
                    }
                }
            }
            is MultiANewArrayInsnNode -> {
                if (canCompleteTypes) {
                    val type = Type.getType(insn.desc)
                    return listOf(
                        object : TailTypeDecorator<LookupElement>(
                            createTypeLookup(type.elementType)
                        ) {
                            override fun computeTailType(context: InsertionContext?) =
                                BracketsTailType(
                                    type.dimensions,
                                    flows[insn]?.hasDecoration(Decorations.ARRAY_CREATION_INFO) == true,
                                )
                        }.createEliminable()
                    )
                }
            }
            is InsnNode -> {
                when (insn.opcode) {
                    Opcodes.ARRAYLENGTH -> {
                        if (canCompleteExprs) {
                            return listOf(
                                LookupElementBuilder.create("length")
                                    .withIcon(PlatformIcons.FIELD_ICON)
                                    .withTypeText("int")
                                    .createEliminable()
                            )
                        }
                    }
                }
            }
        }

        return emptyList()
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

    private fun createLocalVariableLookups(
        project: Project,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: AbstractInsnNode,
        index: Int,
        isStore: Boolean,
        mixinClass: PsiClass,
    ): List<EliminableLookup> {
        // ignore "this"
        if (!targetMethod.hasAccess(Opcodes.ACC_STATIC) && index == 0) {
            return emptyList()
        }

        var argumentsSize = Type.getArgumentsAndReturnSizes(targetMethod.desc) shr 2
        if (targetMethod.hasAccess(Opcodes.ACC_STATIC)) {
            argumentsSize--
        }
        val isArgsOnly = index < argumentsSize

        if (targetMethod.localVariables != null) {
            val localsHere = targetMethod.localVariables.filter { localVariable ->
                val firstValidInstruction = if (isStore) {
                    generateSequence<AbstractInsnNode>(localVariable.start) { it.previous }
                        .firstOrNull { it.opcode >= 0 }
                } else {
                    localVariable.start.next
                }
                if (firstValidInstruction == null) {
                    return@filter false
                }
                val validRange = targetMethod.instructions.indexOf(firstValidInstruction) until
                    targetMethod.instructions.indexOf(localVariable.end)
                targetMethod.instructions.indexOf(insn) in validRange
            }
            val locals = localsHere.filter { it.index == index }

            val elementFactory = JavaPsiFacade.getElementFactory(project)

            return locals.map { localVariable ->
                val localPsiType = if (localVariable.signature != null) {
                    val sigToPsi = SignatureToPsi(elementFactory, mixinClass)
                    SignatureReader(localVariable.signature).acceptType(sigToPsi)
                    sigToPsi.type
                } else {
                    Type.getType(localVariable.desc).toPsiType(elementFactory, mixinClass)
                }
                val localsOfMyType = localsHere.filter { it.desc == localVariable.desc }
                val ordinal = localsOfMyType.indexOf(localVariable)
                val isImplicit = localsOfMyType.size == 1
                LookupElementBuilder.create(localVariable.name.toValidIdentifier())
                    .withIcon(PlatformIcons.VARIABLE_ICON)
                    .withTypeText(localPsiType.presentableText)
                    .withLocalDefinition(
                        localVariable.name.toValidIdentifier(),
                        Type.getType(localVariable.desc),
                        ordinal,
                        isArgsOnly,
                        isImplicit,
                        mixinClass,
                    )
                    .createEliminable(if (isImplicit) -1 else 0)
            }
        }

        // fallback to ASM dataflow
        val localTypes = AsmDfaUtil.getLocalVariableTypes(project, targetClass, targetMethod, insn)
            ?: return emptyList()
        val localType = localTypes.getOrNull(index) ?: return emptyList()
        val ordinal = localTypes.asSequence().take(index).filter { it == localType }.count()
        val localName = localType.typeNameToInsert().replace("[]", "Array") + (ordinal + 1)
        val isImplicit = localTypes.count { it == localType } == 1
        return listOf(
            LookupElementBuilder.create(localName)
                .withIcon(PlatformIcons.VARIABLE_ICON)
                .withTypeText(localType.presentableName())
                .withLocalDefinition(localName, localType, ordinal, isArgsOnly, isImplicit, mixinClass)
                .createEliminable(if (isImplicit) -1 else 0)
        )
    }

    private fun LookupElementBuilder.withDefinition(id: String, at: String) = withDefinition(id, at) { _, _ -> }

    private fun LookupElementBuilder.withDefinitionAndFoldTarget(id: String, at: String) =
        withDefinition(id, at) { context, annotation ->
            val hostEditor = InjectedLanguageEditorUtil.getTopLevelEditor(context.editor)
            CodeFoldingManager.getInstance(context.project).updateFoldRegions(hostEditor)
            val foldingModel = hostEditor.foldingModel
            val regionsToFold = mutableListOf<FoldRegion>()
            val annotationRange = annotation.textRange
            for (foldRegion in foldingModel.allFoldRegions) {
                if (!annotationRange.contains(foldRegion.textRange)) {
                    continue
                }
                val nameValuePair = annotation.findElementAt(foldRegion.startOffset - annotationRange.startOffset)
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

    private fun LookupElementBuilder.withLocalDefinition(
        name: String,
        type: Type,
        ordinal: Int,
        isArgsOnly: Boolean,
        canBeImplicit: Boolean,
        mixinClass: PsiClass,
    ): LookupElementBuilder {
        val isTypeAccessible = type.isAccessibleFrom(mixinClass)
        val isImplicit = canBeImplicit && isTypeAccessible

        val definitionLocal = buildString {
            append("local = @${MixinConstants.MixinExtras.LOCAL}(")
            if (isTypeAccessible) {
                append("type = ${type.className}.class, ")
            }
            if (!isImplicit) {
                append("ordinal = ")
                append(ordinal)
                append(", ")
            }
            if (isArgsOnly) {
                append("argsOnly = true, ")
            }

            if (endsWith(", ")) {
                setLength(length - 2)
            }

            append(")")
        }
        return withDefinition(name, definitionLocal) { context, annotation ->
            if (isImplicit) {
                return@withDefinition
            }

            invokeLater {
                WriteCommandAction.runWriteCommandAction(
                    context.project,
                    "Choose How to Target Local Variable",
                    null,
                    { runLocalTemplate(context.project, context.editor, context.file, annotation, ordinal, name) },
                    annotation.containingFile,
                )
            }
        }
    }

    private fun runLocalTemplate(
        project: Project,
        editor: Editor,
        file: PsiFile,
        annotation: PsiAnnotation,
        ordinal: Int,
        name: String
    ) {
        val elementToReplace =
            (annotation.findDeclaredAttributeValue("local") as? PsiAnnotation)
                ?.findDeclaredAttributeValue("ordinal")
                ?.findContainingNameValuePair() ?: return

        val hostEditor = InjectedLanguageEditorUtil.getTopLevelEditor(editor)
        val hostElement = file.findElementAt(editor.caretModel.offset)?.findMultiInjectionHost() ?: return

        val template = TemplateBuilderImpl(annotation)
        val lookupItems = arrayOf(
            LookupElementBuilder.create("ordinal = $ordinal"),
            LookupElementBuilder.create("name = \"$name\"")
        )
        template.replaceElement(
            elementToReplace,
            object : Expression() {
                override fun calculateLookupItems(context: ExpressionContext?) = lookupItems
                override fun calculateQuickResult(context: ExpressionContext?) = calculateResult(context)
                override fun calculateResult(context: ExpressionContext?) = TextResult("ordinal = $ordinal")
            },
            true,
        )

        val prevCursorPosInLiteral = hostEditor.caretModel.offset - hostElement.textRange.startOffset
        val hostElementPtr = hostElement.createSmartPointer(project)
        hostEditor.caretModel.moveToOffset(annotation.textRange.startOffset)
        TemplateManager.getInstance(project).startTemplate(
            hostEditor,
            template.buildInlineTemplate(),
            object : TemplateEditingAdapter() {
                override fun templateFinished(template: Template, brokenOff: Boolean) {
                    PsiDocumentManager.getInstance(project).commitDocument(hostEditor.document)
                    val newHostElement = hostElementPtr.element ?: return
                    hostEditor.caretModel.moveToOffset(newHostElement.textRange.startOffset + prevCursorPosInLiteral)
                }
            }
        )
    }

    private inline fun LookupElementBuilder.withDefinition(
        id: String,
        at: String,
        crossinline andThen: (InsertionContext, PsiAnnotation) -> Unit
    ) = withInsertHandler { context, _ ->
        context.laterRunnable = Runnable {
            context.commitDocument()
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    val annotation = addDefinition(context, id, at)
                    if (annotation != null) {
                        andThen(context, annotation)
                    }
                }
            }
        }
    }

    private fun addDefinition(context: InsertionContext, id: String, at: String): PsiAnnotation? {
        val contextElement = context.file.findElementAt(context.startOffset) ?: return null
        val injectionHost = contextElement.findMultiInjectionHost() ?: return null
        val expressionAnnotation = injectionHost.parentOfType<PsiAnnotation>() ?: return null
        if (!expressionAnnotation.hasQualifiedName(MixinConstants.MixinExtras.EXPRESSION)) {
            return null
        }
        val modifierList = expressionAnnotation.findContainingModifierList() ?: return null

        // look for an existing definition with this id, skip if it exists
        for (annotation in modifierList.annotations) {
            if (annotation.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION) &&
                annotation.findDeclaredAttributeValue("id")?.constantStringValue == id
            ) {
                return null
            }
        }

        // create and add the new @Definition annotation
        var newAnnotation = JavaPsiFacade.getElementFactory(context.project).createAnnotationFromText(
            "@${MixinConstants.MixinExtras.DEFINITION}(id = \"$id\", $at)",
            modifierList,
        )
        var anchor = modifierList.annotations.lastOrNull { it.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION) }
        if (anchor == null) {
            val definitionPosRelativeToExpression =
                MinecraftProjectSettings.getInstance(context.project).definitionPosRelativeToExpression
            if (definitionPosRelativeToExpression == BeforeOrAfter.AFTER) {
                anchor = expressionAnnotation
            }
        }
        newAnnotation = modifierList.addAfter(newAnnotation, anchor) as PsiAnnotation

        // add imports and reformat
        newAnnotation =
            JavaCodeStyleManager.getInstance(context.project).shortenClassReferences(newAnnotation) as PsiAnnotation
        JavaCodeStyleManager.getInstance(context.project).optimizeImports(modifierList.containingFile)
        val annotationIndex = modifierList.annotations.indexOf(newAnnotation)
        val formattedModifierList =
            CodeStyleManager.getInstance(context.project).reformat(modifierList) as PsiModifierList
        return formattedModifierList.annotations.getOrNull(annotationIndex)
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

    private fun LookupElement.createEliminable(priority: Int = 0) = EliminableLookup(this, priority)

    private class EliminableLookup(
        val lookupElement: LookupElement,
        private val priority: Int
    ) : Comparable<EliminableLookup> {
        override fun compareTo(other: EliminableLookup) = priority.compareTo(other.priority)
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
