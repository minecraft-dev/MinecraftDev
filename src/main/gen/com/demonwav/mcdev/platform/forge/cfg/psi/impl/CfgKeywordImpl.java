// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi.impl;

import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.KEYWORD_ELEMENT;

import com.demonwav.mcdev.platform.forge.cfg.psi.CfgKeyword;
import com.demonwav.mcdev.platform.forge.cfg.psi.CfgVisitor;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class CfgKeywordImpl extends ASTWrapperPsiElement implements CfgKeyword {

  public CfgKeywordImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull CfgVisitor visitor) {
    visitor.visitKeyword(this);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof CfgVisitor) accept((CfgVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getKeywordElement() {
    return findNotNullChildByType(KEYWORD_ELEMENT);
  }

}
