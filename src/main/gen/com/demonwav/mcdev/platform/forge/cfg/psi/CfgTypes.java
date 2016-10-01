// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.psi;

import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgArgumentImpl;
import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgClassNameImpl;
import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgEntryImpl;
import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgFieldNameImpl;
import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgFuncNameImpl;
import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgFunctionImpl;
import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgKeywordImpl;
import com.demonwav.mcdev.platform.forge.cfg.psi.impl.CfgReturnValueImpl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;

public interface CfgTypes {

  IElementType ARGUMENT = new CfgElementType("ARGUMENT");
  IElementType CLASS_NAME = new CfgElementType("CLASS_NAME");
  IElementType ENTRY = new CfgElementType("ENTRY");
  IElementType FIELD_NAME = new CfgElementType("FIELD_NAME");
  IElementType FUNCTION = new CfgElementType("FUNCTION");
  IElementType FUNC_NAME = new CfgElementType("FUNC_NAME");
  IElementType KEYWORD = new CfgElementType("KEYWORD");
  IElementType RETURN_VALUE = new CfgElementType("RETURN_VALUE");

  IElementType ASTERISK = new CfgTokenType("*");
  IElementType CLASS_NAME_ELEMENT = new CfgTokenType("class_name_element");
  IElementType CLASS_VALUE = new CfgTokenType("class_value");
  IElementType CLOSE_PAREN = new CfgTokenType(")");
  IElementType COMMENT = new CfgTokenType("comment");
  IElementType CRLF = new CfgTokenType("crlf");
  IElementType KEYWORD_ELEMENT = new CfgTokenType("keyword_element");
  IElementType NAME_ELEMENT = new CfgTokenType("name_element");
  IElementType OPEN_PAREN = new CfgTokenType("(");
  IElementType PRIMITIVE = new CfgTokenType("primitive");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
       if (type == ARGUMENT) {
        return new CfgArgumentImpl(node);
      }
      else if (type == CLASS_NAME) {
        return new CfgClassNameImpl(node);
      }
      else if (type == ENTRY) {
        return new CfgEntryImpl(node);
      }
      else if (type == FIELD_NAME) {
        return new CfgFieldNameImpl(node);
      }
      else if (type == FUNCTION) {
        return new CfgFunctionImpl(node);
      }
      else if (type == FUNC_NAME) {
        return new CfgFuncNameImpl(node);
      }
      else if (type == KEYWORD) {
        return new CfgKeywordImpl(node);
      }
      else if (type == RETURN_VALUE) {
        return new CfgReturnValueImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
