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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.handlers.desugar.DesugarUtil
import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.platform.mixin.util.InjectionPointSpecifier
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.SLICE
import com.demonwav.mcdev.platform.mixin.util.SourceCodeLocationInfo
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.createLiteralExpression
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.internalName
import com.demonwav.mcdev.util.memoized
import com.demonwav.mcdev.util.realName
import com.demonwav.mcdev.util.shortName
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.KeyedExtensionCollector
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.parentOfType
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.ArrayUtilRt
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.containers.sequenceOfNotNull
import com.intellij.util.xmlb.annotations.Attribute
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LineNumberNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

abstract class InjectionPoint<T : PsiElement> {
    companion object {
        private val COLLECTOR =
            KeyedExtensionCollector<InjectionPoint<*>, String>("com.demonwav.minecraft-dev.injectionPoint")

        fun byAtCode(atCode: String): InjectionPoint<*>? {
            return COLLECTOR.findSingle(atCode)
        }
    }

    open fun usesMemberReference() = false

    open fun onCompleted(editor: Editor, reference: PsiLiteral) {
    }

    protected fun completeExtraStringAtAttribute(editor: Editor, reference: PsiLiteral, attributeName: String) {
        val at = reference.parentOfType<PsiAnnotation>() ?: return
        if (at.findDeclaredAttributeValue(attributeName) != null) {
            return
        }
        at.setDeclaredAttributeValue(
            attributeName,
            JavaPsiFacade.getElementFactory(reference.project).createLiteralExpression("")
        )
        val formattedAt = CodeStyleManager.getInstance(reference.project).reformat(at) as PsiAnnotation
        val targetElement = formattedAt.findDeclaredAttributeValue(attributeName) ?: return
        editor.caretModel.moveToOffset(targetElement.textRange.startOffset + 1)
    }

    open fun getArgsKeys(at: PsiAnnotation): Array<String> {
        return ArrayUtilRt.EMPTY_STRING_ARRAY
    }

    open fun getArgsValues(at: PsiAnnotation, key: String): Array<out Any> {
        return ArrayUtilRt.EMPTY_OBJECT_ARRAY
    }

    open fun getArgValueListDelimiter(at: PsiAnnotation, key: String): Regex? = null

    open val discouragedMessage: String? = null

    open fun isShiftDiscouraged(shift: Int): Boolean = shift != 0

    abstract fun createNavigationVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: PsiClass,
    ): NavigationVisitor?

    abstract fun doCreateCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
    ): CollectVisitor<T>?

    fun createCollectVisitor(
        at: PsiAnnotation,
        target: MixinSelector?,
        targetClass: ClassNode,
        mode: CollectVisitor.Mode,
    ): CollectVisitor<T>? {
        return doCreateCollectVisitor(at, target, targetClass, mode)?.also {
            val isInsideSlice = at.parentOfType<PsiAnnotation>()?.hasQualifiedName(SLICE) == true
            val defaultSpecifier = if (isInsideSlice) InjectionPointSpecifier.FIRST else InjectionPointSpecifier.ALL
            addFilters(at, targetClass, it, defaultSpecifier)
        }
    }

    protected open fun addFilters(
        at: PsiAnnotation,
        targetClass: ClassNode,
        collectVisitor: CollectVisitor<T>,
        defaultSpecifier: InjectionPointSpecifier
    ) {
        addStandardFilters(at, targetClass, collectVisitor, defaultSpecifier)
    }

    fun addStandardFilters(
        at: PsiAnnotation,
        targetClass: ClassNode,
        collectVisitor: CollectVisitor<T>,
        defaultSpecifier: InjectionPointSpecifier
    ) {
        addShiftSupport(at, targetClass, collectVisitor)
        addSliceFilter(at, targetClass, collectVisitor)
        // make sure the ordinal filter is last, so that the ordinal only increments once the other filters have passed
        addOrdinalFilter(at, targetClass, collectVisitor)
        addSpecifierFilter(at, targetClass, collectVisitor, defaultSpecifier)
    }

    protected open fun addShiftSupport(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<*>) {
        collectVisitor.shiftBy = AtResolver.getShift(at)
    }

    protected open fun addSliceFilter(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<T>) {
        // resolve slice annotation, take into account slice id if present
        val sliceId = at.findDeclaredAttributeValue("slice")?.constantStringValue
        val parentAnnotation = at.parentOfType<PsiAnnotation>() ?: return
        val slices = parentAnnotation.findDeclaredAttributeValue("slice")?.findAnnotations() ?: return
        val slice = if (sliceId != null) {
            slices.singleOrNull { aSlice ->
                val aSliceId = aSlice.findDeclaredAttributeValue("id")?.constantStringValue
                    ?: return@singleOrNull false
                aSliceId == sliceId
            }
        } else {
            slices.singleOrNull()
        } ?: return

        // precompute what we can
        val from = slice.findDeclaredAttributeValue("from") as? PsiAnnotation
        val to = slice.findDeclaredAttributeValue("to") as? PsiAnnotation
        if (from == null && to == null) {
            return
        }

        fun resolveSliceIndex(
            sliceAt: PsiAnnotation?,
            method: MethodNode,
        ): Int? {
            return sliceAt?.let {
                AtResolver(sliceAt, targetClass, method).resolveInstructions()
                    .singleOrNull()
                    ?.let { method.instructions.indexOf(it.insn) }
            }
        }

        collectVisitor.addResultFilter("slice") { results, method ->
            val insns = method.instructions
            val fromInsnIndex = resolveSliceIndex(from, method) ?: 0
            val toInsnIndex = resolveSliceIndex(to, method) ?: insns.size()
            results.filter { insns.indexOf(it.insn) in fromInsnIndex.. toInsnIndex }
        }
    }

    protected open fun addOrdinalFilter(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<T>) {
        val ordinal = at.findDeclaredAttributeValue("ordinal")?.constantValue as? Int ?: return
        if (ordinal < 0) return
        collectVisitor.addResultFilter("ordinal") { results, _ ->
            results.drop(ordinal).take(1)
        }
    }

    protected open fun addSpecifierFilter(
        at: PsiAnnotation,
        targetClass: ClassNode,
        collectVisitor: CollectVisitor<T>,
        defaultSpecifier: InjectionPointSpecifier
    ) {
        val point = at.findDeclaredAttributeValue("value")?.constantStringValue ?: return
        val specifier = InjectionPointSpecifier.entries.firstOrNull { point.endsWith(":$it") } ?: defaultSpecifier
        collectVisitor.addResultFilter("specifier") { results, _ ->
            val single = when (specifier) {
                InjectionPointSpecifier.FIRST -> results.firstOrNull()
                InjectionPointSpecifier.LAST -> results.lastOrNull()
                InjectionPointSpecifier.ONE -> results.singleOrNull()
                InjectionPointSpecifier.ALL -> return@addResultFilter results
            }
            sequenceOfNotNull(single)
        }
    }

    abstract fun createLookup(targetClass: ClassNode, result: CollectVisitor.Result<T>): LookupElementBuilder?

    protected fun LookupElementBuilder.setBoldIfInClass(member: PsiMember, clazz: ClassNode): LookupElementBuilder {
        if (member.containingClass?.fullQualifiedName?.replace('.', '/') == clazz.name) {
            return bold()
        }
        return this
    }
}

class InjectionPointInfo : BaseKeyedLazyInstance<InjectionPoint<*>>(), KeyedLazyInstance<InjectionPoint<*>> {
    @Attribute("atCode")
    @RequiredElement
    lateinit var atCode: String

    @Attribute("implementation")
    @RequiredElement
    lateinit var implementation: String

    override fun getImplementationClassName(): String {
        return implementation
    }

    override fun getKey(): String {
        return atCode
    }
}

abstract class QualifiedInjectionPoint<T : PsiMember> : InjectionPoint<T>() {

    final override fun usesMemberReference() = true

    protected abstract fun createLookup(targetClass: ClassNode, m: T, owner: String): LookupElementBuilder

    protected open fun getInternalName(m: T): String {
        return m.realName ?: m.name!!
    }

    final override fun createLookup(
        targetClass: ClassNode,
        result: CollectVisitor.Result<T>,
    ): LookupElementBuilder {
        return qualifyLookup(
            createLookup(targetClass, result.target, result.qualifier ?: targetClass.name),
            targetClass,
            result.target,
        )
    }

    private fun qualifyLookup(
        builder: LookupElementBuilder,
        targetClass: ClassNode,
        m: T,
    ): LookupElementBuilder {
        val owner = m.containingClass ?: return builder
        return if (targetClass.name == owner.fullQualifiedName?.replace('.', '/')) {
            builder
        } else {
            // Qualify member with name of owning class
            builder.withPresentableText(owner.shortName + '.' + getInternalName(m))
        }
    }
}

abstract class AbstractMethodInjectionPoint : QualifiedInjectionPoint<PsiMethod>() {

    override fun createLookup(targetClass: ClassNode, m: PsiMethod, owner: String): LookupElementBuilder {
        return JavaLookupElementBuilder.forMethod(
            m,
            m.getQualifiedMemberReference(owner).toMixinString(),
            PsiSubstitutor.EMPTY,
            null,
        )
            .setBoldIfInClass(m, targetClass)
            .withPresentableText(m.internalName) // Display internal name (e.g. <init> for constructors)
            .withLookupString(m.internalName) // Allow looking up targets by their method name
    }

    override fun getInternalName(m: PsiMethod): String {
        return m.internalName
    }
}

abstract class NavigationVisitor : JavaRecursiveElementVisitor() {
    val result = mutableListOf<PsiElement>()

    protected fun addResult(element: PsiElement) {
        if (!DesugarUtil.isFake(element)) {
            result += element
        }
    }

    open fun configureBytecodeTarget(classNode: ClassNode, methodNode: MethodNode) {
    }

    open fun visitStart(executableElement: PsiElement) {
    }

    open fun visitEnd(executableElement: PsiElement) {
    }

    override fun visitMethod(method: PsiMethod) {
        // do not recurse into methods
    }

    override fun visitAnonymousClass(aClass: PsiAnonymousClass) {
        // do not recurse into anonymous classes
    }

    override fun visitClass(aClass: PsiClass) {
        // do not recurse into inner classes
    }

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {
        // do not recurse into lambda expressions
    }
}

abstract class CollectVisitor<T : PsiElement>(protected val mode: Mode) {
    fun visit(methodNode: MethodNode): InsnResolutionInfo<T> {
        val numRetained = IntArray(resultFilters.size + 1)
        var results = accept(methodNode).onEach { numRetained[0]++ }
        for ((i, filter) in resultFilters.asSequence().map { it.second }.withIndex()) {
            results = filter(results, methodNode).onEach { numRetained[i + 1]++ }
        }
        results = results.memoized()
        if (results.iterator().hasNext()) {
            return InsnResolutionInfo.Success(results)
        }
        val filterStats = resultFilters.asSequence()
            .map { it.first }
            .zip(numRetained.asSequence().zipWithNext(Int::minus))
            .toMap()
        return InsnResolutionInfo.Failure(filterStats)
    }

    fun addResultFilter(name: String, filter: CollectResultFilter<T>) {
        resultFilters += name to filter
    }

    protected abstract fun accept(methodNode: MethodNode): Sequence<Result<T>>

    private var nextIndex = 0
    private val nextIndexByLine = mutableMapOf<Int, Int>()
    private val resultFilters = mutableListOf<Pair<String, CollectResultFilter<T>>>()
    internal var shiftBy = 0

    protected suspend fun SequenceScope<Result<T>>.addResult(
        insn: AbstractInsnNode,
        element: T,
        qualifier: String? = null,
        decorations: Map<String, Any?> = emptyMap(),
    ) {
        // apply shift.
        // being able to break out of the shift loops is important to prevent IDE freezes in case of large shift bys.
        var shiftedInsn: AbstractInsnNode? = insn
        if (shiftBy < 0) {
            for (i in shiftBy until 0) {
                shiftedInsn = shiftedInsn!!.previous
                if (shiftedInsn == null) {
                    break
                }
            }
        } else if (shiftBy > 0) {
            for (i in 0 until shiftBy) {
                shiftedInsn = shiftedInsn!!.next
                if (shiftedInsn == null) {
                    break
                }
            }
        }

        val index = nextIndex++
        val lineNumber = getLineNumber(insn)
        val indexInLineNumber = lineNumber?.let { nextIndexByLine.merge(it, 1, Int::plus)!! - 1 } ?: index
        val result = Result(
            SourceCodeLocationInfo(index, lineNumber, indexInLineNumber),
            insn,
            shiftedInsn ?: return,
            element,
            qualifier,
            if (insn === shiftedInsn) decorations else emptyMap()
        )

        yield(result)
    }

    private fun getLineNumber(insn: AbstractInsnNode): Int? {
        var i: AbstractInsnNode? = insn
        while (i != null) {
            if (i is LineNumberNode) {
                return i.line
            }
            i = i.previous
        }

        return null
    }

    data class Result<out T : PsiElement>(
        val sourceLocationInfo: SourceCodeLocationInfo,
        val originalInsn: AbstractInsnNode,
        val insn: AbstractInsnNode,
        val target: T,
        val qualifier: String? = null,
        val decorations: Map<String, Any?>
    ) {
        val index: Int get() = sourceLocationInfo.index
    }

    enum class Mode { RESOLUTION, COMPLETION }
}

fun nodeMatchesSelector(
    insn: MethodInsnNode,
    mode: CollectVisitor.Mode,
    selector: MixinSelector,
    project: Project,
): PsiMethod? {
    if (mode != CollectVisitor.Mode.COMPLETION) {
        if (!selector.matchMethod(insn.owner, insn.name, insn.desc)) {
            return null
        }
    }

    val fakeMethod = insn.fakeResolve()

    return fakeMethod.method.findOrConstructSourceMethod(
        fakeMethod.clazz,
        project,
        canDecompile = false,
    )
}

typealias CollectResultFilter<T> =
        (Sequence<CollectVisitor.Result<T>>, MethodNode) -> Sequence<CollectVisitor.Result<T>>
