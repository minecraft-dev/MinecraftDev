// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.parser;

import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.ARGUMENTS;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.ASTERISK;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CLASS_NAME;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CLASS_NAME_ELEMENT;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CLASS_VALUE;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CLOSE_PAREN;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.COMMENT;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.CRLF;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.ENTRY;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.FIELD_NAME;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.FUNCTION;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.FUNC_NAME;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.INIT;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.KEYWORD;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.KEYWORD_ELEMENT;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.NAME_ELEMENT;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.OPEN_PAREN;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.RETURN_VALUE;
import static com.intellij.lang.parser.GeneratedParserUtilBase.TRUE_CONDITION;
import static com.intellij.lang.parser.GeneratedParserUtilBase._COLLAPSE_;
import static com.intellij.lang.parser.GeneratedParserUtilBase._NONE_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.adapt_builder_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken;
import static com.intellij.lang.parser.GeneratedParserUtilBase.current_position_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.empty_element_parsed_guard_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIs;
import static com.intellij.lang.parser.GeneratedParserUtilBase.recursion_guard_;

import com.intellij.lang.ASTNode;
import com.intellij.lang.LightPsiParser;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class CfgParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    if (t == ARGUMENTS) {
      r = arguments(b, 0);
    }
    else if (t == CLASS_NAME) {
      r = class_name(b, 0);
    }
    else if (t == ENTRY) {
      r = entry(b, 0);
    }
    else if (t == FIELD_NAME) {
      r = field_name(b, 0);
    }
    else if (t == FUNC_NAME) {
      r = func_name(b, 0);
    }
    else if (t == FUNCTION) {
      r = function(b, 0);
    }
    else if (t == KEYWORD) {
      r = keyword(b, 0);
    }
    else if (t == RETURN_VALUE) {
      r = return_value(b, 0);
    }
    else {
      r = parse_root_(t, b, 0);
    }
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return cfgFile(b, l + 1);
  }

  /* ********************************************************** */
  // class_value
  public static boolean arguments(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "arguments")) return false;
    if (!nextTokenIs(b, CLASS_VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CLASS_VALUE);
    exit_section_(b, m, ARGUMENTS, r);
    return r;
  }

  /* ********************************************************** */
  // item_*
  static boolean cfgFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cfgFile")) return false;
    int c = current_position_(b);
    while (true) {
      if (!item_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "cfgFile", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // class_name_element
  public static boolean class_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_name")) return false;
    if (!nextTokenIs(b, CLASS_NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CLASS_NAME_ELEMENT);
    exit_section_(b, m, CLASS_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // keyword class_name (function|field_name|asterisk)?
  public static boolean entry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry")) return false;
    if (!nextTokenIs(b, KEYWORD_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = keyword(b, l + 1);
    r = r && class_name(b, l + 1);
    r = r && entry_2(b, l + 1);
    exit_section_(b, m, ENTRY, r);
    return r;
  }

  // (function|field_name|asterisk)?
  private static boolean entry_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry_2")) return false;
    entry_2_0(b, l + 1);
    return true;
  }

  // function|field_name|asterisk
  private static boolean entry_2_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry_2_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = function(b, l + 1);
    if (!r) r = field_name(b, l + 1);
    if (!r) r = consumeToken(b, ASTERISK);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // name_element
  public static boolean field_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_name")) return false;
    if (!nextTokenIs(b, NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, NAME_ELEMENT);
    exit_section_(b, m, FIELD_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // name_element | init
  public static boolean func_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "func_name")) return false;
    if (!nextTokenIs(b, "<func name>", INIT, NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNC_NAME, "<func name>");
    r = consumeToken(b, NAME_ELEMENT);
    if (!r) r = consumeToken(b, INIT);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // func_name open_paren arguments? close_paren return_value
  public static boolean function(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function")) return false;
    if (!nextTokenIs(b, "<function>", INIT, NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, FUNCTION, "<function>");
    r = func_name(b, l + 1);
    r = r && consumeToken(b, OPEN_PAREN);
    r = r && function_2(b, l + 1);
    r = r && consumeToken(b, CLOSE_PAREN);
    r = r && return_value(b, l + 1);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  // arguments?
  private static boolean function_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_2")) return false;
    arguments(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // (entry comment?|comment|crlf)?
  static boolean item_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item_")) return false;
    item__0(b, l + 1);
    return true;
  }

  // entry comment?|comment|crlf
  private static boolean item__0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item__0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = item__0_0(b, l + 1);
    if (!r) r = consumeToken(b, COMMENT);
    if (!r) r = consumeToken(b, CRLF);
    exit_section_(b, m, null, r);
    return r;
  }

  // entry comment?
  private static boolean item__0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item__0_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = entry(b, l + 1);
    r = r && item__0_0_1(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // comment?
  private static boolean item__0_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "item__0_0_1")) return false;
    consumeToken(b, COMMENT);
    return true;
  }

  /* ********************************************************** */
  // keyword_element
  public static boolean keyword(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyword")) return false;
    if (!nextTokenIs(b, KEYWORD_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, KEYWORD_ELEMENT);
    exit_section_(b, m, KEYWORD, r);
    return r;
  }

  /* ********************************************************** */
  // class_value
  public static boolean return_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_value")) return false;
    if (!nextTokenIs(b, CLASS_VALUE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, CLASS_VALUE);
    exit_section_(b, m, RETURN_VALUE, r);
    return r;
  }

}
