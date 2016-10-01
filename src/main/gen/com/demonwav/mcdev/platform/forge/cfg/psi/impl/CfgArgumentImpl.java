// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CLASS_VALUE;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.PRIMITIVE;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgArgument;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CfgArgumentImpl extends ASTWrapperPsiElement implements CfgArgument {

  public CfgArgumentImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CfgVisitor visitor) {
    visitor.visitArgument(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfgVisitor) accept((CfgVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiElement getClassValue() {
    return findChildByType(CLASS_VALUE);
  }

  @Override
  @Nullable
  public PsiElement getPrimitive() {
    return findChildByType(PRIMITIVE);
  }

  public Class<?> getPrimitiveArgumentType() {
    return CfgPsiImplUtil.getPrimitiveArgumentType(this);
  }

  public String getArgumentText() {
    return CfgPsiImplUtil.getArgumentText(this);
  }

  public void setArgument(String argument) {
    CfgPsiImplUtil.setArgument(this, argument);
  }

}
