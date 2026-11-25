/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2026 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.minecraft.unpick

import com.demonwav.mcdev.framework.BASE_DATA_PATH
import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.framework.ProjectBuilderTest
import com.intellij.lang.annotation.HighlightSeverity
import java.io.File
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Checks that the test files of the reference implementation (https://github.com/Vineflower/unpick-parser) are
 * accepted or rejected in the same way by the parser and annotator.
 */
@ExtendWith(EdtInterceptor::class)
@DisplayName("Unpick Reference Fixtures Tests")
class UnpickReferenceFixturesTest : ProjectBuilderTest() {

    private val fixturesDir = File("$BASE_DATA_PATH/platform/minecraft/unpick/reference")

    private fun fixtures(kind: String) = fixturesDir.resolve(kind).listFiles { file -> file.extension == "unpick" }!!
        .sortedBy { it.name }
        .also { Assertions.assertTrue(it.isNotEmpty(), "No $kind fixtures found") }

    private fun errorsIn(file: File): List<String> {
        fixture.configureByText(file.name, file.readText())
        return fixture.doHighlighting(HighlightSeverity.ERROR).map { it.description }
    }

    @Test
    @DisplayName("Valid Reference Fixtures")
    fun validFixtures() {
        val failures = fixtures("valid").mapNotNull { file ->
            errorsIn(file).takeIf { it.isNotEmpty() }?.let { "${file.name}: $it" }
        }
        Assertions.assertEquals(emptyList<String>(), failures)
    }

    @Test
    @DisplayName("Invalid Reference Fixtures")
    fun invalidFixtures() {
        val failures = fixtures("invalid").mapNotNull { file ->
            val errors = errorsIn(file)
            val expected = EXPECTED_MESSAGES[file.name]
            when {
                errors.isEmpty() -> "${file.name}: no errors"
                expected != null && errors.none { it == expected } -> "${file.name}: expected '$expected' in $errors"
                else -> null
            }
        }
        Assertions.assertEquals(emptyList<String>(), failures)
    }

    private companion object {
        val EXPECTED_MESSAGES = mapOf(
            "other_missing_header.unpick" to "Missing version marker, expected 'unpick v4' on the first line",
            "whitespace_before_first_line.unpick" to
                "Missing version marker, expected 'unpick v4' on the first line",
            "whitespace_comment_on_first_line.unpick" to "The first line must be exactly 'unpick v3'",
            "syntax_target_annotation_unpick_v3.unpick" to "Target annotations require unpick v4",
            "syntax_group_definition_flag_default_group.unpick" to
                "The @flags attribute is not applicable to the default group",
            "syntax_group_definition_flag_invalid_data_type.unpick" to
                "The @flags attribute is only applicable to int and long groups",
            "syntax_group_definition_invalid_format.unpick" to "Unknown format 'foo'",
            "syntax_group_definition_multiple_flags.unpick" to "Duplicate @flags attribute",
            "syntax_group_definition_multiple_formats.unpick" to "Duplicate @format attribute",
            "syntax_group_definition_multiple_strict.unpick" to "Duplicate @strict attribute",
            "syntax_method_name_angled_brackets_incorrect.unpick" to "Expected 'init' or 'clinit'",
            "syntax_target_method_duplicate_param.unpick" to "Parameter 0 is targeted more than once",
            "syntax_target_method_duplicate_return.unpick" to "The return value is targeted more than once",
            "tokens_char_empty.unpick" to "Empty character literal",
            "tokens_char_multiple_chars.unpick" to "Too many characters in character literal",
            "tokens_char_unclosed.unpick" to "Unterminated character literal",
            "tokens_string_unclosed.unpick" to "Unterminated string literal",
            "tokens_double_missing_exp_value.unpick" to "Malformed number",
            "tokens_double_missing_exp_value_after_sign.unpick" to "Malformed number",
            "tokens_double_missing_frac_part.unpick" to "Malformed number",
            "tokens_double_out_of_bounds.unpick" to "Double out of bounds",
            "tokens_float_out_of_bounds.unpick" to "Float out of bounds",
            "tokens_integer_missing_value_binary.unpick" to "Malformed number",
            "tokens_integer_missing_value_hex.unpick" to "Malformed number",
            "tokens_integer_out_of_bounds_negative_binary.unpick" to "Integer out of bounds",
            "tokens_integer_out_of_bounds_negative_decimal.unpick" to "Integer out of bounds",
            "tokens_integer_out_of_bounds_negative_hex.unpick" to "Integer out of bounds",
            "tokens_integer_out_of_bounds_negative_octal.unpick" to "Integer out of bounds",
            "tokens_integer_out_of_bounds_positive_binary.unpick" to "Integer out of bounds",
            "tokens_integer_out_of_bounds_positive_decimal.unpick" to "Integer out of bounds",
            "tokens_integer_out_of_bounds_positive_hex.unpick" to "Integer out of bounds",
            "tokens_integer_out_of_bounds_positive_octal.unpick" to "Integer out of bounds",
            "tokens_field_descriptor_empty_object.unpick" to "Invalid field descriptor",
            "tokens_field_descriptor_invalid_char.unpick" to "Invalid field descriptor",
            "tokens_field_descriptor_object_illegal_bracket.unpick" to "Invalid field descriptor",
            "tokens_field_descriptor_object_illegal_period.unpick" to "Invalid field descriptor",
            "tokens_field_descriptor_unterminated_object.unpick" to "Invalid field descriptor",
            "tokens_field_descriptor_untyped_array.unpick" to "Invalid field descriptor",
            "tokens_field_descriptor_void.unpick" to "Invalid field descriptor",
            "tokens_method_descriptor_field_descriptor.unpick" to "Invalid method descriptor",
            "tokens_method_descriptor_no_return_type.unpick" to "Invalid method descriptor",
            "tokens_method_descriptor_unclosed_paren.unpick" to "Invalid method descriptor",
            "tokens_method_descriptor_void_parameter.unpick" to "Invalid method descriptor",
        )
    }
}
