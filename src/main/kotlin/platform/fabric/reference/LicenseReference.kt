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

package com.demonwav.mcdev.platform.fabric.reference

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.rootManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.ResolveResult
import com.intellij.util.ProcessingContext

object LicenseReference : PsiReferenceProvider() {
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        return arrayOf(Reference(element as JsonStringLiteral))
    }

    private class Reference(element: JsonStringLiteral) :
        PsiReferenceBase<JsonStringLiteral>(element),
        PsiPolyVariantReference {

        override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
            val modules = ModuleManager.getInstance(element.project).modules
            val psiManager = PsiManager.getInstance(element.project)
            return modules.flatMap { module ->
                module.rootManager.contentRoots.mapNotNull {
                    val licenseFile = it.findChild("LICENSE")
                        ?: it.findChild("LICENSE.txt")
                        ?: return@mapNotNull null
                    val psiLicenseFile = psiManager.findFile(licenseFile) ?: return@mapNotNull null
                    PsiElementResolveResult(psiLicenseFile)
                }
            }.toTypedArray()
        }

        override fun resolve(): PsiElement? {
            val results = multiResolve(false)
            return if (results.size == 1) {
                results[0].element
            } else {
                null
            }
        }
    }
}
