/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.util.reference

import com.demonwav.mcdev.util.manipulator
import com.demonwav.mcdev.util.packageName
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiQualifiedNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.util.IncorrectOperationException
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext

abstract class PackageNameReferenceProvider : PsiReferenceProvider() {

    protected open val description
        get() = "package '%s'"

    protected open fun getBasePackage(element: PsiElement): String? = null

    protected open fun canBindTo(element: PsiElement) = element is PsiPackage

    protected open fun resolve(
        qualifiedName: String,
        element: PsiElement,
        facade: JavaPsiFacade
    ): Array<ResolveResult> {
        facade.findPackage(qualifiedName)?.let { return arrayOf(PsiElementResolveResult(it)) }
        return ResolveResult.EMPTY_ARRAY
    }

    protected abstract fun collectVariants(element: PsiElement, context: PsiElement?): Array<Any>

    protected fun collectSubpackages(context: PsiPackage, classes: Iterable<PsiClass>): Array<Any> {
        return collectPackageChildren(context, classes) {}
    }

    protected inline fun collectPackageChildren(
        context: PsiPackage,
        classes: Iterable<PsiClass>,
        classFunc: (PsiClass) -> Any?
    ): Array<Any> {
        val parentPackage = context.qualifiedName
        val subPackageStart = parentPackage.length + 1

        val packages = HashSet<String>()
        val list = ArrayList<Any>()

        for (psiClass in classes) {
            val packageName = psiClass.packageName ?: continue
            if (!packageName.startsWith(parentPackage)) {
                continue
            }
            if (packageName.length < subPackageStart) {
                classFunc(psiClass)?.let { list.add(it) }
                continue
            }

            val end = packageName.indexOf('.', subPackageStart)
            val nextName =
                if (end == -1) packageName.substring(subPackageStart) else packageName.substring(subPackageStart, end)

            if (packages.add(nextName)) {
                list.add(LookupElementBuilder.create(nextName).withIcon(PlatformIcons.PACKAGE_ICON))
            }
        }

        return list.toArray()
    }

    fun resolve(element: PsiElement): PsiElement? {
        val range = element.manipulator!!.getRangeInElement(element)
        return Reference(element, range, range.startOffset, null).multiResolve(false).firstOrNull()?.element
    }

    final override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val baseRange = element.manipulator!!.getRangeInElement(element)

        val text = element.text!!

        val start = baseRange.startOffset
        val end = baseRange.endOffset
        var pos = start

        var current: Reference? = null

        val list = ArrayList<PsiReference>()
        while (true) {
            val separatorPos = text.indexOf('.', pos)
            if (separatorPos == -1) {
                list.add(Reference(element, TextRange(pos, end), start, current))
                break
            }

            if (separatorPos >= end) {
                break
            }

            current = Reference(element, TextRange(pos, separatorPos), start, current)
            list.add(current)
            pos = separatorPos + 1

            if (pos == end) {
                list.add(Reference(element, TextRange(end, end), start, current))
                break
            }
        }

        return list.toTypedArray()
    }

    private inner class Reference(element: PsiElement, range: TextRange, start: Int, val previous: Reference?) :
        PsiReferenceBase.Poly<PsiElement>(element, range, false), InspectionReference {

        override val description: String
            get() = this@PackageNameReferenceProvider.description

        private val qualifiedRange = TextRange(start, range.endOffset)

        private val qualifiedName: String
            get() {
                val name = qualifiedRange.substring(element.text)
                return getBasePackage(element)?.let { it + '.' + name } ?: name
            }

        override val unresolved: Boolean
            get() = multiResolve(false).isEmpty()

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            return resolve(qualifiedName, element, JavaPsiFacade.getInstance(element.project))
        }

        override fun getVariants(): Array<Any> {
            return collectVariants(element, previous?.multiResolve(false)?.firstOrNull()?.element)
        }

        private fun getNewName(newTarget: PsiQualifiedNamedElement): String {
            val newName = newTarget.qualifiedName!!
            return getBasePackage(element)?.let { newName.removePrefix(it + '.') } ?: newName
        }

        override fun bindToElement(newTarget: PsiElement): PsiElement? {
            if (!canBindTo(newTarget)) {
                throw IncorrectOperationException("Cannot bind to $newTarget")
            }

            if (super.isReferenceTo(newTarget)) {
                return element
            }

            val newName = getNewName(newTarget as PsiQualifiedNamedElement)
            return element.manipulator?.handleContentChange(element, qualifiedRange, newName)
        }

        override fun isReferenceTo(element: PsiElement) = canBindTo(element) && super.isReferenceTo(element)
    }
}
