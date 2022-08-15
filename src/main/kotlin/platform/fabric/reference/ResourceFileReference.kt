/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2022 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.fabric.reference

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.util.SourceType
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.manipulator
import com.demonwav.mcdev.util.reference.InspectionReference
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ProcessingContext

class ResourceFileReference(private val description: String) : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        return arrayOf(Reference(description, element as JsonStringLiteral))
    }

    private class Reference(desc: String, element: JsonStringLiteral) :
        PsiReferenceBase<JsonStringLiteral>(element),
        InspectionReference {
        override val description = desc
        override val unresolved = resolve() == null

        override fun resolve(): PsiElement? {
            val module = element.findModule() ?: return null
            val facet = MinecraftFacet.getInstance(module) ?: return null
            val virtualFile = facet.findFile(element.value, SourceType.RESOURCE) ?: return null
            return PsiManager.getInstance(element.project).findFile(virtualFile)
        }

        override fun bindToElement(newTarget: PsiElement): PsiElement? {
            if (newTarget !is PsiFile) {
                throw IncorrectOperationException("Cannot target $newTarget")
            }
            val manipulator = element.manipulator ?: return null
            return manipulator.handleContentChange(element, manipulator.getRangeInElement(element), newTarget.name)
        }
    }
}
