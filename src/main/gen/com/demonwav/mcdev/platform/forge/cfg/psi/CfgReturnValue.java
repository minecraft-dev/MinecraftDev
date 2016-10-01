// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface CfgReturnValue extends PsiElement {

  @Nullable
  PsiElement getClassValue();

  @Nullable
  PsiElement getPrimitive();

  String getReturnValueText();

  void setReturnValue(String returnValue);

}
