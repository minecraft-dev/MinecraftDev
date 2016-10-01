// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface CfgArgument extends PsiElement {

  @Nullable
  PsiElement getClassValue();

  @Nullable
  PsiElement getPrimitive();

  Class<?> getPrimitiveArgumentType();

  String getArgumentText();

  void setArgument(String argument);

}
