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

import com.demonwav.mcdev.util.PsiChildPointer
import com.demonwav.mcdev.util.cached
import com.demonwav.mcdev.util.childrenOfType
import com.demonwav.mcdev.util.createChildPointer
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.lockedCached
import com.demonwav.mcdev.util.normalize
import com.demonwav.mcdev.util.packageName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.UnfairTextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.platform.workspace.storage.impl.cache.CachedValue
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiParenthesizedExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiSubstitutor
import com.intellij.psi.PsiSuperExpression
import com.intellij.psi.PsiThisExpression
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeCastExpression
import com.intellij.psi.PsiTypeParameter
import com.intellij.psi.PsiTypes
import com.intellij.psi.PsiVariable
import com.intellij.psi.impl.light.LightMemberReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil
import com.intellij.psi.util.parents
import com.intellij.refactoring.util.ConflictsUtil
import com.intellij.refactoring.util.classMembers.ClassThisReferencesVisitor
import com.intellij.util.JavaPsiConstructorUtil
import com.intellij.util.Processor
import com.intellij.util.containers.MultiMap
import com.siyeh.ig.psiutils.PsiElementOrderComparator
import java.util.Objects
import org.jetbrains.annotations.VisibleForTesting
import org.objectweb.asm.Handle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.util.Printer

object DesugarUtil {
    private val ORIGINAL_ELEMENT_KEY = Key.create<PsiElement>("mcdev.desugar.originalElement")
    private val UNNAMED_VARIABLE_KEY = Key.create<Boolean>("mcdev.desugar.unnamedVariable")
    private val INDY_DATA_KEY = Key.create<IndyData>("mcdev.desugar.indyData")
    private val OTHER_FLOW_CHILDREN_KEY = Key.create<MutableList<PsiChildPointer<PsiExpression>>>("mcdev.desugar.otherFlowChildren")
    private val VALUE_POPPED_KEY = Key.create<Boolean>("mcdev.desugar.valuePopped")
    private val FAKE_KEY = Key.create<Boolean>("mcdev.desugar.fake")

    private val DESUGARERS = arrayOf(
        MethodReferenceToLambdaDesugarer,
        RemoveVarArgsDesugarer,
        AnonymousAndLocalClassDesugarer,
        FieldAssignmentDesugarer,
        LambdaDesugarer,
    )

    fun getOriginalElement(desugared: PsiElement): PsiElement? {
        return desugared.parents(true).firstNotNullOfOrNull { it.getCopyableUserData(ORIGINAL_ELEMENT_KEY) }
    }

    fun setOriginalElement(desugared: PsiElement, original: PsiElement?) {
        desugared.putCopyableUserData(ORIGINAL_ELEMENT_KEY, original)
    }

    fun getOriginalToDesugaredMap(desugared: PsiElement): Map<PsiElement, List<PsiElement>> {
        val desugaredFile = desugared.containingFile ?: return emptyMap()
        return desugaredFile.cached {
            val result = mutableMapOf<PsiElement, MutableList<PsiElement>>()
            PsiTreeUtil.processElements(desugaredFile) { desugaredElement ->
                desugaredElement.getCopyableUserData(ORIGINAL_ELEMENT_KEY)?.let { original ->
                    result.getOrPut(original) { mutableListOf() } += desugaredElement
                }
                true
            }
            result
        }
    }

    fun isUnnamedVariable(variable: PsiVariable): Boolean {
        return variable.getCopyableUserData(UNNAMED_VARIABLE_KEY) == true
    }

    fun setUnnamedVariable(variable: PsiVariable, value: Boolean) {
        variable.putCopyableUserData(UNNAMED_VARIABLE_KEY, value)
    }

    fun getIndyData(methodCall: PsiMethodCallExpression): IndyData? {
        return methodCall.getCopyableUserData(INDY_DATA_KEY)
    }

    fun isIndy(methodCall: PsiMethodCallExpression): Boolean {
        return getIndyData(methodCall) != null
    }

    fun setIndyData(methodCall: PsiMethodCallExpression, indyData: IndyData) {
        methodCall.putCopyableUserData(INDY_DATA_KEY, indyData)
    }

    fun addOtherFlowChild(expression: PsiExpression, child: PsiExpression) {
        val otherFlowChildren = expression.getCopyableUserData(OTHER_FLOW_CHILDREN_KEY)
        if (otherFlowChildren == null) {
            expression.putCopyableUserData(OTHER_FLOW_CHILDREN_KEY, mutableListOf(expression.createChildPointer(child)))
        } else {
            otherFlowChildren += expression.createChildPointer(child)
        }
    }

    fun getOtherFlowChildren(expression: PsiExpression): List<PsiExpression> {
        return expression.getCopyableUserData(OTHER_FLOW_CHILDREN_KEY)
            ?.mapNotNull { it.dereference(expression) }
            ?: emptyList()
    }

    fun isValuePopped(expression: PsiExpression): Boolean {
        return expression.getCopyableUserData(VALUE_POPPED_KEY) == true
    }

    fun setValuePopped(expression: PsiExpression, value: Boolean) {
        expression.putCopyableUserData(VALUE_POPPED_KEY, value)
    }

    fun isFake(element: PsiElement): Boolean {
        return element.getCopyableUserData(FAKE_KEY) == true
    }

    fun setFake(element: PsiElement, value: Boolean) {
        element.putCopyableUserData(FAKE_KEY, value)
    }

    fun desugar(project: Project, clazz: PsiClass, context: DesugarContext): PsiClass? {
        val file = clazz.containingFile as? PsiJavaFile ?: return null
        return file.cached {
            val desugaredFile = file.copy() as PsiJavaFile
            setOriginalRecursive(desugaredFile, file)
            for (desugarer in DESUGARERS) {
                desugarer.desugar(project, desugaredFile, context)
            }
            getOriginalToDesugaredMap(desugaredFile)[clazz]?.filterIsInstance<PsiClass>()?.firstOrNull()
        }
    }

    @VisibleForTesting
    fun setOriginalRecursive(desugared: PsiElement, original: PsiElement) {
        val desugaredElements = mutableListOf<PsiElement>()
        desugared.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                desugaredElements.add(element)
            }
        })

        val originalElements = mutableListOf<PsiElement>()
        original.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                originalElements.add(element)
            }
        })

        for ((originalElement, desugaredElement) in originalElements.zip(desugaredElements)) {
            setOriginalElement(desugaredElement, originalElement)
        }
    }

    internal fun allClasses(file: PsiJavaFile): List<PsiClass> {
        return file.childrenOfType<PsiClass>().filter { it !is PsiTypeParameter }
    }

    internal fun allClassesShallow(clazz: PsiClass): List<PsiClass> {
        val allClasses = mutableListOf<PsiClass>()
        clazz.acceptChildren(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitClass(aClass: PsiClass) {
                if (aClass !is PsiTypeParameter) {
                    allClasses += aClass
                }
            }
        })
        return allClasses
    }

    internal fun findReferencesInFile(element: PsiElement): List<PsiReference> {
        fun PsiMember.createSyntheticReference(): PsiReference {
            return object : LightMemberReference(manager, this, PsiSubstitutor.EMPTY) {
                override fun getElement() = this@createSyntheticReference
                override fun getRangeInElement(): TextRange {
                    val identifier = (this@createSyntheticReference as? PsiNameIdentifierOwner)?.nameIdentifier
                    if (identifier != null) {
                        val startOffsetInParent = identifier.startOffsetInParent
                        return if (startOffsetInParent >= 0) {
                            TextRange.from(startOffsetInParent, identifier.textLength)
                        } else {
                            UnfairTextRange(-1, -1)
                        }
                    }

                    return super.getRangeInElement()
                }
            }
        }

        val file = element.containingFile as? PsiJavaFile ?: return emptyList()
        val results = mutableListOf<PsiReference>()

        ReferencesSearch.search(element, LocalSearchScope(file)).forEach(Processor {
            results += it
            true
        })

        // subclass constructor references don't work for non-physical files
        if (element is PsiMethod && element.isConstructor) {
            val clazz = element.containingClass
            if (clazz != null) {
                for (subClass in allClasses(file)) {
                    if (subClass !is PsiAnonymousClass && subClass.isInheritor(clazz, false)) {
                        val countImplicitSuperCalls = element.parameterList.isEmpty
                        val constructors = subClass.constructors
                        for (constructor in constructors) {
                            val thisOrSuperCall = JavaPsiConstructorUtil.findThisOrSuperCallInConstructor(constructor)
                            if (JavaPsiConstructorUtil.isSuperConstructorCall(thisOrSuperCall)) {
                                val reference = thisOrSuperCall!!.methodExpression.reference
                                if (reference != null && reference.isReferenceTo(element)) {
                                    results += reference
                                }
                            } else if (thisOrSuperCall == null && countImplicitSuperCalls) {
                                results += constructor.createSyntheticReference()
                            }
                        }

                        if (constructors.isEmpty() && countImplicitSuperCalls) {
                            results += subClass.createSyntheticReference()
                        }
                    }
                }
            }
        }

        return results
    }

    internal fun elementNeedsThis(containingClass: PsiClass, element: PsiElement): Boolean {
        val elementNeedsThis = object : ClassThisReferencesVisitor(containingClass) {
            var result = false

            override fun visitExplicitThis(referencedClass: PsiClass?, reference: PsiThisExpression?) {
                result = true
            }

            override fun visitExplicitSuper(referencedClass: PsiClass?, reference: PsiSuperExpression?) {
                result = true
            }

            override fun visitClassMemberReferenceElement(
                classMember: PsiMember?,
                classMemberReference: PsiJavaCodeReferenceElement?
            ) {
                if (classMember == null || classMember == element) {
                    return
                }
                if (classMember.hasModifierProperty(PsiModifier.STATIC)) {
                    return
                }
                if (classMember is PsiTypeParameter) {
                    return
                }
                result = true
            }
        }

        element.accept(elementNeedsThis)
        return elementNeedsThis.result
    }

    internal fun getTypeParametersToCopy(
        sourceContext: PsiElement,
        destContext: PsiElement,
        capturedVariables: Iterable<PsiVariable>,
        extraTypes: Iterable<PsiType> = emptyList(),
    ): List<PsiTypeParameter> {
        val typeParameters = mutableListOf<PsiTypeParameter>()
        val addedTypeParameters = mutableSetOf<PsiTypeParameter>()
        val visitQueue = ArrayDeque<PsiElement>()

        fun acceptTypeParameter(typeParam: PsiTypeParameter) {
            val owner = typeParam.owner
            if (owner != null && !PsiTreeUtil.isAncestor(sourceContext, owner, false) &&
                !PsiTreeUtil.isAncestor(owner, destContext, false)) {
                if (addedTypeParameters.add(typeParam)) {
                    typeParameters += typeParam
                    visitQueue.add(typeParam.extendsList)
                }
            }
        }

        val visitor = object : JavaRecursiveElementWalkingVisitor() {
            override fun visitReferenceElement(reference: PsiJavaCodeReferenceElement) {
                super.visitReferenceElement(reference)
                val resolved = reference.resolve()
                if (resolved is PsiTypeParameter) {
                    acceptTypeParameter(resolved)
                }
            }
        }

        visitQueue.add(sourceContext)
        for (capturedVariable in capturedVariables) {
            capturedVariable.typeElement?.let(visitQueue::add)
        }
        for (type in extraTypes) {
            if (type is PsiClassType) {
                val resolved = type.resolve()
                if (resolved is PsiTypeParameter) {
                    acceptTypeParameter(resolved)
                }
            }
        }
        while (visitQueue.isNotEmpty()) {
            visitQueue.removeFirst().accept(visitor)
        }

        typeParameters.sortWith(PsiElementOrderComparator.getInstance())
        return typeParameters
    }

    internal fun generateMethodName(project: Project, containingClass: PsiClass, args: List<PsiType>): String {
        val factory = JavaPsiFacade.getElementFactory(project)

        val params = args.withIndex().map { (index, type) -> factory.createParameter("p$index", type.normalize()) }

        var i = 1
        while (true) {
            val templateMethod = factory.createMethod("synthetic$i", PsiTypes.voidType())
            for (param in params) {
                templateMethod.parameterList.add(param)
            }
            val conflicts = MultiMap<PsiElement, String>()
            ConflictsUtil.checkMethodConflicts(containingClass, null, templateMethod, conflicts)
            if (conflicts.isEmpty) {
                return "synthetic$i"
            }
            i++
        }
    }

    internal fun createNullCheck(project: Project, expression: PsiExpression, classVersion: Int): PsiExpression {
        val factory = JavaPsiFacade.getElementFactory(project)
        if (classVersion >= Opcodes.V9) {
            val nonNullAssertion = factory.createExpressionFromText(
                "java.util.Objects.requireNonNull(expr)",
                expression
            ) as PsiMethodCallExpression
            val innerExpr = nonNullAssertion.argumentList.expressions.first().replace(expression)
                    as PsiExpression
            addOtherFlowChild(nonNullAssertion, innerExpr)
            setValuePopped(nonNullAssertion, true)
            return nonNullAssertion
        } else {
            var getClassExpr = factory.createExpressionFromText(
                "(x).getClass()",
                expression
            ) as PsiMethodCallExpression
            val exprType = expression.type
            val nonNullAssertion = if (exprType != null) {
                val castExpr = factory.createExpressionFromText(
                    "(Type) x",
                    null
                ) as PsiTypeCastExpression
                castExpr.castType!!.replace(factory.createTypeElement(exprType))
                getClassExpr = castExpr.operand!!.replace(getClassExpr) as PsiMethodCallExpression
                setFake(castExpr, true)
                setValuePopped(getClassExpr, true)
                castExpr
            } else {
                getClassExpr
            }
            val innerExpr =
                (getClassExpr.methodExpression.qualifierExpression as PsiParenthesizedExpression).expression!!.replace(expression)
                    as PsiExpression
            addOtherFlowChild(nonNullAssertion, innerExpr)
            setValuePopped(nonNullAssertion, true)
            return nonNullAssertion
        }
    }

    // see com.sun.tools.javac.comp.Lower.access, accReq
    fun needsBridgeMethod(expression: PsiJavaCodeReferenceElement, classVersion: Int): Boolean {
        // method calls with qualified super need bridge methods
        if (expression is PsiMethodCallExpression) {
            val qualifier = PsiUtil.skipParenthesizedExprDown(expression.methodExpression.qualifierExpression)
            if (qualifier is PsiSuperExpression && qualifier.qualifier != null) {
                return true
            }
        }

        val resolved = expression.resolve() as? PsiMember ?: return false
        val resolvedClass = resolved.containingClass ?: return false
        val fromClass = expression.findContainingClass() ?: return false

        if (resolvedClass == fromClass) {
            return false
        }

        if (classVersion <= Opcodes.V1_8 && resolved.hasModifierProperty(PsiModifier.PRIVATE)) {
            return true
        }

        if (resolved.hasModifierProperty(PsiModifier.PROTECTED) &&
            fromClass.packageName != resolvedClass.packageName &&
            !fromClass.isInheritor(resolvedClass, true)
        ) {
            return true
        }

        return false
    }

    class IndyData(val methodName: String, val methodDesc: String, val bsm: Handle, vararg val bsmArgs: Any) {
        override fun toString(): String {
            return buildString {
                append("IndyData(\n")
                objectToString(methodName, "    ").append(",\n")
                objectToString(methodDesc, "    ").append(",\n")
                objectToString(bsm, "    ").append(",\n")
                for (bsmArg in bsmArgs) {
                    objectToString(bsmArg, "    ").append(",\n")
                }
                append(")")
            }
        }

        private fun StringBuilder.objectToString(obj: Any, indent: String): StringBuilder {
            append(indent)
            when (obj) {
                is Float -> append(obj).append("F")
                is Long -> append(obj).append("L")
                is Int, is Double -> append(obj)
                is String -> append('"').append(StringUtil.escapeStringCharacters(obj).replace("$", "\\$")).append('"')
                is Type -> append("Type.").append(if (obj.sort == Type.METHOD) { "getMethodType" } else { "getType" })
                    .append("(\"").append(StringUtil.escapeStringCharacters(obj.descriptor).replace("$", "\\$")).append("\")")
                is Handle -> {
                    append("Handle(\n")
                    val innerIndent = "$indent    "
                    append(innerIndent).append("Opcodes.").append(Printer.HANDLE_TAG[obj.tag]).append(",\n")
                    objectToString(obj.owner, innerIndent).append(",\n")
                    objectToString(obj.name, innerIndent).append(",\n")
                    objectToString(obj.desc, innerIndent).append(",\n")
                    append(innerIndent).append(obj.isInterface).append("\n")
                    append(indent).append(")")
                }
            }
            return this
        }

        override fun hashCode(): Int {
            return Objects.hash(methodName, methodDesc, bsm, bsmArgs.contentDeepHashCode())
        }

        override fun equals(other: Any?): Boolean {
            if (other !is IndyData) {
                return false
            }

            return methodName == other.methodName &&
                methodDesc == other.methodDesc &&
                bsm == other.bsm &&
                bsmArgs.contentDeepEquals(other.bsmArgs)
        }
    }
}
