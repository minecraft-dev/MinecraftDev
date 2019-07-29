/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.search

import com.demonwav.mcdev.platform.mixin.util.findSoftImplementedMethods
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
        consumer: Processor<in MethodSignatureBackedByPsiMethod>
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

            method.findSoftImplementedMethods(checkBases) {
                if (!consumer.process(it.hierarchicalMethodSignature)) {
                    return@run false
                }
            }
        }

        return true
    }
}
