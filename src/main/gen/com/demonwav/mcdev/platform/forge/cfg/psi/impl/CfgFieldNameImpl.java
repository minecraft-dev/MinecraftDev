// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.NAME_ELEMENT;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgFieldName;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CfgFieldNameImpl extends ASTWrapperPsiElement implements CfgFieldName {

  public CfgFieldNameImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CfgVisitor visitor) {
    visitor.visitFieldName(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfgVisitor) accept((CfgVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getNameElement() {
    return findNotNullChildByType(NAME_ELEMENT);
  }

}
