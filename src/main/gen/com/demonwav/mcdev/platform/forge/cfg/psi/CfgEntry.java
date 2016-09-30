// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CfgEntry extends PsiElement {

  @NotNull
  CfgClassName getClassName();

  @Nullable
  CfgFieldName getFieldName();

  @Nullable
  CfgFunction getFunction();

  @NotNull
  CfgKeyword getKeyword();

}
