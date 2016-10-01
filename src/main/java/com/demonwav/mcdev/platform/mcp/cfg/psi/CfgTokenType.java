package com.demonwav.mcdev.platform.mcp.cfg.psi;

import com.demonwav.mcdev.platform.mcp.cfg.CfgLanguage;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;

public class CfgTokenType extends IElementType {
    public CfgTokenType(@NonNls final String debugName) {
        super(debugName, CfgLanguage.getInstance());
    }
}
