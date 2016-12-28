/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2016 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.editor

import com.demonwav.mcdev.platform.mixin.actions.insertShadows
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findFieldByNameAndDescriptor
import com.demonwav.mcdev.util.findMethodsByInternalNameAndDescriptor
import com.demonwav.mcdev.util.findParent
import com.demonwav.mcdev.util.getClassOfElement
import com.demonwav.mcdev.util.internalNameAndDescriptor
import com.demonwav.mcdev.util.nameAndDescriptor
import com.intellij.codeInsight.editorActions.JavaCopyPasteReferenceProcessor
import com.intellij.codeInsight.editorActions.ReferenceData
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement
import com.intellij.psi.PsiMember
import com.intellij.psi.PsiMethod
import java.util.ArrayList

/**
 * Automatically creates @Shadows for the referenced field and methods when
 * copying from a target class.
 */
class MixinCopyPasteReferenceProcessor : JavaCopyPasteReferenceProcessor() {

    override fun addReferenceData(file: PsiFile, startOffset: Int, element: PsiElement, to: ArrayList<ReferenceData>) {
        super.addReferenceData(file, startOffset, element, to)

        val reference = element as? PsiJavaCodeReferenceElement ?: return
        val resolved = reference.advancedResolve(false).element as? PsiMember ?: return

        val name = when (resolved) {
            is PsiMethod -> {
                if (resolved.isConstructor) {
                    return
                }

                resolved.internalNameAndDescriptor
            }
            is PsiField -> resolved.nameAndDescriptor
            else -> return
        }

        val resolvedOwner = resolved.containingClass ?: return
        val qualifiedName = resolvedOwner.qualifiedName ?: return
        to.add(MixinReferenceData.create(element, startOffset, qualifiedName, name))
    }

    override fun findReferencesToRestore(file: PsiFile, bounds: RangeMarker, referenceData: Array<out ReferenceData>)
            : Array<PsiJavaCodeReferenceElement?> {
        val refs = super.findReferencesToRestore(file, bounds, referenceData)

        // Check if pasting to Mixin class
        val elementInTargetClass = file.findElementAt(bounds.startOffset) ?: return refs
        val psiClass = getClassOfElement(elementInTargetClass) ?: return refs

        val targets = MixinUtils.getAllMixedClasses(psiClass).values
        if (targets.isEmpty()) {
            // Not a Mixin, so there is no need to add @Shadow members
            return refs
        }

        val targetNames = targets.mapNotNull { it.qualifiedName }

        for ((i, data) in referenceData.withIndex()) {
            if (data !is MixinReferenceData || data.qClassName !in targetNames) {
                continue
            }

            val startOffset = data.startOffset + bounds.startOffset

            val element = file.findElementAt(startOffset) ?: continue
            val reference = findParent<PsiJavaCodeReferenceElement>(element) ?: continue

            val endOffset = data.endOffset + bounds.startOffset

            if (!reference.textRange.equalsToRange(startOffset, endOffset)) {
                continue
            }

            // Check if reference already exists in Mixin class
            val name = data.staticMemberName!!
            if ('(' in name) {
                // Check if method does not already exist in target class
                if (psiClass.findMethodsByInternalNameAndDescriptor(name).findAny().isPresent) {
                    continue
                }
            } else {
                // Field
                if (psiClass.findFieldByNameAndDescriptor(name, false) != null) {
                    continue
                }
            }

            refs[i] = reference
        }

        return refs
    }

    override fun restoreReferences(referenceData: Array<out ReferenceData>, refs: Array<PsiJavaCodeReferenceElement?>) {
        val members = ArrayList<PsiMember>()

        var psiClass: PsiClass? = null

        for ((i, data) in referenceData.withIndex()) {
            val reference = refs[i] ?: continue

            if (data !is MixinReferenceData) {
                continue
            }

            try {
                // Store Mixin class so we can use it later
                if (psiClass == null) {
                    psiClass = (getClassOfElement(reference) ?: return)
                }

                // Lookup target class
                val targetClass = JavaPsiFacade.getInstance(psiClass.project).findClass(data.qClassName, reference.resolveScope) ?: continue

                val name = data.staticMemberName!!

                if ('(' in name) {
                    // Add target method
                    val targetMethod = targetClass.findMethodsByInternalNameAndDescriptor(name).findAny().orElse(null) ?: continue
                    members.add(targetMethod)
                } else {
                    // Add target field
                    val targetField = targetClass.findFieldByNameAndDescriptor(name) ?: continue
                    members.add(targetField)
                }
            } finally {
                refs[i] = null
            }
        }

        // Create @Shadows
        if (psiClass != null) {
            insertShadows(psiClass.project, psiClass, members.stream())
        }

        super.restoreReferences(referenceData, refs)
    }

}
