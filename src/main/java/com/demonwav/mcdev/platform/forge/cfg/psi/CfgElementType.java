package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.demonwav.mcdev.platform.forge.cfg.CfgLanguage;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CfgElementType extends IElementType {
    public CfgElementType(@NotNull @NonNls String debugName) {
        super(debugName, CfgLanguage.getInstance());
    }
}
