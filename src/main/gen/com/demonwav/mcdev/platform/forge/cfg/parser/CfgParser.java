// This is a generated file. Not intended for manual editing.
package com.demonwav.mcdev.platform.forge.cfg.parser;

import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.ARGUMENT;
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
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.KEYWORD;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.KEYWORD_ELEMENT;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.NAME_ELEMENT;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.OPEN_PAREN;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.PRIMITIVE;
import static com.demonwav.mcdev.platform.forge.cfg.psi.CfgTypes.RETURN_VALUE;
import static com.intellij.lang.parser.GeneratedParserUtilBase.TRUE_CONDITION;
import static com.intellij.lang.parser.GeneratedParserUtilBase._COLLAPSE_;
import static com.intellij.lang.parser.GeneratedParserUtilBase._NONE_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.adapt_builder_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.consumeToken;
import static com.intellij.lang.parser.GeneratedParserUtilBase.consumeTokenFast;
import static com.intellij.lang.parser.GeneratedParserUtilBase.current_position_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.empty_element_parsed_guard_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.enter_section_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.exit_section_;
import static com.intellij.lang.parser.GeneratedParserUtilBase.nextTokenIsFast;
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
    if (t == ARGUMENT) {
      r = argument(b, 0);
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
    return cfg_file(b, l + 1);
  }

  /* ********************************************************** */
  // primitive | class_value
  public static boolean argument(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "argument")) return false;
    if (!nextTokenIsFast(b, CLASS_VALUE, PRIMITIVE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, ARGUMENT, "<argument>");
    r = consumeTokenFast(b, PRIMITIVE);
    if (!r) r = consumeTokenFast(b, CLASS_VALUE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

  /* ********************************************************** */
  // (entry? comment? crlf)*
  static boolean cfg_file(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cfg_file")) return false;
    int c = current_position_(b);
    while (true) {
      if (!cfg_file_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "cfg_file", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  // entry? comment? crlf
  private static boolean cfg_file_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cfg_file_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = cfg_file_0_0(b, l + 1);
    r = r && cfg_file_0_1(b, l + 1);
    r = r && consumeToken(b, CRLF);
    exit_section_(b, m, null, r);
    return r;
  }

  // entry?
  private static boolean cfg_file_0_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cfg_file_0_0")) return false;
    entry(b, l + 1);
    return true;
  }

  // comment?
  private static boolean cfg_file_0_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "cfg_file_0_1")) return false;
    consumeTokenFast(b, COMMENT);
    return true;
  }

  /* ********************************************************** */
  // class_name_element
  public static boolean class_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "class_name")) return false;
    if (!nextTokenIsFast(b, CLASS_NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, CLASS_NAME_ELEMENT);
    exit_section_(b, m, CLASS_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // keyword class_name (function|field_name|asterisk)?
  public static boolean entry(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "entry")) return false;
    if (!nextTokenIsFast(b, KEYWORD_ELEMENT)) return false;
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
    if (!r) r = consumeTokenFast(b, ASTERISK);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // name_element
  public static boolean field_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "field_name")) return false;
    if (!nextTokenIsFast(b, NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, NAME_ELEMENT);
    exit_section_(b, m, FIELD_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // name_element
  public static boolean func_name(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "func_name")) return false;
    if (!nextTokenIsFast(b, NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, NAME_ELEMENT);
    exit_section_(b, m, FUNC_NAME, r);
    return r;
  }

  /* ********************************************************** */
  // func_name open_paren argument* close_paren return_value
  public static boolean function(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function")) return false;
    if (!nextTokenIsFast(b, NAME_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = func_name(b, l + 1);
    r = r && consumeToken(b, OPEN_PAREN);
    r = r && function_2(b, l + 1);
    r = r && consumeToken(b, CLOSE_PAREN);
    r = r && return_value(b, l + 1);
    exit_section_(b, m, FUNCTION, r);
    return r;
  }

  // argument*
  private static boolean function_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "function_2")) return false;
    int c = current_position_(b);
    while (true) {
      if (!argument(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "function_2", c)) break;
      c = current_position_(b);
    }
    return true;
  }

  /* ********************************************************** */
  // keyword_element
  public static boolean keyword(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "keyword")) return false;
    if (!nextTokenIsFast(b, KEYWORD_ELEMENT)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeTokenFast(b, KEYWORD_ELEMENT);
    exit_section_(b, m, KEYWORD, r);
    return r;
  }

  /* ********************************************************** */
  // primitive | class_value
  public static boolean return_value(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "return_value")) return false;
    if (!nextTokenIsFast(b, CLASS_VALUE, PRIMITIVE)) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, RETURN_VALUE, "<return value>");
    r = consumeTokenFast(b, PRIMITIVE);
    if (!r) r = consumeTokenFast(b, CLASS_VALUE);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
