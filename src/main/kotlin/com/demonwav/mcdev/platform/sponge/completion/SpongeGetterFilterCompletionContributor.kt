/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2019 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.sponge.completion

import com.demonwav.mcdev.platform.sponge.inspection.SpongeInvalidGetterTargetInspection
import com.demonwav.mcdev.platform.sponge.util.SpongeConstants
import com.demonwav.mcdev.platform.sponge.util.isValidSpongeListener
import com.demonwav.mcdev.platform.sponge.util.resolveSpongeGetterTarget
import com.demonwav.mcdev.util.findContainingMethod
import com.demonwav.mcdev.util.withImportInsertion
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.JavaCompletionContributor
import com.intellij.codeInsight.completion.JavaLookupElementBuilder
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.jvm.types.JvmReferenceType
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.JavaTokenType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiJavaToken
import com.intellij.psi.PsiModifierList
import com.intellij.psi.PsiType
import com.intellij.psi.PsiTypeElement
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.PropertyUtil
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType
import org.jetbrains.plugins.groovy.lang.psi.util.childrenOfType

class SpongeGetterFilterCompletionContributor : CompletionContributor() {

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        val position = parameters.position
        if (!JavaCompletionContributor.isInJavaContext(position)) {
            return
        }

        val eventHandler = position.findContainingMethod() ?: return
        if (!eventHandler.isValidSpongeListener()) {
            return
        }

        val prevVisibleLeaf = PsiTreeUtil.prevVisibleLeaf(position)
        if (prevVisibleLeaf is PsiJavaToken && prevVisibleLeaf.tokenType == JavaTokenType.COMMA) {
            // We are right after a comma
            val projectScope = ProjectScope.getAllScope(parameters.position.project)
            val getterAnnoClass = JavaPsiFacade.getInstance(parameters.position.project)
                .findClass(SpongeConstants.GETTER_ANNOTATION, projectScope)

            if (getterAnnoClass != null) {
                result.addElement(JavaLookupElementBuilder.forClass(getterAnnoClass, getterAnnoClass.name, true)
                    .withTypeText("Event filter")
                    .withInsertHandler { context, _ ->
                        val at = context.document.text[context.startOffset - 1]
                        if (at != '@') {
                            context.document.insertString(context.startOffset, "@")
                            context.commitDocument()
                        }

                        val inserted = context.file.findElementAt(context.startOffset)
                            ?.parentOfType(PsiAnnotation::class) ?: return@withInsertHandler
                        SpongeInvalidGetterTargetInspection.QuickFix.doFix(getterAnnoClass.project, inserted)
                    }
                )
            }
        } else if (prevVisibleLeaf is PsiJavaToken && prevVisibleLeaf.tokenType != JavaTokenType.AT) {
            // We are not trying to complete an annotation
            val modifierList = prevVisibleLeaf.parentOfType<PsiModifierList>()
            val getterAnnotation = modifierList?.childrenOfType<PsiAnnotation>()
                ?.firstOrNull { it.hasQualifiedName(SpongeConstants.GETTER_ANNOTATION) }
            if (getterAnnotation != null) {
                // We are right after a @Getter()
                completeGetterParameterType(getterAnnotation, result)
            }
        }

        val paramType = PsiTreeUtil.getPrevSiblingOfType(position, PsiTypeElement::class.java)
        if (paramType != null) {
            // We are completing the parameter name
            val mods = PsiTreeUtil.getPrevSiblingOfType(paramType, PsiModifierList::class.java)
            val getter = mods?.childrenOfType<PsiAnnotation>()
                ?.firstOrNull { it.hasQualifiedName(SpongeConstants.GETTER_ANNOTATION) }
            val getterTargetName = getter?.resolveSpongeGetterTarget()?.name
            if (getterTargetName != null) {
                val propertyName = PropertyUtil.getPropertyName(getterTargetName)
                if (propertyName != null) {
                    val element = LookupElementBuilder.create(propertyName)
                    result.addElement(PrioritizedLookupElement.withPriority(element, 100.0))
                }
            }
        }
    }

    private fun completeGetterParameterType(annotation: PsiAnnotation, result: CompletionResultSet) {
        val getterTarget = annotation.resolveSpongeGetterTarget()
        val classType = getterTarget?.returnType
        if (classType != null) {
            suggestGetterParameter(classType, result)
        }

        val getterTargetClass = (getterTarget?.returnType as? PsiClassType)?.resolve()
        if (getterTargetClass != null && getterTargetClass.qualifiedName == SpongeConstants.OPTIONAL) {
            val paramRefType = getterTarget.returnTypeElement?.type as JvmReferenceType
            val psiType = paramRefType.typeArguments().firstOrNull() as? PsiType
            if (psiType != null) {
                suggestGetterParameter(psiType, result)
            }
        }
    }

    private fun suggestGetterParameter(psiType: PsiType, result: CompletionResultSet) {
        if (psiType is PsiClassType) {
            val resolveResult = psiType.resolveGenerics()
            val resolvedClass = resolveResult.element ?: return
            val genericTypes = resolveResult.substitutor.substitutionMap.values

            val genericClasses = mutableListOf(resolvedClass)
            genericTypes.mapNotNullTo(genericClasses) { (it as? PsiClassType)?.resolve() }

            val element = JavaLookupElementBuilder.forClass(resolvedClass, psiType.presentableText, true)
                .withTypeText("@Getter target type")
                .withImportInsertion(genericClasses)
            result.addElement(element)
        }
    }
}
