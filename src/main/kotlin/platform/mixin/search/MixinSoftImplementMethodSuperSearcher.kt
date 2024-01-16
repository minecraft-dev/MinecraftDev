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

package com.demonwav.mcdev.platform.mixin.search

import com.demonwav.mcdev.platform.mixin.util.forEachSoftImplementedMethods
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.searches.SuperMethodsSearch
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class MixinSoftImplementMethodSuperSearcher :
    QueryExecutor<MethodSignatureBackedByPsiMethod, SuperMethodsSearch.SearchParameters> {

    override fun execute(
        queryParameters: SuperMethodsSearch.SearchParameters,
        consumer: Processor<in MethodSignatureBackedByPsiMethod>,
    ): Boolean {
        if (queryParameters.psiClass != null) {
            return true // Not entirely sure what this is used for
        }

        val method = queryParameters.method
        val checkBases = queryParameters.isCheckBases

        // This is very simple and probably doesn't handle all cases
        // Right now we simply check for @Implements annotation on the class and look
        // for a similar method in the interface
        runReadAction run@{
            if (!method.name.contains('$') || method.hasModifierProperty(PsiModifier.STATIC)) {
                return@run true
            }

            // Don't return anything if method has an @Override annotation because that would be an error
            if (method.modifierList.findAnnotation(CommonClassNames.JAVA_LANG_OVERRIDE) != null) {
                return@run true
            }

            method.forEachSoftImplementedMethods(checkBases) {
                if (!consumer.process(it.hierarchicalMethodSignature)) {
                    return@run false
                }
            }
        }

        return true
    }
}
