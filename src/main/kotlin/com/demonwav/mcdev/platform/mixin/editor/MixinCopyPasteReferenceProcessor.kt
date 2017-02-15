/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.editor

import com.demonwav.mcdev.platform.mixin.actions.insertShadows
import com.demonwav.mcdev.platform.mixin.util.MixinUtils
import com.demonwav.mcdev.util.findContainingClass
import com.demonwav.mcdev.util.findField
import com.demonwav.mcdev.util.findJavaCodeReferenceElement
import com.demonwav.mcdev.util.findMethods
import com.demonwav.mcdev.util.qualifiedMemberReference
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
import com.intellij.util.containers.isEmpty
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

        val member = when (resolved) {
            is PsiMethod -> {
                if (resolved.isConstructor) {
                    return
                }

                resolved.qualifiedMemberReference
            }
            is PsiField -> resolved.qualifiedMemberReference
            else -> return
        }

        to.add(MixinReferenceData.create(element, startOffset, member))
    }

    override fun findReferencesToRestore(file: PsiFile, bounds: RangeMarker, referenceData: Array<out ReferenceData>)
            : Array<PsiJavaCodeReferenceElement?> {
        val refs = super.findReferencesToRestore(file, bounds, referenceData)

        // Check if pasting to Mixin class
        val elementInTargetClass = file.findElementAt(bounds.startOffset) ?: return refs
        val psiClass = elementInTargetClass.findContainingClass() ?: return refs

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
            val reference = element.findJavaCodeReferenceElement() ?: continue

            val endOffset = data.endOffset + bounds.startOffset

            if (!reference.textRange.equalsToRange(startOffset, endOffset)) {
                continue
            }

            // Check if reference already exists in Mixin class
            val name = data.staticMemberName!!
            if ('(' in name) {
                // Check if method does not already exist in target class
                if (!psiClass.findMethods(data.reference).isEmpty()) {
                    continue
                }
            } else {
                // Field
                if (psiClass.findField(data.reference) != null) {
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
                    psiClass = reference.findContainingClass() ?: return
                }

                // Lookup target class
                val targetClass = JavaPsiFacade.getInstance(psiClass.project).findClass(data.qClassName, reference.resolveScope) ?: continue

                val name = data.staticMemberName!!

                if ('(' in name) {
                    // Add target method
                    val targetMethod = targetClass.findMethods(data.reference).findAny().orElse(null) ?: continue
                    members.add(targetMethod)
                } else {
                    // Add target field
                    val targetField = targetClass.findField(data.reference) ?: continue
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
