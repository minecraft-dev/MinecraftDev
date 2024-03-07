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

package com.demonwav.mcdev.platform.mixin.handlers.injectionPoint

import com.demonwav.mcdev.platform.mixin.reference.MixinSelector
import com.demonwav.mcdev.platform.mixin.reference.toMixinString
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Classes.SHIFT
import com.demonwav.mcdev.platform.mixin.util.fakeResolve
import com.demonwav.mcdev.platform.mixin.util.findOrConstructSourceMethod
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.constantValue
import com.demonwav.mcdev.util.createLiteralExpression
import com.demonwav.mcdev.util.equivalentTo
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.fullQualifiedName
import com.demonwav.mcdev.util.getQualifiedMemberReference
import com.demonwav.mcdev.util.internalName
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
import com.intellij.psi.PsiEnumConstant
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiLambdaExpression
import com.intellij.psi.PsiLiteral
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodReferenceExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parentOfType
import com.intellij.serviceContainer.BaseKeyedLazyInstance
import com.intellij.util.ArrayUtilRt
import com.intellij.util.KeyedLazyInstance
import com.intellij.util.xmlb.annotations.Attribute
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
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

    open fun getArgsValues(at: PsiAnnotation, key: String): Array<Any> {
        return ArrayUtilRt.EMPTY_OBJECT_ARRAY
    }

    open fun isArgValueList(at: PsiAnnotation, key: String) = false

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
            addFilters(at, targetClass, it)
        }
    }

    protected open fun addFilters(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<T>) {
        addStandardFilters(at, targetClass, collectVisitor)
    }

    fun addStandardFilters(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<*>) {
        addShiftSupport(at, targetClass, collectVisitor)
        addSliceFilter(at, targetClass, collectVisitor)
        // make sure the ordinal filter is last, so that the ordinal only increments once the other filters have passed
        addOrdinalFilter(at, targetClass, collectVisitor)
    }

    protected open fun addShiftSupport(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<*>) {
        val shiftAttr = at.findDeclaredAttributeValue("shift") as? PsiExpression ?: return
        val shiftReference = PsiUtil.skipParenthesizedExprDown(shiftAttr) as? PsiReferenceExpression ?: return
        val shift = shiftReference.resolve() as? PsiEnumConstant ?: return
        val containingClass = shift.containingClass ?: return
        val shiftClass = JavaPsiFacade.getInstance(at.project).findClass(SHIFT, at.resolveScope) ?: return
        if (!(containingClass equivalentTo shiftClass)) return
        when (shift.name) {
            "BEFORE" -> collectVisitor.shiftBy = -1
            "AFTER" -> collectVisitor.shiftBy = 1
            "BY" -> {
                val by = at.findDeclaredAttributeValue("by")?.constantValue as? Int ?: return
                collectVisitor.shiftBy = by
            }
        }
    }

    protected open fun addSliceFilter(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<*>) {
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
        val fromSelector = from?.findDeclaredAttributeValue("value")?.constantStringValue?.let { atCode ->
            SliceSelector.values().firstOrNull { atCode.endsWith(":${it.name}") }
        } ?: SliceSelector.FIRST
        val toSelector = to?.findDeclaredAttributeValue("value")?.constantStringValue?.let { atCode ->
            SliceSelector.values().firstOrNull { atCode.endsWith(":${it.name}") }
        } ?: SliceSelector.FIRST

        fun resolveSliceIndex(
            sliceAt: PsiAnnotation?,
            selector: SliceSelector,
            insns: InsnList,
            method: MethodNode,
        ): Int? {
            return sliceAt?.let {
                val results = AtResolver(sliceAt, targetClass, method).resolveInstructions()
                val insn = if (selector == SliceSelector.LAST) {
                    results.lastOrNull()?.insn
                } else {
                    results.firstOrNull()?.insn
                }
                insn?.let { insns.indexOf(it) }
            }
        }

        // allocate lazy indexes so we don't have to re-run the at resolver for the slices each time
        var fromInsnIndex: Int? = null
        var toInsnIndex: Int? = null

        collectVisitor.addResultFilter("slice") { result, method ->
            val insns = method.instructions ?: return@addResultFilter true
            if (fromInsnIndex == null) {
                fromInsnIndex = resolveSliceIndex(from, fromSelector, insns, method) ?: 0
            }
            if (toInsnIndex == null) {
                toInsnIndex = resolveSliceIndex(to, toSelector, insns, method) ?: insns.size()
            }

            insns.indexOf(result.insn) in fromInsnIndex!!..toInsnIndex!!
        }
    }

    protected open fun addOrdinalFilter(at: PsiAnnotation, targetClass: ClassNode, collectVisitor: CollectVisitor<*>) {
        val ordinal = at.findDeclaredAttributeValue("ordinal")?.constantValue as? Int ?: return
        if (ordinal < 0) return
        collectVisitor.addResultFilter("ordinal") { _, _ ->
            collectVisitor.ordinal++ == ordinal
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
    private var hasVisitedAnything = false

    protected fun addResult(element: PsiElement) {
        result += element
    }

    open fun configureBytecodeTarget(classNode: ClassNode, methodNode: MethodNode) {
    }

    open fun visitStart(executableElement: PsiElement) {
    }

    open fun visitEnd(executableElement: PsiElement) {
    }

    override fun visitElement(element: PsiElement) {
        hasVisitedAnything = true
        super.visitElement(element)
    }

    override fun visitMethod(method: PsiMethod) {
        if (!hasVisitedAnything) {
            visitStart(method)
            super.visitMethod(method)
            visitEnd(method)
        }
    }

    override fun visitAnonymousClass(aClass: PsiAnonymousClass) {
        // do not recurse into anonymous classes
        if (!hasVisitedAnything) {
            visitStart(aClass)
            super.visitAnonymousClass(aClass)
            visitEnd(aClass)
        }
    }

    override fun visitClass(aClass: PsiClass) {
        // do not recurse into inner classes
        if (!hasVisitedAnything) {
            visitStart(aClass)
            super.visitClass(aClass)
            visitEnd(aClass)
        }
    }

    override fun visitMethodReferenceExpression(expression: PsiMethodReferenceExpression) {
        val hadVisitedAnything = hasVisitedAnything
        if (!hadVisitedAnything) {
            visitStart(expression)
        }
        super.visitMethodReferenceExpression(expression)
        if (!hadVisitedAnything) {
            visitEnd(expression)
        }
    }

    override fun visitLambdaExpression(expression: PsiLambdaExpression) {
        // do not recurse into lambda expressions
        if (!hasVisitedAnything) {
            visitStart(expression)
            super.visitLambdaExpression(expression)
            visitEnd(expression)
        }
    }
}

abstract class CollectVisitor<T : PsiElement>(protected val mode: Mode) {
    fun visit(methodNode: MethodNode) {
        this.method = methodNode
        try {
            accept(methodNode)
        } catch (e: StopWalkingException) {
            // ignore
        }
    }

    fun addResultFilter(name: String, filter: CollectResultFilter<T>) {
        resultFilters += name to filter
    }

    protected abstract fun accept(methodNode: MethodNode)

    private lateinit var method: MethodNode
    private var nextIndex = 0
    val result = mutableListOf<Result<T>>()
    private val resultFilters = mutableListOf<Pair<String, CollectResultFilter<T>>>()
    var filterToBlame: String? = null
    internal var ordinal = 0
    internal var shiftBy = 0

    protected fun addResult(
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

        val result = Result(
            nextIndex++,
            insn,
            shiftedInsn ?: return,
            element,
            qualifier,
            if (insn === shiftedInsn) decorations else emptyMap()
        )
        var isFiltered = false
        for ((name, filter) in resultFilters) {
            if (!filter(result, method)) {
                isFiltered = true
                if (filterToBlame == null) {
                    filterToBlame = name
                }
                break
            }
        }
        if (!isFiltered) {
            this.result.add(result)
            if (mode == Mode.MATCH_FIRST) {
                stopWalking()
            }
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    protected fun stopWalking() {
        throw StopWalkingException()
    }

    private class StopWalkingException : Exception() {
        override fun fillInStackTrace(): Throwable {
            return this
        }
    }

    data class Result<T : PsiElement>(
        val index: Int,
        val originalInsn: AbstractInsnNode,
        val insn: AbstractInsnNode,
        val target: T,
        val qualifier: String? = null,
        val decorations: Map<String, Any?>
    )

    enum class Mode { MATCH_ALL, MATCH_FIRST, COMPLETION }
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

typealias CollectResultFilter<T> = (CollectVisitor.Result<T>, MethodNode) -> Boolean
