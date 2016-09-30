// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CfgFunction extends PsiElement {

  @Nullable
  CfgArguments getArguments();

  @NotNull
  CfgFuncName getFuncName();

  @NotNull
  CfgReturnValue getReturnValue();

}
