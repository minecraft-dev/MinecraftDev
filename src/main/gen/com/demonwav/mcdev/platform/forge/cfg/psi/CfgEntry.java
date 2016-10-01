// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.demonwav.mcdev.platform.forge.cfg.CfgElementFactory.Keyword;

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

  void setEntry(String entry);

  void setKeyword(Keyword keyword);

  void setClassName(String className);

  void setFieldName(String fieldName);

  void setFunction(String function);

}
