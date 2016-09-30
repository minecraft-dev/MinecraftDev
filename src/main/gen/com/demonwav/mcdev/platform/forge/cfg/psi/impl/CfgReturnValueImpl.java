// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CLASS_VALUE;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgReturnValue;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CfgReturnValueImpl extends ASTWrapperPsiElement implements CfgReturnValue {

  public CfgReturnValueImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CfgVisitor visitor) {
    visitor.visitReturnValue(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfgVisitor) accept((CfgVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getClassValue() {
    return findNotNullChildByType(CLASS_VALUE);
  }

}
