package com.demonwav.mcdev.platform.mcp.cfg;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class CfgCompletionContributor extends CompletionContributor {
    public CfgCompletionContributor() {
        extend(CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(CfgLanguage.getInstance()),
            new CompletionProvider<CompletionParameters>() {
                @Override
                protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {

                    result.addElement(LookupElementBuilder.create("public"));
                    result.addElement(LookupElementBuilder.create("public-f"));
                    result.addElement(LookupElementBuilder.create("protected"));
                    result.addElement(LookupElementBuilder.create("protected-f"));
                    result.addElement(LookupElementBuilder.create("private"));
                    result.addElement(LookupElementBuilder.create("private-f"));
                }
            });
    }
}
