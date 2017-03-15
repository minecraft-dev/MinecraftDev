/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.platform.mcp.at

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

class AtCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,
               PlatformPatterns.psiElement().withLanguage(AtLanguage),
               object : CompletionProvider<CompletionParameters>() {
                   override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
                       result.addElement(LookupElementBuilder.create("public"))
                       result.addElement(LookupElementBuilder.create("protected"))
                       result.addElement(LookupElementBuilder.create("private"))
                       result.addElement(LookupElementBuilder.create("default"))
                   }
               }
        )
    }
}
