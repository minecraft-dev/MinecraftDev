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

package com.demonwav.mcdev.platform.mixin.expression

import com.demonwav.mcdev.MinecraftProjectSettings
import com.demonwav.mcdev.platform.mixin.expression.MEExpressionMatchUtil.virtualInsn
import com.demonwav.mcdev.platform.mixin.expression.MEExpressionMatchUtil.virtualInsnOrNull
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEArrayAccessExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEAssignStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEBoundMethodReferenceExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MECapturingExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MECastExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEClassConstantExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpression
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionStatement
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEExpressionTypes
import com.demonwav.mcdev.platform.mixin.expression.gen.psi.MEFreeMethodReferenceExpression
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
import com.llamalad7.mixinextras.expression.impl.flow.ComplexFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.DummyFlowValue
import com.llamalad7.mixinextras.expression.impl.flow.FlowValue
import com.llamalad7.mixinextras.expression.impl.flow.expansion.InsnExpander
import com.llamalad7.mixinextras.expression.impl.flow.postprocessing.InstantiationInfo
import com.llamalad7.mixinextras.expression.impl.point.ExpressionContext
import com.llamalad7.mixinextras.expression.impl.pool.IdentifierPool
import com.llamalad7.mixinextras.expression.impl.utils.FlowDecorations
import org.apache.commons.lang3.mutable.MutableInt
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode
import org.objectweb.asm.tree.VarInsnNode

private typealias TemplateExpressionContext = com.intellij.codeInsight.template.ExpressionContext

object MEExpressionCompletionUtil {
    private const val DEBUG_COMPLETION = false

    private val NORMAL_ELEMENT = PlatformPatterns.psiElement()
        .inside(MEStatement::class.java)
        .andNot(PlatformPatterns.psiElement().inside(MELitExpression::class.java))
        .notInTypePosition()
    private val TYPE_PATTERN = PlatformPatterns.psiElement()
        .inside(MEStatement::class.java)
        .validType()
    private val AFTER_END_EXPRESSION_PATTERN = StandardPatterns.or(
        PlatformPatterns.psiElement().afterLeaf(
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
        ),
        PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement().withText("new").afterLeaf("::")),
    )

    val STATEMENT_KEYWORD_PLACE = PlatformPatterns.psiElement().afterLeaf(
        PlatformPatterns.psiElement().withText("{").withParent(MEStatementItem::class.java)
    )
    val VALUE_KEYWORD_PLACE = StandardPatterns.and(
        NORMAL_ELEMENT,
        StandardPatterns.not(AFTER_END_EXPRESSION_PATTERN),
        StandardPatterns.not(PlatformPatterns.psiElement().afterLeaf(".")),
        StandardPatterns.not(PlatformPatterns.psiElement().afterLeaf("::")),
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
    val METHOD_REFERENCE_PLACE = StandardPatterns.and(
        NORMAL_ELEMENT,
        PlatformPatterns.psiElement().afterLeaf("::"),
    )
    val STRING_LITERAL_PLACE = PlatformPatterns.psiElement().withElementType(
        TokenSet.create(MEExpressionTypes.TOKEN_STRING, MEExpressionTypes.TOKEN_STRING_TERMINATOR)
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

    private val COLON_COLON_NEW_TAIL = object : TailType() {
        override fun processTail(editor: Editor, tailOffset: Int): Int {
            editor.document.insertString(tailOffset, "::new")
            return moveCaret(editor, tailOffset, 5)
        }

        override fun isApplicable(context: InsertionContext): Boolean {
            val chars = context.document.charsSequence
            val colonColonOffset = CharArrayUtil.shiftForward(chars, context.tailOffset, " \n\t")
            if (!CharArrayUtil.regionMatches(chars, colonColonOffset, "::")) {
                return true
            }
            val newOffset = CharArrayUtil.shiftForward(chars, colonColonOffset + 2, " \n\t")
            return !CharArrayUtil.regionMatches(chars, newOffset, "new")
        }
    }

    fun getStringCompletions(project: Project, contextElement: PsiElement): List<LookupElement> {
        val expressionAnnotation = contextElement.findMultiInjectionHost()?.parentOfType<PsiAnnotation>()
            ?: return emptyList()
        if (!expressionAnnotation.hasQualifiedName(MixinConstants.MixinExtras.EXPRESSION)) {
            return emptyList()
        }

        val modifierList = expressionAnnotation.findContainingModifierList() ?: return emptyList()

        val handlerAnnotation = modifierList.annotations.firstOrNull {
            MixinAnnotationHandler.forMixinAnnotation(it, project) != null
        } ?: return emptyList()

        return MixinAnnotationHandler.resolveTarget(handlerAnnotation).flatMap { member ->
            (member as? MethodTargetMember)?.classAndMethod
                ?.let { (clazz, method) -> MEExpressionMatchUtil.getFlowMap(project, clazz, method) }
                ?.values
                ?.asSequence()
                ?.filterNot { it.isComplex }
                ?.map { it.insn }
                ?.mapNotNull { insn ->
                    if (insn is LdcInsnNode && insn.cst is String) {
                        LookupElementBuilder.create(insn.cst)
                    } else {
                        null
                    }
                }
                .orEmpty()
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
            val handler = MixinAnnotationHandler.forMixinAnnotation(annotation, project) ?: return@mapFirstNotNull null
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
        /*
         * MixinExtras isn't designed to match against incomplete expressions, which is what we need to do to produce
         * completion options. The only support there is, is to match incomplete parameter lists and so on
         * ("list inputs" to expressions). What follows is a kind of DIY match where we figure out different options
         * for what the user might be trying to complete and hand it to MixinExtras to do the actual matching. Note that
         * IntelliJ already inserts an identifier at the caret position to make auto-completion easier.
         *
         * We have four classes of problems to solve here:
         * 1. There may already be a capture in the expression causing MixinExtras to return the wrong instructions.
         * 2. There may be unresolved identifiers in the expression, causing MixinExtras to match nothing, which isn't
         *    ideal.
         * 3. "this.<caret>" expands to a field access, but the user may be trying to complete a method call (and other
         *    similar situations).
         * 4. What the user is typing may form only a subexpression of a larger expression. For example, with
         *    "foo(<caret>)", the user may actually be trying to type the expression "foo(x + y) + z". That is, "x",
         *    which is where the caret is, may not be a direct subexpression to the "foo" call expression, which itself
         *    may not be a direct subexpression of its parent.
         *
         * Throughout this process, we have to keep careful track of where the caret is, because:
         * 1. As we make changes to the expression to the left of the caret, the caret may shift.
         * 2. As we make copies of the element, or entirely new elements, that new element's textOffset may be different
         *    from the original one.
         */

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

        // Removing all explicit captures from the expression solves problem 1 (see comment above).
        removeExplicitCaptures(statement, cursorOffset)
        // Replacing unresolved names with wildcards solves problem 2 (see comment above).
        replaceUnresolvedNamesWithWildcards(project, statement, cursorOffset, pool)

        val elementAtCursor = statement.findElementAt(cursorOffset.toInt()) ?: return emptyList()

        /*
         * To solve problem 4 (see comment above), we first find matches for the top level statement, ignoring the
         * subexpression that the caret is on. Then we iterate down into the subexpression that contains the caret and
         * match that against all the statement's input flows in the same way as we matched the statement against all
         * the instructions in the target method. Then we keep iterating until we reach the identifier the caret is on.
         */

        // Replace the subexpression the caret is on with a wildcard expression, so MixinExtras ignores it.
        val wildcardReplacedStatement = statement.copy() as MEStatement
        var cursorOffsetInCopyFile =
            cursorOffset.toInt() - statement.textRange.startOffset + wildcardReplacedStatement.textRange.startOffset
        replaceCursorInputWithWildcard(project, wildcardReplacedStatement, cursorOffsetInCopyFile)

        // Iterate through possible "variants" of the statement that the user may be trying to complete; it doesn't
        // matter if they don't parse, then we just skip them. This solves problem 3 (see comment above).
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
                flows.keys,
                ExpressionContext.Type.MODIFY_EXPRESSION_VALUE, // use most permissive type for completion
                true,
            ) { match ->
                matchingFlows += match.flow
                if (DEBUG_COMPLETION) {
                    println("Matched ${match.flow.virtualInsnOrNull?.insn?.textify()}")
                }
            }
        }
        if (matchingFlows.isEmpty()) {
            return emptyList()
        }

        // Iterate through subexpressions until we reach the identifier the caret is on
        var roundNumber = 0
        var subExpr: MEMatchableElement = statement
        while (true) {
            // Replace the subexpression the caret is on with a wildcard expression, so MixinExtras ignores it.
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

            // Iterate through the possible "varaints" of the expression in the same way as we did for the statement
            // above. This solves problem 3 (see comment above).
            val newMatchingFlows = mutableSetOf<FlowValue>()
            for (exprToMatch in getExpressionVariants(project.meExpressionElementFactory, wildcardReplacedExpr)) {
                if (DEBUG_COMPLETION) {
                    println("Matching against expression ${exprToMatch.text}")
                }

                val meExpression = MEExpressionMatchUtil.createExpression(exprToMatch.text) ?: continue

                val flattenedInstructions = mutableSetOf<ExpandedInstruction>()
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
                    flattenedInstructions.map { it.insn },
                    ExpressionContext.Type.MODIFY_EXPRESSION_VALUE, // use most permissive type for completion
                    true,
                ) { match ->
                    newMatchingFlows += match.flow
                    if (DEBUG_COMPLETION) {
                        println("Matched ${match.flow.virtualInsnOrNull?.insn?.textify()}")
                    }
                }
            }

            if (newMatchingFlows.isEmpty()) {
                return emptyList()
            }
            matchingFlows = newMatchingFlows.toMutableList()

            subExpr = inputExprOnCursor
        }

        val cursorInstructions = mutableSetOf<ExpandedInstruction>()
        for (flow in matchingFlows) {
            getInstructionsInFlowTree(flow, cursorInstructions, false)
        }

        if (DEBUG_COMPLETION) {
            println("Found ${cursorInstructions.size} matching instructions:")
            for (insn in cursorInstructions) {
                println("- ${insn.insn.insn.textify()}")
            }
        }

        // Try to decide if we should be completing types or normal expressions.
        // Not as easy as it sounds (think incomplete casts looking like parenthesized expressions).
        // Note that it's possible to complete types and expressions at the same time.
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
                insn.insn,
                insn.originalInsn,
                flows,
                mixinClass,
                canCompleteExprs,
                canCompleteTypes
            )
        }

        // In the case of multiple instructions producing the same lookup, attempt to show only the "best" lookup.
        // For example, if a local variable is only sometimes able to be targeted using implicit ordinals in this
        // expression, prefer specifying the ordinal.
        return eliminableResults.groupBy { it.uniquenessKey }.values.map { it.max().lookupElement }
    }

    private fun replaceUnresolvedNamesWithWildcards(
        project: Project,
        statement: MEStatement,
        cursorOffset: MutableInt,
        pool: IdentifierPool,
    ) {
        val unresolvedNames = mutableListOf<MEName>()
        statement.accept(object : MERecursiveWalkingVisitor() {
            override fun visitType(o: METype) {
                val name = o.meName
                if (!name.isWildcard && !pool.typeExists(name.text)) {
                    unresolvedNames += name
                }
            }

            override fun visitNameExpression(o: MENameExpression) {
                val name = o.meName
                if (!name.isWildcard) {
                    if (METypeUtil.isExpressionDirectlyInTypePosition(o)) {
                        if (!pool.typeExists(name.text)) {
                            unresolvedNames += name
                        }
                    } else {
                        if (!pool.memberExists(name.text)) {
                            unresolvedNames += name
                        }
                    }
                }
            }

            override fun visitSuperCallExpression(o: MESuperCallExpression) {
                val name = o.memberName
                if (name != null && !name.isWildcard && !pool.memberExists(name.text)) {
                    unresolvedNames += name
                }
                super.visitSuperCallExpression(o)
            }

            override fun visitMethodCallExpression(o: MEMethodCallExpression) {
                val name = o.memberName
                if (!name.isWildcard && !pool.memberExists(name.text)) {
                    unresolvedNames += name
                }
                super.visitMethodCallExpression(o)
            }

            override fun visitStaticMethodCallExpression(o: MEStaticMethodCallExpression) {
                val name = o.memberName
                if (!name.isWildcard && !pool.memberExists(name.text)) {
                    unresolvedNames += name
                }
                super.visitStaticMethodCallExpression(o)
            }

            override fun visitBoundMethodReferenceExpression(o: MEBoundMethodReferenceExpression) {
                val name = o.memberName
                if (name != null && !name.isWildcard && !pool.memberExists(name.text)) {
                    unresolvedNames += name
                }
                super.visitBoundMethodReferenceExpression(o)
            }

            override fun visitFreeMethodReferenceExpression(o: MEFreeMethodReferenceExpression) {
                val name = o.memberName
                if (name != null && !name.isWildcard && !pool.memberExists(name.text)) {
                    unresolvedNames += name
                }
                super.visitFreeMethodReferenceExpression(o)
            }

            override fun visitMemberAccessExpression(o: MEMemberAccessExpression) {
                val name = o.memberName
                if (!name.isWildcard && !pool.memberExists(name.text)) {
                    unresolvedNames += name
                }
                super.visitMemberAccessExpression(o)
            }

            override fun visitNewExpression(o: MENewExpression) {
                val name = o.type
                if (name != null && !name.isWildcard && !pool.typeExists(name.text)) {
                    unresolvedNames += name
                }
                super.visitNewExpression(o)
            }
        })

        for (unresolvedName in unresolvedNames) {
            val startOffset = unresolvedName.textRange.startOffset
            if (cursorOffset.toInt() > startOffset) {
                cursorOffset.setValue(cursorOffset.toInt() - unresolvedName.textLength + 1)
            }

            unresolvedName.replace(project.meExpressionElementFactory.createName("?"))
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
        outInstructions: MutableSet<ExpandedInstruction>,
        strict: Boolean,
    ) {
        if (flow is DummyFlowValue || flow is ComplexFlowValue) {
            return
        }

        if (!strict) {
            val originalInsn = InsnExpander.getRepresentative(flow) ?: flow.insn
            if (!outInstructions.add(ExpandedInstruction(flow.virtualInsn, originalInsn))) {
                return
            }
        }
        for (i in 0 until flow.inputCount()) {
            getInstructionsInFlowTree(flow.getInput(i), outInstructions, false)
        }
    }

    private fun getCompletionsForInstruction(
        project: Project,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        insn: VirtualInsn,
        originalInsn: AbstractInsnNode,
        flows: FlowMap,
        mixinClass: PsiClass,
        canCompleteExprs: Boolean,
        canCompleteTypes: Boolean
    ): List<EliminableLookup> {
        val flow = flows[insn]
        when (insn.insn) {
            is LdcInsnNode -> {
                when (val cst = insn.insn.cst) {
                    is Type -> {
                        if (canCompleteExprs && cst.isAccessibleFrom(mixinClass)) {
                            return listOf(
                                createTypeLookup(cst)
                                    .withTailText(".class")
                                    .withTail(DOT_CLASS_TAIL)
                                    .createEliminable("class ${insn.insn.cst}")
                            )
                        }
                    }
                }
            }
            is VarInsnNode -> return createLocalVariableLookups(
                project,
                targetClass,
                targetMethod,
                originalInsn,
                insn.insn.`var`,
                insn.insn.opcode in Opcodes.ISTORE..Opcodes.ASTORE,
                mixinClass
            )
            is IincInsnNode -> return createLocalVariableLookups(
                project,
                targetClass,
                targetMethod,
                originalInsn,
                insn.insn.`var`,
                false,
                mixinClass
            )
            is FieldInsnNode -> {
                if (canCompleteExprs) {
                    val definitionValue = "field = \"L${insn.insn.owner};${insn.insn.name}:${insn.insn.desc}\""
                    var lookup = createUniqueLookup(insn.insn.name.toValidIdentifier())
                        .withIcon(PlatformIcons.FIELD_ICON)
                        .withPresentableText(insn.insn.owner.substringAfterLast('/') + "." + insn.insn.name)
                        .withTypeText(Type.getType(insn.insn.desc).presentableName())
                        .withDefinitionAndFold(insn.insn.name.toValidIdentifier(), "field", definitionValue)
                    if (insn.insn.opcode == Opcodes.GETSTATIC || insn.insn.opcode == Opcodes.PUTSTATIC) {
                        lookup = lookup.withLookupString(insn.insn.owner.substringAfterLast('/') + "." + insn.insn.name)
                    }
                    return listOf(
                        lookup.createEliminable("field ${insn.insn.owner}.${insn.insn.name}:${insn.insn.desc}")
                    )
                }
            }
            is MethodInsnNode -> {
                if (canCompleteExprs) {
                    val definitionValue = "method = \"L${insn.insn.owner};${insn.insn.name}${insn.insn.desc}\""
                    var lookup = createUniqueLookup(insn.insn.name.toValidIdentifier())
                        .withIcon(PlatformIcons.METHOD_ICON)
                        .withPresentableText(insn.insn.owner.substringAfterLast('/') + "." + insn.insn.name)
                        .withDescTailText(insn.insn.desc)
                        .withTypeText(Type.getReturnType(insn.insn.desc).presentableName())
                        .withDefinitionAndFold(insn.insn.name.toValidIdentifier(), "method", definitionValue)
                    if (insn.insn.opcode == Opcodes.INVOKESTATIC) {
                        lookup = lookup.withLookupString(insn.insn.owner.substringAfterLast('/') + "." + insn.insn.name)
                    }
                    return listOf(
                        lookup.withTail(ParenthesesTailType(!insn.insn.desc.startsWith("()")))
                            .createEliminable("invoke ${insn.insn.owner}.${insn.insn.name}${insn.insn.desc}")
                    )
                }
            }
            is TypeInsnNode -> {
                val type = Type.getObjectType(insn.insn.desc)
                if (canCompleteTypes && type.isAccessibleFrom(mixinClass)) {
                    val lookup = createTypeLookup(type)
                    when (insn.insn.opcode) {
                        Opcodes.ANEWARRAY -> {
                            val arrayType = Type.getType('[' + Type.getObjectType(insn.insn.desc).descriptor)
                            return createNewArrayCompletion(flow, arrayType)
                        }
                        Opcodes.NEW -> {
                            val initCall = flow
                                ?.getDecoration<InstantiationInfo>(FlowDecorations.INSTANTIATION_INFO)
                                ?.initCall
                                ?.virtualInsnOrNull
                                ?.insn as? MethodInsnNode
                                ?: return emptyList()
                            return listOf(
                                lookup
                                    .withDescTailText(initCall.desc)
                                    .withTail(ParenthesesTailType(!initCall.desc.startsWith("()")))
                                    .createEliminable("new ${insn.insn.desc}${initCall.desc}")
                            )
                        }
                        else -> return listOf(lookup.createEliminable("type ${insn.insn.desc}"))
                    }
                }
            }
            is IntInsnNode -> {
                if (insn.insn.opcode == Opcodes.NEWARRAY) {
                    if (canCompleteTypes) {
                        val arrayType = Type.getType(
                            when (insn.insn.operand) {
                                Opcodes.T_BOOLEAN -> "[B"
                                Opcodes.T_CHAR -> "[C"
                                Opcodes.T_FLOAT -> "[F"
                                Opcodes.T_DOUBLE -> "[D"
                                Opcodes.T_BYTE -> "[B"
                                Opcodes.T_SHORT -> "[S"
                                Opcodes.T_INT -> "[I"
                                Opcodes.T_LONG -> "[J"
                                else -> "[Lnull;" // wtf?
                            }
                        )
                        return createNewArrayCompletion(flow, arrayType)
                    }
                }
            }
            is MultiANewArrayInsnNode -> {
                if (canCompleteTypes) {
                    val arrayType = Type.getType(insn.insn.desc)
                    return createNewArrayCompletion(flow, arrayType)
                }
            }
            is InsnNode -> {
                when (insn.insn.opcode) {
                    Opcodes.ARRAYLENGTH -> {
                        if (canCompleteExprs) {
                            return listOf(
                                createUniqueLookup("length")
                                    .withIcon(PlatformIcons.FIELD_ICON)
                                    .withTypeText("int")
                                    .createEliminable("arraylength")
                            )
                        }
                    }
                }
            }
            is InvokeDynamicInsnNode -> {
                if (insn.insn.bsm.owner == "java/lang/invoke/LambdaMetafactory") {
                    if (!canCompleteExprs) {
                        return emptyList()
                    }

                    val handle = insn.insn.bsmArgs.getOrNull(1) as? Handle ?: return emptyList()
                    val definitionValue = "method = \"L${handle.owner};${handle.name}${handle.desc}\""
                    if (handle.tag !in Opcodes.H_INVOKEVIRTUAL..Opcodes.H_INVOKEINTERFACE) {
                        return emptyList()
                    }
                    if (handle.tag == Opcodes.H_NEWINVOKESPECIAL) {
                        return listOf(
                            createTypeLookup(Type.getObjectType(handle.owner))
                                .withTailText("::new")
                                .withTail(COLON_COLON_NEW_TAIL)
                                .createEliminable("constructorRef ${handle.owner}")
                        )
                    } else {
                        return listOf(
                            createUniqueLookup(handle.name.toValidIdentifier())
                                .withIcon(PlatformIcons.METHOD_ICON)
                                .withPresentableText(handle.owner.substringAfterLast('/') + "." + handle.name)
                                .withTypeText(Type.getReturnType(handle.desc).presentableName())
                                .withDefinitionAndFold(handle.name.toValidIdentifier(), "method", definitionValue)
                                .createEliminable("methodRef ${handle.owner}.${handle.name}${handle.desc}")
                        )
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

    private fun createTypeLookup(type: Type): LookupElementBuilder {
        val definitionId = type.typeNameToInsert()

        val lookupElement = createUniqueLookup(definitionId)
            .withIcon(PlatformIcons.CLASS_ICON)
            .withPresentableText(type.presentableName())

        return if (type.isPrimitive) {
            lookupElement
        } else {
            lookupElement.withDefinition(definitionId, "type = ${type.canonicalName}.class")
        }
    }

    private fun createNewArrayCompletion(flow: FlowValue?, arrayType: Type): List<EliminableLookup> {
        val hasInitializer = flow?.hasDecoration(FlowDecorations.ARRAY_CREATION_INFO) == true
        val initializerText = if (hasInitializer) "{}" else ""
        return listOf(
            createTypeLookup(arrayType.elementType)
                .withTailText("[]".repeat(arrayType.dimensions) + initializerText)
                .withTail(
                    BracketsTailType(
                        arrayType.dimensions,
                        hasInitializer,
                    )
                )
                .createEliminable("new ${arrayType.descriptor}$initializerText")
        )
    }

    private fun createLocalVariableLookups(
        project: Project,
        targetClass: ClassNode,
        targetMethod: MethodNode,
        originalInsn: AbstractInsnNode,
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
                targetMethod.instructions.indexOf(originalInsn) in validRange
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
                val localName = localVariable.name.toValidIdentifier()
                createUniqueLookup(localName)
                    .withIcon(PlatformIcons.VARIABLE_ICON)
                    .withTypeText(localPsiType.presentableText)
                    .withLocalDefinition(
                        localName,
                        Type.getType(localVariable.desc),
                        ordinal,
                        isArgsOnly,
                        isImplicit,
                        mixinClass,
                    )
                    .createEliminable("local $localName", if (isImplicit) -1 else 0)
            }
        }

        // fallback to ASM dataflow
        val localTypes = AsmDfaUtil.getLocalVariableTypes(project, targetClass, targetMethod, originalInsn)
            ?: return emptyList()
        val localType = localTypes.getOrNull(index) ?: return emptyList()
        val ordinal = localTypes.asSequence().take(index).filter { it == localType }.count()
        val localName = localType.typeNameToInsert().replace("[]", "Array") + (ordinal + 1)
        val isImplicit = localTypes.count { it == localType } == 1
        return listOf(
            createUniqueLookup(localName)
                .withIcon(PlatformIcons.VARIABLE_ICON)
                .withTypeText(localType.presentableName())
                .withLocalDefinition(localName, localType, ordinal, isArgsOnly, isImplicit, mixinClass)
                .createEliminable("local $localName", if (isImplicit) -1 else 0)
        )
    }

    private fun LookupElementBuilder.withDescTailText(desc: String) =
        withTailText(
            Type.getArgumentTypes(desc).joinToString(prefix = "(", postfix = ")") { it.presentableName() }
        )

    private fun LookupElement.withTail(tailType: TailType?) = object : TailTypeDecorator<LookupElement>(this) {
        override fun computeTailType(context: InsertionContext?) = tailType
    }

    private fun LookupElementBuilder.withDefinition(id: String, definitionValue: String) =
        withDefinition(id, definitionValue) { _, _ -> }

    private fun LookupElementBuilder.withDefinitionAndFold(id: String, foldAttribute: String, definitionValue: String) =
        withDefinition(id, definitionValue) { context, annotation ->
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
                if (nameValuePair.name == foldAttribute &&
                    nameValuePair.parentOfType<PsiAnnotation>()?.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION)
                    == true
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

        val definitionValue = buildString {
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
        return withDefinition(name, definitionValue) { context, annotation ->
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
                override fun calculateLookupItems(context: TemplateExpressionContext?) = lookupItems
                override fun calculateQuickResult(context: TemplateExpressionContext?) = calculateResult(context)
                override fun calculateResult(context: TemplateExpressionContext?) =
                    TextResult("ordinal = $ordinal")
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
        definitionValue: String,
        crossinline andThen: (InsertionContext, PsiAnnotation) -> Unit
    ) = withInsertHandler { context, _ ->
        context.laterRunnable = Runnable {
            context.commitDocument()
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    val annotation = addDefinition(context, id, definitionValue)
                    if (annotation != null) {
                        andThen(context, annotation)
                    }
                }
            }
        }
    }

    private fun addDefinition(context: InsertionContext, id: String, definitionValue: String): PsiAnnotation? {
        val contextElement = context.file.findElementAt(context.startOffset) ?: return null
        return addDefinition(context.project, contextElement, id, definitionValue)
    }

    fun addDefinition(
        project: Project,
        contextElement: PsiElement,
        id: String,
        definitionValue: String
    ): PsiAnnotation? {
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
        var newAnnotation = JavaPsiFacade.getElementFactory(project).createAnnotationFromText(
            "@${MixinConstants.MixinExtras.DEFINITION}(id = \"$id\", $definitionValue)",
            modifierList,
        )
        var anchor = modifierList.annotations.lastOrNull { it.hasQualifiedName(MixinConstants.MixinExtras.DEFINITION) }
        if (anchor == null) {
            val definitionPosRelativeToExpression =
                MinecraftProjectSettings.getInstance(project).definitionPosRelativeToExpression
            if (definitionPosRelativeToExpression == BeforeOrAfter.AFTER) {
                anchor = expressionAnnotation
            }
        }
        newAnnotation = modifierList.addAfter(newAnnotation, anchor) as PsiAnnotation

        // add imports and reformat
        newAnnotation =
            JavaCodeStyleManager.getInstance(project).shortenClassReferences(newAnnotation) as PsiAnnotation
        JavaCodeStyleManager.getInstance(project).optimizeImports(modifierList.containingFile)
        val annotationIndex = modifierList.annotations.indexOf(newAnnotation)
        val formattedModifierList =
            CodeStyleManager.getInstance(project).reformat(modifierList) as PsiModifierList
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

                    val arrayLitExpr = factory.createExpression("new ?[]{?}") as MENewExpression
                    arrayLitExpr.type!!.replace(type)
                    variants += arrayLitExpr
                }
            }
            is MESuperCallExpression -> {
                // Might be missing its parentheses
                val callExpr = factory.createExpression("super.?()") as MESuperCallExpression
                expression.memberName?.let { callExpr.memberName!!.replace(it) }
                variants += callExpr
            }
        }

        return variants
    }

    private fun createUniqueLookup(text: String) = LookupElementBuilder.create(Any(), text)

    private fun LookupElement.createEliminable(uniquenessKey: String, priority: Int = 0) =
        EliminableLookup(uniquenessKey, this, priority)

    private class EliminableLookup(
        val uniquenessKey: String,
        val lookupElement: LookupElement,
        private val priority: Int
    ) : Comparable<EliminableLookup> {
        override fun compareTo(other: EliminableLookup) = priority.compareTo(other.priority)
    }

    private data class ExpandedInstruction(val insn: VirtualInsn, val originalInsn: AbstractInsnNode)

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
