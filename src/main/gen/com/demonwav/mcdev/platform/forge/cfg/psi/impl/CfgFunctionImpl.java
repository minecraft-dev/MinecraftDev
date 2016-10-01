// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgArgument;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFuncName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFunction;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgReturnValue;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
  @NotNull
  public List<CfgArgument> getArgumentList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, CfgArgument.class);
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

  public void setFunction(String function) {
    CfgPsiImplUtil.setFunction(this, function);
  }

  public void setArgumentList(String arguments) {
    CfgPsiImplUtil.setArgumentList(this, arguments);
  }

  public void setReturnValue(String returnValue) {
    CfgPsiImplUtil.setReturnValue(this, returnValue);
  }

}
