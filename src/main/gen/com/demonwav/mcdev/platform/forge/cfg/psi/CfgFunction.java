// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface CfgFunction extends PsiElement {

  @NotNull
  List<CfgArgument> getArgumentList();

  @NotNull
  CfgFuncName getFuncName();

  @NotNull
  CfgReturnValue getReturnValue();

  void setFunction(String function);

  void setArgumentList(String arguments);

  void setReturnValue(String returnValue);

}
