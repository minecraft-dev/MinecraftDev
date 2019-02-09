/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2018 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.completion

import com.demonwav.mcdev.platform.sponge.inspection.SpongeInvalidGetterTargetInspection
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.packageName
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.AutoCompletionPolicy
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.*
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

class SpongeGetterFilterCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        if (!JavaCompletionContributor.isInJavaContext(position)) {
            return
        }

        val eventHandler = position.findContainingMethod() ?: return
        if (!eventHandler.hasParameters()) {
            return
        }

        if (!eventHandler.isValidSpongeListener()) {
            return
        }

        val annotation = position.parentOfType(PsiAnnotation::class)
        if (annotation == null || annotation.qualifiedName?.equals(SpongeConstants.GETTER_ANNOTATION) == false) {
            val param = position.parentOfType(PsiParameter::class)
            if (param != null && eventHandler.parameters.contains(param) && eventHandler.parameters[0] != param && PsiTreeUtil.nextLeaf(position) is PsiReferenceParameterList) {
                val projectScope = ProjectScope.getAllScope(parameters.position.project)
                val getterAnnoClass = JavaPsiFacade.getInstance(parameters.position.project).findClass(SpongeConstants.GETTER_ANNOTATION, projectScope)
                if (getterAnnoClass != null) {
                    result.addElement(LookupElementBuilder.createWithIcon(getterAnnoClass)
                            .appendTailText(" (${getterAnnoClass.packageName})", true)
                            .withTypeText("EventFilter")
                            .withInsertHandler { context, _ ->
                                val at = context.document.text[context.startOffset - 1]
                                if (at != '@') {
                                    context.document.insertString(context.startOffset, "@")
                                    context.commitDocument()
                                }

                                val inserted = context.file.findElementAt(context.startOffset)?.parentOfType(PsiAnnotation::class) ?: return@withInsertHandler
                                SpongeInvalidGetterTargetInspection.QuickFix.doFix(getterAnnoClass.project, inserted)
                            }
                    )
                }
            }
        }

        if (!SkipAutopopupInStrings.isInStringLiteral(position)) {
            return
        }

        val memberValue = position.parentOfType(PsiNameValuePair::class) ?: return
        if (memberValue.attributeName != "value") {
            return
        }

        val eventReferenceType = eventHandler.parameters[0].type as? JvmReferenceType ?: return
        val eventClass = eventReferenceType.resolve() as PsiClass
        for (method in eventClass.allMethods) {
            if (method.returnType != PsiType.VOID && method.containingClass?.qualifiedName != "java.lang.Object") {
                result.addElement(JavaLookupElementBuilder.forMethod(method as PsiMethod, PsiSubstitutor.EMPTY)
                        .withAutoCompletionPolicy(AutoCompletionPolicy.GIVE_CHANCE_TO_OVERWRITE))
            }
        }
    }
}
