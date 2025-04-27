/*******************************************************************************
 * Copyright (c)  Sebastien Moran
 * https://github.com/sebz
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/
/*Code from: https://github.com/sebz/eclipse-javascript-formatter*/
package com.eco.bio7.ijmacro.editor.beautifier;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class JSBeautifier {

	private JSBeautifierOptions opt;

	private static enum TK {
		EOF, // TK_EOF
		START_EXPR, // TK_START_EXPR
		END_EXPR, // TK_END_EXPR
		START_BLOCK, // TK_START_BLOCK
		END_BLOCK, // TK_END_BLOCK
		WORD, // TK_WORD
		SEMICOLON, // TK_SEMICOLON
		STRING, // TK_STRING
		DOT, // TK_DOT
		EQUALS, // TK_EQUALS
		COMMA, // TK_COMMA
		OPERATOR, // TK_OPERATOR
		BLOCK_COMMENT, // TK_BLOCK_COMMENT
		INLINE_COMMENT, // TK_INLINE_COMMENT
		COMMENT, // TK_COMMENT
		UNKNOWN, // TK_UNKNOWN
	}

	private class Token {
		public String token_text;
		public TK token_type;

		public Token(String text, TK type) {
			token_text = text;
			token_type = type;
		}

		public Token(char c, TK type) {
			token_text = "" + c;
			token_type = type;
		}
	}

	private static enum Mode {
		BLOCK, // BLOCK
		B_EXPRESSION, // [EXPRESSION]
		P_EXPRESSION, // (EXPRESSION)
		FOR_EXPRESSION, // (FOR-EXPRESSION)
		COND_EXPRESSION, // (COND-EXPRESSION)
		INDENTED_EXPRESSION, // [INDENTED-EXPRESSION]
		DO_BLOCK, // DO-BLOCK
		OBJECT // OBJECT
	}

	private class Flags {
		Mode previous_mode = null;
		Mode mode = null;
		boolean var_line = false;
		boolean var_line_tainted = false;
		boolean var_line_reindented = false;
		boolean in_html_comment = false;
		boolean if_line = false;
		int chain_extra_indentation = 0;
		boolean in_case_statement = false; // switch(..){ INSIDE HERE }
		boolean in_case = false; // we're on the exact line with "case 0:"
		boolean case_body = false; // the indented case-action block
		boolean eat_next_space = false;
		int indentation_level = 0;
		int ternary_depth = 0;
	}

	private static final int NONE = 0;
	private static final int NEWLINE = 1;
	private static final int SPACE = 2;

	private static final String whitespace = "\n\r\t ";
	private static final String wordchar = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$";
	private static final String[] punct = { "+", "-", "*", "/", "%", "&", "++",
			"--", "=", "+=", "-=", "*=", "/=", "%=", "==", "===", "!=", "!==",
			">", "<", ">=", "<=", ">>", "<<", ">>>", ">>>=", ">>=", "<<=",
			"&&", "&=", "|", "||", "!", "!!", ",", ":", "?", "^", "^=", "|=",
			"::", "<%=", "<%", "%>", "<?=", "<?", "?>" };
	private static final String[] line_starters = { "continue", "try", "throw",
			"return", "var", "if", "switch", "case", "default", "for", "while",
			"break", "function" };
	private static final String digits = "0123456789";

	private String input;
	private Stack<String> output;
	private String token_text;
	private TK token_type;
	private TK last_type;
	private String last_text;
	private String last_last_text;
	private String last_word;
	private Flags flags;
	private Stack<Flags> flag_store;
	private String indent_string;
	private String preindent_string;
	private int parser_pos;
	private int prefix;
	private boolean do_block_just_closed;
	private boolean wanted_newline;
	private boolean just_added_newline;
	private int n_newlines;
	private int input_length;

	private void trim_output() {
		trim_output(false);
	}

	private void trim_output(boolean eat_newlines) {
		while (!output.isEmpty()
				&& (output.peek().equals(" ")
						|| output.peek().equals(indent_string)
						|| output.peek().equals(preindent_string) || (eat_newlines && (output
						.peek().equals("\n") || output.peek().equals("\r"))))) {
			output.pop();
		}
	}

	private String[] split_newlines(String s) {
		return s.split("\\x0d\\x0a|\\x0a");
	}

	@SuppressWarnings("unused")
	private void force_newline() {
		boolean old_keep_array_indentation = opt.keep_array_indentation;
		opt.keep_array_indentation = false;
		print_newline();
		opt.keep_array_indentation = old_keep_array_indentation;
	}

	private void print_newline() {
		print_newline(true, true);
	}

	private void print_newline(boolean ignore_repeated) {
		print_newline(ignore_repeated, true);
	}

	private void print_newline(boolean ignore_repeated,
			boolean reset_statement_flags) {
		flags.eat_next_space = false;
		if (opt.keep_array_indentation && is_array(flags.mode)) {
			return;
		}

		if (reset_statement_flags) {
			flags.if_line = false;
			flags.chain_extra_indentation = 0;
		}

		trim_output();

		if (output.isEmpty()) {
			return; // no newline on start of file
		}

		if (!output.peek().equals("\n") || !ignore_repeated) {
			just_added_newline = true;
			output.push("\n");
		}
		if (!"".equals(preindent_string)) {
			output.push(preindent_string);
		}
		for (int i = 0; i < flags.indentation_level
				+ flags.chain_extra_indentation; i++) {
			print_indent_string();
		}
		if (flags.var_line && flags.var_line_reindented) {
			print_indent_string(); // skip space-stuffing, if indenting with a
									// tab
		}
	}

	private void print_indent_string() {
		// Never indent your first output indent at the start of the file
		if (!"".equals(last_text)) {
			output.push(indent_string);
		}
	}

	private void print_single_space() {

		String last_output = " ";

		if (flags.eat_next_space) {
			flags.eat_next_space = false;
		} else if (last_type == TK.COMMENT) {
			print_newline();
		} else {
			if (!output.isEmpty()) {
				last_output = output.peek();
			}
			if (!last_output.equals(" ") && !last_output.equals("\n")
					&& !last_output.equals(indent_string)) {
				// prevent occassional duplicate space
				output.push(" ");
			}
		}
	}

	private void print_token() {
		just_added_newline = false;
		flags.eat_next_space = false;
		output.push(token_text);
	}

	private void indent() {
		flags.indentation_level++;
	}

	private void remove_indent() {
		if (!output.isEmpty() && output.peek().equals(indent_string)) {
			output.pop();
		}
	}

	private void set_mode(Mode mode) {
		Mode previous_mode = Mode.BLOCK;
		int indentation_level = 0;
		if (flags != null) {
			flag_store.push(flags);
			previous_mode = flags.mode;
			indentation_level = flags.indentation_level
					+ ((flags.var_line && flags.var_line_reindented) ? 1 : 0);
		}
		flags = new Flags();
		flags.mode = mode;
		flags.previous_mode = previous_mode;
		flags.indentation_level = indentation_level;
	}

	private boolean is_array(Mode mode) {
		return mode == Mode.B_EXPRESSION || mode == Mode.INDENTED_EXPRESSION;
	}

	private boolean is_expression(Mode mode) {
		return mode == Mode.B_EXPRESSION || mode == Mode.P_EXPRESSION
				|| mode == Mode.FOR_EXPRESSION || mode == Mode.COND_EXPRESSION;
	}

	private void restore_mode() {
		do_block_just_closed = flags.mode == Mode.DO_BLOCK;
		if (flag_store.size() > 0) {
			Mode mode = flags.mode;
			flags = flag_store.pop();
			flags.previous_mode = mode;
		}
	}

	private boolean all_lines_start_with(String[] lines, char c) {
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.length() > 0 && line.charAt(0) != c) {
				return false;
			}
		}
		return true;
	}

	private boolean is_special_word(String word) {
		return in_array(word, new String[] { "case", "return", "do", "if",
				"throw", "else" });
	}

	private boolean in_array(char c, String... array) {
		return in_array((Object) ((String) "" + c), (Object[]) array);
	}

	private boolean in_array(String item, String... array) {
		return in_array((Object) item, (Object[]) array);
	}

	private boolean in_array(Object item, Object... array) {
		for (Object element : array) {
			if (element.equals(item)) {
				return true;
			}
		}
		return false;
	}

	private String unescape_string(String s) {
		boolean esc = false;
		String out = "";
		int pos = 0;
		String s_hex = "";
		int escaped = 0;
		char c;

		while (esc || pos < s.length()) {

			c = s.charAt(pos);
			pos++;

			if (esc) {
				esc = false;
				if (c == 'x') {
					// simple hex-escape \x24
					s_hex = s.substring(pos, pos + 2);
					pos += 2;
				} else if (c == 'u') {
					// unicode-escape, \u2134
					s_hex = s.substring(pos, pos + 4);
					pos += 4;
				} else {
					// some common escape, e.g \n
					out += '\\' + c;
					continue;
				}
				if (!s_hex.matches("^[0123456789abcdefABCDEF]+$")) {
					// some weird escaping, bail out,
					// leaving whole string intact
					return s;
				}

				escaped = Integer.parseInt(s_hex, 16);

				if (escaped >= 0x00 && escaped < 0x20) {
					// leave 0x00...0x1f escaped
					if (c == 'x') {
						out += "\\x" + s_hex;
					} else {
						out += "\\u" + s_hex;
					}
					continue;
				} else if (escaped == 0x22 || escaped == 0x27
						|| escaped == 0x5c) {
					// single-quote, apostrophe, backslash - escape these
					out += '\\' + (char) escaped; // String.fromCharCode(escaped);
				} else if (c == 'x' && escaped > 0x7e && escaped <= 0xff) {
					// we bail out on \x7f..\xff,
					// leaving whole string escaped,
					// as it's probably completely binary
					return s;
				} else {
					out += (char) escaped; // String.fromCharCode(escaped);
				}
			} else if (c == '\\') {
				esc = true;
			} else {
				out += c;
			}
		}
		return out;
	}

	private char look_up(char exclude) {
		if (parser_pos >= input_length) {
			return 0;
		}
		int local_pos = parser_pos;
		char c = input.charAt(local_pos);
		while (whitespace.indexOf(c) >= 0 && c != exclude) {
			local_pos++;
			if (local_pos >= input_length) {
				return 0;
			}
			c = input.charAt(local_pos);
		}
		return c;
	}

	private Token get_next_token() {
		int i;

		n_newlines = 0;

		if (parser_pos >= input_length) {
			return new Token("", TK.EOF);
		}

		wanted_newline = false;

		char c = input.charAt(parser_pos);
		parser_pos++;

		boolean keep_whitespace = opt.keep_array_indentation
				&& is_array(flags.mode);

		if (keep_whitespace) {

			while (whitespace.indexOf(c) >= 0) {

				if (c == '\n') {
					trim_output();
					output.push("\n");
					just_added_newline = true;
				} else {
					if (just_added_newline) {
						// if (c == indent_string.charAt(0)) {
						// output.push(indent_string);
						// } else {
						if (c != '\r') {
							output.push(" ");
						}
						// }
					}
				}

				if (parser_pos >= input_length) {
					return new Token("", TK.EOF);
				}

				c = input.charAt(parser_pos);
				parser_pos++;

			}

		} else {

			while (whitespace.indexOf(c) >= 0) {

				if (c == '\n') {
					if (opt.max_preserve_newlines > 0) {
						if (n_newlines <= opt.max_preserve_newlines) {
							n_newlines++;
						}
					} else {
						n_newlines++;
					}
				}

				if (parser_pos >= input_length) {
					return new Token("", TK.EOF);
				}

				c = input.charAt(parser_pos);
				parser_pos++;

			}

			if (opt.preserve_newlines) {
				if (n_newlines > 1) {
					for (i = 0; i < n_newlines; i++) {
						print_newline(i == 0);
						just_added_newline = true;
					}
				}
			}
			wanted_newline = n_newlines > 0;
		}

		if (wordchar.indexOf(c) >= 0) {
			String s = "" + c;
			if (parser_pos < input_length) {
				while (wordchar.indexOf(input.charAt(parser_pos)) >= 0) {
					s += input.charAt(parser_pos);
					parser_pos++;
					if (parser_pos == input_length) {
						break;
					}
				}
			}

			// small and surprisingly unugly hack for 1E-10 representation
			if (parser_pos != input_length
					&& s.matches("^[0-9]+[Ee]$")
					&& (input.charAt(parser_pos) == '-' || input
							.charAt(parser_pos) == '+')) {

				char sign = input.charAt(parser_pos);
				parser_pos++;

				Token t = get_next_token();
				s += "" + sign + t.token_text;
				return new Token(s, TK.WORD);
			}

			if (s.equals("in")) { // hack for 'in' operator
				return new Token(s, TK.OPERATOR);
			}
			if (wanted_newline && last_type != TK.OPERATOR
					&& last_type != TK.EQUALS && !flags.if_line
					&& (opt.preserve_newlines || !last_text.equals("var"))) {
				print_newline();
			}
			return new Token(s, TK.WORD);
		}

		if (c == '(' || c == '[') {
			return new Token(c, TK.START_EXPR);
		}

		if (c == ')' || c == ']') {
			return new Token(c, TK.END_EXPR);
		}

		if (c == '{') {
			return new Token(c, TK.START_BLOCK);
		}

		if (c == '}') {
			return new Token(c, TK.END_BLOCK);
		}

		if (c == ';') {
			return new Token(c, TK.SEMICOLON);
		}

		if (c == '/') {
			String comment = "";
			// peek for comment /* ... */
			boolean inline_comment = true;
			if (input.charAt(parser_pos) == '*') {
				parser_pos++;
				if (parser_pos < input_length) {
					while (parser_pos < input_length
							&& !(input.charAt(parser_pos) == '*'
									&& parser_pos + 1 < input_length && input
									.charAt(parser_pos + 1) == '/')) {
						c = input.charAt(parser_pos);
						comment += c;
						if (c == '\n' || c == '\r') {
							inline_comment = false;
						}
						parser_pos++;
						if (parser_pos >= input_length) {
							break;
						}
					}
				}
				parser_pos += 2;
				if (inline_comment && n_newlines == 0) {
					return new Token("/*" + comment + "*/", TK.INLINE_COMMENT);
				} else {
					return new Token("/*" + comment + "*/", TK.BLOCK_COMMENT);
				}
			}
			// peek for comment // ...
			if (input.charAt(parser_pos) == '/') {
				comment = "" + c;
				while (input.charAt(parser_pos) != '\r'
						&& input.charAt(parser_pos) != '\n') {
					comment += input.charAt(parser_pos);
					parser_pos++;
					if (parser_pos >= input_length) {
						break;
					}
				}
				if (wanted_newline) {
					print_newline();
				}
				return new Token(comment, TK.COMMENT);
			}

		}

		if (c == '\''
				|| c == '"'
				|| (c == '/' && ((last_type == TK.WORD && is_special_word(last_text))
						|| (last_text.equals(")") && in_array(
								flags.previous_mode, new Object[] {
										Mode.COND_EXPRESSION,
										Mode.FOR_EXPRESSION })) || (last_type == TK.COMMA
						|| last_type == TK.COMMENT
						|| last_type == TK.START_EXPR
						|| last_type == TK.START_BLOCK
						|| last_type == TK.END_BLOCK
						|| last_type == TK.OPERATOR
						|| last_type == TK.EQUALS
						|| last_type == TK.EOF || last_type == TK.SEMICOLON)))) { // regexp
			char sep = c;
			boolean esc = false;
			boolean has_char_escapes = false;
			String resulting_string = "" + c;

			if (parser_pos < input_length) {
				if (sep == '/') {
					//
					// handle regexp separately...
					//
					boolean in_char_class = false;
					while (esc || in_char_class
							|| input.charAt(parser_pos) != sep) {
						resulting_string += input.charAt(parser_pos);
						if (!esc) {
							esc = input.charAt(parser_pos) == '\\';
							if (input.charAt(parser_pos) == '[') {
								in_char_class = true;
							} else if (input.charAt(parser_pos) == ']') {
								in_char_class = false;
							}
						} else {
							esc = false;
						}
						parser_pos++;
						if (parser_pos >= input_length) {
							// incomplete string/rexp when end-of-file reached.
							// bail out with what had been received so far.
							return new Token(resulting_string, TK.STRING);
						}
					}

				} else {
					//
					// and handle string also separately
					//
					while (esc || input.charAt(parser_pos) != sep) {
						resulting_string += input.charAt(parser_pos);
						if (esc) {
							if (input.charAt(parser_pos) == 'x'
									|| input.charAt(parser_pos) == 'u') {
								has_char_escapes = true;
							}
							esc = false;
						} else {
							esc = input.charAt(parser_pos) == '\\';
						}
						parser_pos++;
						if (parser_pos >= input_length) {
							// incomplete string/rexp when end-of-file reached.
							// bail out with what had been received so far.
							return new Token(resulting_string, TK.STRING);
						}
					}

				}
			}

			parser_pos++;
			resulting_string += sep;

			if (has_char_escapes && opt.unescape_strings) {
				resulting_string = unescape_string(resulting_string);
			}

			if (sep == '/') {
				// regexps may have modifiers /regexp/MOD , so fetch those, too
				while (parser_pos < input_length
						&& wordchar.indexOf(input.charAt(parser_pos)) >= 0) {
					resulting_string += input.charAt(parser_pos);
					parser_pos++;
				}
			}
			return new Token(resulting_string, TK.STRING);
		}

		if (c == '#') {

			if (output.isEmpty() && parser_pos < input_length
					&& input.charAt(parser_pos) == '!') {
				// shebang
				String resulting_string = "" + c;
				while (parser_pos < input_length && c != '\n') {
					c = input.charAt(parser_pos);
					resulting_string += c;
					parser_pos++;
				}
				output.push(resulting_string.trim() + "\n");
				print_newline();
				return get_next_token();
			}

			// Spidermonkey-specific sharp variables for circular references
			// https://developer.mozilla.org/En/Sharp_variables_in_JavaScript
			// http://mxr.mozilla.org/mozilla-central/source/js/src/jsscan.cpp
			// around line 1935
			String sharp = "#";
			if (parser_pos < input_length
					&& digits.indexOf(input.charAt(parser_pos)) >= 0) {
				do {
					c = input.charAt(parser_pos);
					sharp += c;
					parser_pos++;
				} while (parser_pos < input_length && c != '#' && c != '=');
				if (c == '#') {
					//
				} else if (parser_pos + 1 < input_length
						&& input.charAt(parser_pos) == '['
						&& input.charAt(parser_pos + 1) == ']') {
					sharp += "[]";
					parser_pos += 2;
				} else if (parser_pos + 1 < input_length
						&& input.charAt(parser_pos) == '{'
						&& input.charAt(parser_pos + 1) == '}') {
					sharp += "{}";
					parser_pos += 2;
				}
				return new Token(sharp, TK.WORD);
			}
		}

		if (c == '<'
				&& parser_pos + 3 <= input_length
				&& input.substring(parser_pos - 1, parser_pos + 3).equals(
						"<!--")) {
			parser_pos += 3;
			String resulting_string = "<!--";
			while (parser_pos < input_length
					&& input.charAt(parser_pos) != '\n') {
				resulting_string += input.charAt(parser_pos);
				parser_pos++;
			}
			flags.in_html_comment = true;
			return new Token(resulting_string, TK.COMMENT);
		}

		if (c == '-'
				&& parser_pos + 2 <= input_length
				&& flags.in_html_comment
				&& input.substring(parser_pos - 1, parser_pos + 2)
						.equals("-->")) {
			flags.in_html_comment = false;
			parser_pos += 2;
			if (wanted_newline) {
				print_newline();
			}
			return new Token("-->", TK.COMMENT);
		}

		if (c == '.') {
			return new Token(c, TK.DOT);
		}

		if (in_array(c, punct)) {
			String resulting_string = "" + c;
			while (parser_pos < input_length
					&& in_array(resulting_string + input.charAt(parser_pos),
							punct)) {
				c = input.charAt(parser_pos);
				resulting_string += c;
				parser_pos++;
			}

			if (resulting_string.equals(",")) {
				return new Token(resulting_string, TK.COMMA);
			} else if (resulting_string.equals("=")) {
				return new Token(resulting_string, TK.EQUALS);
			} else {
				return new Token(resulting_string, TK.OPERATOR);
			}
		}

		return new Token(c, TK.UNKNOWN);
	}

	public String js_beautify(String js_source_text, JSBeautifierOptions options) {

		opt = options == null ? new JSBeautifierOptions() : options;

		if (opt.indent_char.equals("\t")) {
			indent_string = opt.indent_char;
		} else {
			char[] cb = new char[opt.indent_size];
			Arrays.fill(cb, opt.indent_char.charAt(0));
			indent_string = new String(cb);
		}
		preindent_string = "";

		while (js_source_text.length() > 0
				&& (js_source_text.charAt(0) == ' ' || js_source_text.charAt(0) == '\t')) {
			preindent_string += js_source_text.charAt(0);
			js_source_text = js_source_text.substring(1);
		}
		input = js_source_text;
		input_length = js_source_text.length();

		last_word = ""; // last TK_WORD passed
		last_type = TK.START_EXPR; // last token type
		last_text = ""; // last token text
		last_last_text = ""; // pre-last token text
		output = new Stack<String>();

		do_block_just_closed = false;
		just_added_newline = false;

		// words which should always start on new line.

		// states showing if we are currently in expression (i.e. "if" case) -
		// 'EXPRESSION', or in usual block (like, procedure), 'BLOCK'.
		// some formatting depends on that.

		flag_store = new Stack<Flags>();
		flags = null;
		set_mode(Mode.BLOCK); // create initial flags

		parser_pos = 0;
		while (true) {
			Token t = get_next_token();
			token_text = t.token_text;
			token_type = t.token_type;
			if (token_type == TK.EOF) {
				break;
			}

			switch (token_type) {

			case START_EXPR:

				if (token_text.equals("[")) {

					if (last_type == TK.WORD || last_text.equals(")")) {
						// this is array index specifier, break immediately
						// a[x], fn()[x]
						if (in_array(last_text, line_starters)) {
							print_single_space();
						}
						set_mode(Mode.P_EXPRESSION);
						print_token();
						break;
					}

					if (flags.mode == Mode.B_EXPRESSION
							|| flags.mode == Mode.INDENTED_EXPRESSION) {
						if (last_last_text.equals("]") && last_text.equals(",")) {
							// ], [ goes to new line
							if (flags.mode == Mode.B_EXPRESSION) {
								flags.mode = Mode.INDENTED_EXPRESSION;
								if (!opt.keep_array_indentation) {
									indent();
								}
							}
							set_mode(Mode.B_EXPRESSION);
							if (!opt.keep_array_indentation) {
								print_newline();
							}
						} else if (last_text.equals("[")) {
							if (flags.mode == Mode.B_EXPRESSION) {
								flags.mode = Mode.INDENTED_EXPRESSION;
								if (!opt.keep_array_indentation) {
									indent();
								}
							}
							set_mode(Mode.B_EXPRESSION);

							if (!opt.keep_array_indentation) {
								print_newline();
							}
						} else {
							set_mode(Mode.B_EXPRESSION);
						}
					} else {
						set_mode(Mode.B_EXPRESSION);
					}

				} else {
					if (last_word.equals("for")) {
						set_mode(Mode.FOR_EXPRESSION);
					} else if (in_array(last_word,
							new String[] { "if", "while" })) {
						set_mode(Mode.COND_EXPRESSION);
					} else {
						set_mode(Mode.P_EXPRESSION);
					}
				}

				if (last_text.equals(";") || last_type == TK.START_BLOCK) {
					print_newline();
				} else if (last_type == TK.END_EXPR
						|| last_type == TK.START_EXPR
						|| last_type == TK.END_BLOCK || last_text.equals(".")) {
					if (wanted_newline) {
						print_newline();
					}
					// do nothing on (( and )( and ][ and ]( and .(
				} else if (last_type != TK.WORD && last_type != TK.OPERATOR) {
					print_single_space();
				} else if (last_word.equals("function")
						|| last_word.equals("typeof")) {
					// function() vs function ()
					if (opt.jslint_happy) {
						print_single_space();
					}
				} else if (in_array(last_text, line_starters)
						|| last_text.equals("catch")) {
					if (opt.space_before_conditional) {
						print_single_space();
					}
				}
				print_token();

				break;

			case DOT:

				if (is_special_word(last_text)) {
					print_single_space();
				} else if (last_text.equals(")")) {
					if (opt.break_chained_methods || wanted_newline) {
						flags.chain_extra_indentation = 1;
						print_newline(true /* ignore_repeated */, false /* reset_statement_flags */);
					}
				}

				print_token();
				break;

			case END_EXPR:
				if (token_text.equals("]")) {
					if (opt.keep_array_indentation) {
						if (last_text.equals("}")) {
							// trim_output();
							// print_newline(true);
							remove_indent();
							print_token();
							restore_mode();
							break;
						}
					} else {
						if (flags.mode == Mode.INDENTED_EXPRESSION) {
							if (last_text.equals("]")) {
								restore_mode();
								print_newline();
								print_token();
								break;
							}
						}
					}
				}
				restore_mode();
				print_token();
				break;

			case START_BLOCK:

				if (last_word.equals("do")) {
					set_mode(Mode.DO_BLOCK);
				} else {
					set_mode(Mode.BLOCK);
				}
				if (opt.brace_style == JSBeautifierOptions.BS_EXPAND
						|| opt.brace_style == JSBeautifierOptions.BS_EXPAND_STRICT) {
					boolean empty_braces = false;
					if (opt.brace_style == JSBeautifierOptions.BS_EXPAND_STRICT) {
						empty_braces = (look_up((char) 0) == '}');
						if (!empty_braces) {
							print_newline(true);
						}
					} else {
						if (last_type != TK.OPERATOR) {
							if (last_type == TK.EQUALS
									|| (is_special_word(last_text) && !last_text
											.equals("else"))) {
								print_single_space();
							} else {
								print_newline(true);
							}
						}
					}
					print_token();
					if (!empty_braces) {
						indent();
					}
				} else {
					if (last_type != TK.OPERATOR && last_type != TK.START_EXPR) {
						if (last_type == TK.START_BLOCK) {
							print_newline();
						} else {
							print_single_space();
						}
					} else {
						// if TK_OPERATOR or TK_START_EXPR
						if (is_array(flags.previous_mode)
								&& last_text.equals(",")) {
							if (last_last_text.equals("}")) {
								// }, { in array context
								print_single_space();
							} else {
								print_newline(); // [a, b, c, {
							}
						}
					}
					indent();
					print_token();
				}

				break;

			case END_BLOCK:
				restore_mode();
				if (opt.brace_style == JSBeautifierOptions.BS_EXPAND
						|| opt.brace_style == JSBeautifierOptions.BS_EXPAND_STRICT) {
					if (!last_text.equals("{")) {
						print_newline();
					}
					print_token();
				} else {
					if (last_type == TK.START_BLOCK) {
						// nothing
						if (just_added_newline) {
							remove_indent();
						} else {
							// {}
							trim_output();
						}
					} else {
						if (is_array(flags.mode) && opt.keep_array_indentation) {
							// we REALLY need a newline here, but newliner would
							// skip that
							opt.keep_array_indentation = false;
							print_newline();
							opt.keep_array_indentation = true;

						} else {
							print_newline();
						}
					}
					print_token();
				}
				break;

			case WORD:

				// no, it's not you. even I have problems understanding how this
				// works
				// and what does what.
				if (do_block_just_closed) {
					// do {} ## while ()
					print_single_space();
					print_token();
					print_single_space();
					do_block_just_closed = false;
					break;
				}

				prefix = NONE;

				if (token_text.equals("function")) {
					if (flags.var_line && last_type != TK.EQUALS) {
						flags.var_line_reindented = true;
					}
					if ((just_added_newline || last_text.equals(";"))
							&& !last_text.equals("{")
							&& last_type != TK.BLOCK_COMMENT
							&& last_type != TK.COMMENT) {
						// make sure there is a nice clean space of at least one
						// blank line
						// before a new function definition
						n_newlines = just_added_newline ? n_newlines : 0;
						if (!opt.preserve_newlines) {
							n_newlines = 1;
						}

						for (int i = 0; i < 2 - n_newlines; i++) {
							print_newline(false);
						}
					}
					if (last_type == TK.WORD) {
						if (last_text.equals("get") || last_text.equals("set")
								|| last_text.equals("new")
								|| last_text.equals("return")) {
							print_single_space();
						} else {
							print_newline();
						}
					} else if (last_type == TK.OPERATOR
							|| last_text.equals("=")) {
						// foo = function
						print_single_space();
					} else if (is_expression(flags.mode)) {
						// print nothing
					} else {
						print_newline();
					}

					print_token();
					last_word = token_text;
					break;
				}

				if (token_text.equals("case")
						|| (token_text.equals("default") && flags.in_case_statement)) {
					print_newline();
					if (flags.case_body) {
						// switch cases following one another
						flags.indentation_level--;
						flags.case_body = false;
						remove_indent();
					}
					print_token();
					flags.in_case = true;
					flags.in_case_statement = true;
					break;
				}

				if (last_type == TK.END_BLOCK) {

					if (!in_array(token_text.toLowerCase(), new String[] {
							"else", "catch", "finally" })) {
						prefix = NEWLINE;
					} else {
						if (opt.brace_style == JSBeautifierOptions.BS_EXPAND
								|| opt.brace_style == JSBeautifierOptions.BS_END_EXPAND
								|| opt.brace_style == JSBeautifierOptions.BS_EXPAND_STRICT) {
							prefix = NEWLINE;
						} else {
							prefix = SPACE;
							print_single_space();
						}
					}
				} else if (last_type == TK.SEMICOLON
						&& (flags.mode == Mode.BLOCK || flags.mode == Mode.DO_BLOCK)) {
					prefix = NEWLINE;
				} else if (last_type == TK.SEMICOLON
						&& is_expression(flags.mode)) {
					prefix = SPACE;
				} else if (last_type == TK.STRING) {
					prefix = NEWLINE;
				} else if (last_type == TK.WORD) {
					if (last_text.equals("else")) {
						// eat newlines between ...else *** some_op...
						// won't preserve extra newlines in this place (if any),
						// but don't care that much
						trim_output(true);
					}
					prefix = SPACE;
				} else if (last_type == TK.START_BLOCK) {
					prefix = NEWLINE;
				} else if (last_type == TK.END_EXPR) {
					print_single_space();
					prefix = NEWLINE;
				}

				if (in_array(token_text, line_starters)
						&& !last_text.equals(")")) {
					if (last_text.equals("else")) {
						prefix = SPACE;
					} else {
						prefix = NEWLINE;
					}

				}

				if (flags.if_line && last_type == TK.END_EXPR) {
					flags.if_line = false;
				}
				if (in_array(token_text.toLowerCase(), new String[] { "else",
						"catch", "finally" })) {
					if (last_type != TK.END_BLOCK
							|| opt.brace_style == JSBeautifierOptions.BS_EXPAND
							|| opt.brace_style == JSBeautifierOptions.BS_END_EXPAND
							|| opt.brace_style == JSBeautifierOptions.BS_EXPAND_STRICT) {
						print_newline();
					} else {
						trim_output(true);
						print_single_space();
					}
				} else if (prefix == NEWLINE) {
					if (is_special_word(last_text)) {
						// no newline between 'return nnn'
						print_single_space();
					} else if (last_type != TK.END_EXPR) {
						if ((last_type != TK.START_EXPR || !token_text
								.equals("var")) && !last_text.equals(":")) {
							// no need to force newline on 'var': for (var x =
							// 0...)
							if (token_text.equals("if")
									&& last_word.equals("else")
									&& !last_text.equals("{")) {
								// no newline for } else if {
								print_single_space();
							} else {
								flags.var_line = false;
								flags.var_line_reindented = false;
								print_newline();
							}
						}
					} else if (in_array(token_text, line_starters)
							&& !last_text.equals(")")) {
						flags.var_line = false;
						flags.var_line_reindented = false;
						print_newline();
					}
				} else if (is_array(flags.mode) && last_text.equals(",")
						&& last_last_text.equals("}")) {
					print_newline(); // }, in lists get a newline treatment
				} else if (prefix == SPACE) {
					print_single_space();
				}
				print_token();
				last_word = token_text;

				if (token_text.equals("var")) {
					flags.var_line = true;
					flags.var_line_reindented = false;
					flags.var_line_tainted = false;
				}

				if (token_text.equals("if")) {
					flags.if_line = true;
				}
				if (token_text.equals("else")) {
					flags.if_line = false;
				}

				break;

			case SEMICOLON:

				print_token();
				flags.var_line = false;
				flags.var_line_reindented = false;
				if (flags.mode == Mode.OBJECT) {
					// OBJECT mode is weird and doesn't get reset too well.
					flags.mode = Mode.BLOCK;
				}
				break;

			case STRING:

				if (last_type == TK.END_EXPR
						&& in_array(flags.previous_mode, new Object[] {
								Mode.COND_EXPRESSION, Mode.FOR_EXPRESSION })) {
					print_single_space();
				} else if (last_type == TK.WORD) {
					print_single_space();
				} else if (last_type == TK.COMMA || last_type == TK.START_EXPR
						|| last_type == TK.EQUALS || last_type == TK.OPERATOR) {
					if (opt.preserve_newlines && wanted_newline
							&& flags.mode != Mode.OBJECT) {
						print_newline();
						print_indent_string();
					}
				} else {
					print_newline();
				}
				print_token();
				break;

			case EQUALS:
				if (flags.var_line) {
					// just got an '=' in a var-line, different
					// formatting/line-breaking, etc will now be done
					flags.var_line_tainted = true;
				}
				print_single_space();
				print_token();
				print_single_space();
				break;

			case COMMA:
				if (flags.var_line) {
					if (is_expression(flags.mode) || last_type == TK.END_BLOCK) {
						// do not break on comma, for(var a = 1, b = 2)
						flags.var_line_tainted = false;
					}
					if (flags.var_line_tainted) {
						print_token();
						flags.var_line_reindented = true;
						flags.var_line_tainted = false;
						print_newline();
						break;
					} else {
						flags.var_line_tainted = false;
					}

					print_token();
					print_single_space();
					break;
				}

				if (last_type == TK.COMMENT) {
					print_newline();
				}

				if (last_type == TK.END_BLOCK
						&& flags.mode != Mode.P_EXPRESSION) {
					print_token();
					if (flags.mode == Mode.OBJECT && last_text.equals("}")) {
						print_newline();
					} else {
						print_single_space();
					}
				} else {
					if (flags.mode == Mode.OBJECT) {
						print_token();
						print_newline();
					} else {
						// EXPR or DO_BLOCK
						print_token();
						print_single_space();
					}
				}
				break;

			case OPERATOR:

				boolean space_before = true;
				boolean space_after = true;
				if (is_special_word(last_text)) {
					// "return" had a special handling in TK_WORD. Now we need
					// to return the favor
					print_single_space();
					print_token();
					break;
				}

				// hack for actionscript's import .*;
				if (token_text.equals("*") && last_type == TK.DOT
						&& !last_last_text.matches("^\\d+$")) {
					print_token();
					break;
				}

				if (token_text.equals(":") && flags.in_case) {
					flags.case_body = true;
					indent();
					print_token();
					print_newline();
					flags.in_case = false;
					break;
				}

				if (token_text.equals("::")) {
					// no spaces around exotic namespacing syntax operator
					print_token();
					break;
				}

				if (in_array(token_text, new String[] { "--", "++", "!" })
						|| (in_array(token_text, new String[] { "-", "+" }) && (in_array(
								last_type, new Object[] { TK.START_BLOCK,
										TK.START_EXPR, TK.EQUALS, TK.OPERATOR })
								|| in_array(last_text, line_starters) || last_text
									.equals(",")))) {
					// unary operators (and binary +/- pretending to be unary)
					// special cases

					space_before = false;
					space_after = false;

					if (last_text.equals(";") && is_expression(flags.mode)) {
						// for (;; ++i)
						// ^^^
						space_before = true;
					}
					if (last_type == TK.WORD
							&& in_array(last_text, line_starters)) {
						space_before = true;
					}

					if (flags.mode == Mode.BLOCK
							&& (last_text.equals("{") || last_text.equals(";"))) {
						// { foo; --i }
						// foo(); --bar;
						print_newline();
					}
				} else if (token_text.equals(":")) {
					if (flags.ternary_depth == 0) {
						if (flags.mode == Mode.BLOCK) {
							flags.mode = Mode.OBJECT;
						}
						space_before = false;
					} else {
						flags.ternary_depth -= 1;
					}
				} else if (token_text.equals("?")) {
					flags.ternary_depth++;
				}
				if (space_before) {
					print_single_space();
				}

				print_token();

				if (space_after) {
					print_single_space();
				}

				break;

			case BLOCK_COMMENT:

				String[] lines = split_newlines(token_text);
				int j; // iterator for this case

				if (all_lines_start_with(slice(lines, 1), '*')) {
					// javadoc: reformat and reindent
					print_newline();
					output.push(lines[0]);
					for (j = 1; j < lines.length; j++) {
						print_newline();
						output.push(" ");
						output.push(lines[j].trim());
					}

				} else {

					// simple block comment: leave intact
					if (lines.length > 1) {
						// multiline comment block starts with a new line
						print_newline();
					} else {
						// single-line /* comment */ stays where it is
						if (last_type == TK.END_BLOCK) {
							print_newline();
						} else {
							print_single_space();
						}
					}

					for (j = 0; j < lines.length; j++) {
						output.push(lines[j]);
						output.push("\n");
					}

				}
				if (look_up('\n') != '\n') {
					print_newline();
				}
				break;

			case INLINE_COMMENT:

				print_single_space();
				print_token();
				print_single_space();
				break;

			case COMMENT:

				if (last_text.equals(",") && !wanted_newline) {
					trim_output(true);
				}
				if (last_type != TK.COMMENT) {
					if (wanted_newline) {
						print_newline();
					} else {
						print_single_space();
					}
				}
				print_token();
				print_newline();
				break;

			case UNKNOWN:

				print_token();
				break;
			case EOF:
				break;
			default:
				break;
			}

			// The cleanest handling of inline comments is to treat them as
			// though they aren't there.
			// Just continue formatting and the behavior should be logical.
			if (token_type != TK.INLINE_COMMENT) {
				last_last_text = last_text;
				last_type = token_type;
				last_text = token_text;
			}
		}

		String sweet_code = preindent_string
				+ join(output).replaceAll("[\\r\\n ]+$", "");
		return sweet_code;
	}

	private String join(List<String> list) {
		StringBuffer buf = new StringBuffer();
		for (String item : list) {
			buf.append(item);
		}
		return buf.toString();
	}

	private String[] slice(String[] lines, int start) {
		String[] sliced = new String[lines.length - start];
		System.arraycopy(lines, start, sliced, 0, sliced.length);
		return sliced;
	}
}
