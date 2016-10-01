package com.demonwav.mcdev.platform.mcp.cfg.psi.mixins;

import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgArgument;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgFuncName;
import com.demonwav.mcdev.platform.mcp.cfg.psi.CfgReturnValue;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CfgFunctionMixin extends PsiElement {

    @NotNull
    List<CfgArgument> getArgumentList();

    @NotNull
    CfgFuncName getFuncName();

    @NotNull
    CfgReturnValue getReturnValue();

    void setArgumentList(@NotNull String argumentList);

    void setReturnValue(@NotNull String returnValue);

    void setFunction(@NotNull String function);
}
