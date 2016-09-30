// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgArguments;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFuncName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgReturnValue;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CfgFunctionImpl extends ASTWrapperPsiElement implements CfgFunction {

  public CfgFunctionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CfgVisitor visitor) {
    visitor.visitFunction(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfgVisitor) accept((CfgVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public CfgArguments getArguments() {
    return findChildByClass(CfgArguments.class);
  }

  @Override
  @NotNull
  public CfgFuncName getFuncName() {
    return findNotNullChildByClass(CfgFuncName.class);
  }

  @Override
  @NotNull
  public CfgReturnValue getReturnValue() {
    return findNotNullChildByClass(CfgReturnValue.class);
  }

}
