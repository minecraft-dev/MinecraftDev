package com.demonwav.mcdev.platform.mcp.at.psi;

import com.demonwav.mcdev.platform.mcp.at.AtLanguage;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;

public class AtTokenType extends IElementType {
    public AtTokenType(@NonNls final String debugName) {
        super(debugName, AtLanguage.getInstance());
    }
}
