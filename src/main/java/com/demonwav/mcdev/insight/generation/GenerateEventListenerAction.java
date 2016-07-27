package com.demonwav.mcdev.insight.generation;

import com.intellij.codeInsight.generation.actions.BaseGenerateAction;

public class GenerateEventListenerAction extends BaseGenerateAction {
    public GenerateEventListenerAction() {
        super(new GenerateEventListenerHandler());
    }
}
