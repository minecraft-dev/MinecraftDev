/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mixin.search

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.IMPLEMENTS
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findAnnotations
import com.demonwav.mcdev.util.resolveClass
import com.demonwav.mcdev.util.runInlineReadAction
import com.intellij.psi.CommonClassNames
import com.intellij.psi.HierarchicalMethodSignature
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.searches.SuperMethodsSearch
import com.intellij.psi.util.MethodSignature
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.intellij.psi.util.MethodSignatureUtil
import com.intellij.util.Processor
import com.intellij.util.QueryExecutor

class MixinImplementsMethodSuperSearcher : QueryExecutor<MethodSignatureBackedByPsiMethod, SuperMethodsSearch.SearchParameters> {

    override fun execute(queryParameters: SuperMethodsSearch.SearchParameters,
                         consumer: Processor<MethodSignatureBackedByPsiMethod>): Boolean {

        if (queryParameters.psiClass != null) {
            return true // Not entirely sure what this is used for
        }

        val method = queryParameters.method
        val checkBases = queryParameters.isCheckBases

        // This is very simple and probably doesn't handle all cases
        // Right now we simply check for @Implements annotation on the class and look
        // for a similar method in the interface
        runInlineReadAction {
            val methodName = method.name
            if (!methodName.contains('$') || method.hasModifierProperty(PsiModifier.STATIC)) {
                return true
            }

            // Don't return anything if method has an @Override annotation because that would be an error
            if (method.modifierList.findAnnotation(CommonClassNames.JAVA_LANG_OVERRIDE) != null) {
                return true
            }

            val containingClass = method.containingClass ?: return true
            if (!containingClass.isMixin) {
                return true
            }

            val implements = containingClass.modifierList!!.findAnnotation(IMPLEMENTS) ?: return true
            val interfaces = implements.findDeclaredAttributeValue(null)?.findAnnotations() ?: return true
            if (interfaces.isEmpty()) {
                return true
            }

            val signature = method.hierarchicalMethodSignature

            for (interfaceAnnotation in interfaces) {
                val prefix = interfaceAnnotation.findDeclaredAttributeValue("prefix")?.constantStringValue ?: continue
                if (!methodName.startsWith(prefix)) {
                    continue
                }

                val iface = interfaceAnnotation.findDeclaredAttributeValue("iface")?.resolveClass() ?: continue
                if (!iface.isInterface) {
                    continue
                }

                val realName = methodName.removePrefix(prefix)
                val prefixedSignature = PrefixedMethodSignature(realName, signature)

                val methods = iface.findMethodsByName(methodName.removePrefix(prefix), checkBases)
                for (superMethod in methods) {
                    if (superMethod.hasModifierProperty(PsiModifier.STATIC)) {
                        continue
                    }

                    val superSignature = superMethod.hierarchicalMethodSignature
                    if (MethodSignatureUtil.isSubsignature(prefixedSignature, superSignature) && !consumer.process(superSignature)) {
                        return false
                    }
                }
            }
        }

        return true
    }

    private class PrefixedMethodSignature(private val name: String, signature: HierarchicalMethodSignature) : MethodSignature by signature {
        override fun getName() = name
    }
}
