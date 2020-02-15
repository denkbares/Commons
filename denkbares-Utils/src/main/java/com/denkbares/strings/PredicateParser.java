/*
 * Copyright (C) 2020 denkbares GmbH, Germany
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */

package com.denkbares.strings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.jetbrains.annotations.NotNull;

/**
 * Class that implements a simple recursive descent parser for logical expressions (condition) that evaluate a set of
 * variables to a boolean value.
 * <p>
 * Note that the evaluator also allows multiple bindings for each variable, so each variable potentially represents a
 * set of values. If a variable is unbound (or 'null'), there will be an empty binding set for the variable. If each
 * variable is either null or single-bounded, the conditions behave like 'normal' conditions.
 * <p>
 * When evaluating the parsed condition, comparing variables with '=' (or '=='), '&lt;', '&lt;=', '&gt;', '&gt;=', it is
 * required that there is at least one variable value for the tested variables that matches the condition. Otherwise (or
 * it no value is available at all), the condition evaluates to false. But note, when checking for a varaible value by
 * comparing with '!=', all values of the variable must be unequal to match the condition. If the variable has no
 * value at all, the unequal check will also be true. It is also possible to directly check for the absence of a
 * specific variable value by testing for '== null'. It is also possible to directly check for the existence of a at
 * least one (but any) value of a specific variable value by testing for '!= null'.
 * <p>
 * The following types of conditions are allowed: <ul> <li><b>Negation</b>: <br>NOT &lt;Cond&gt; <br> !&lt;Cond&gt;
 * </li> <li><b>Disjunction</b>: <br> &lt;Cond&gt; OR &lt;Cond&gt; <br> &lt;Cond&gt; | &lt;Cond&gt; <br> &lt;Cond&gt;
 * || &lt;Cond&gt;</li> <li><b>Conjunction</b>: <br>&lt;Cond&gt; AND &lt;Cond&gt; <br> &lt;Cond&gt; &amp; &lt;Cond&gt;
 * <br> &lt;Cond&gt; &amp;&amp; &lt;Cond&gt;</li> <li><b>Brackets</b>: <br>(&lt;Cond&gt;)</li> <li><b>Atomic
 * Checks</b>:
 * <br>&lt;Variable&gt; [=,==,&lt;,&lt;=,&gt;,&gt;=,!=, ~=] &lt;Text-or-Num-Value&gt;</li></ul>
 * <p>
 * When nesting atomic checks, negation has the highest priority, followed by AND, followed by OR, as usual in common
 * boolean expressions. You may use brackets to naturally change this priority.
 * <p>
 * '~=' is a case-sensitive match against the regular expression, where any of the valiable's values must entirely
 * match the pattern. See {@link Pattern} for details on regular expressions and flags, e.g. to match case-insensitive
 * use prefix '(?i)'. For the other atomic checks, if the right value may be a number or a string, where strings are
 * compared case-insensitive and number-aware ({@link NumberAwareComparator#CASE_INSENSITIVE}). Text values or regular
 * expressions may optionally be quoted by ' or ", e.g. if they contain any whitespaces or token characters.
 * <p>
 * Some examples of valid conditions: <ul> <li>price > '2.000,00 €'</li> <li>weight <= 2</li> <li>ports ~=
 * '.*usb.*'</li> <li>weight >= 1 && weight <= 2 AND processor != i5</li> <li>processor == i5 OR weight >= 1.5 && weight
 * <= 2 OR ports = audio</li> <li>(processor == i5 OR processor == i7) AND ports = audio</li><li>processor != null</li>
 * <li>win_version = null</li> <li>win_version != 10</li> <li>win_version == 10</li> <li>ports != audio</li> </ul>
 *
 * @author Volker Belli (denkbares GmbH)
 * @created 15.02.2020
 */
public class PredicateParser {

	private final String stopToken;

	/**
	 * Creates a new predicate parser.
	 */
	public PredicateParser() {
		this(null);
	}

	/**
	 * Creates a new predicate parser that additionally stops parsing at the specified stopToken literal. Note that this
	 * stop literal is of high priority, it should not be a prefix of any valid token or variable name, otherwise the
	 * parser stops too early. Safe stop literals are e.g. "{" or "\n" (for single line expressions). If null is
	 * specified, no custom stop literal is used.
	 *
	 * @param stopToken the custom stop literal
	 */
	public PredicateParser(String stopToken) {
		if (stopToken != null && stopToken.isEmpty()) stopToken = null;
		this.stopToken = stopToken;
	}

	public Node parse(String condition) throws ParseException {
		Lexer lexer = new Lexer(condition);
		Node node = parseStart(lexer);
		lexer.consume(TokenType.eof);
		return node;
	}

	private Node parseStart(Lexer lexer) throws ParseException {
		return parseOr(lexer);
	}

	private Node parseOr(Lexer lexer) throws ParseException {
		List<Node> nodes = new ArrayList<>();
		nodes.add(parseAnd(lexer));
		while (lexer.consumeIf(TokenType.or)) {
			nodes.add(parseAnd(lexer));
		}
		return (nodes.size() <= 1) ? nodes.get(0)
				: equipment -> nodes.stream().anyMatch(node -> node.eval(equipment));
	}

	private Node parseAnd(Lexer lexer) throws ParseException {
		List<Node> nodes = new ArrayList<>();
		nodes.add(parseNot(lexer));
		while (lexer.consumeIf(TokenType.and)) {
			nodes.add(parseNot(lexer));
		}
		return (nodes.size() == 1) ? nodes.get(0)
				: equipment -> nodes.stream().allMatch(node -> node.eval(equipment));
	}

	private Node parseNot(Lexer lexer) throws ParseException {
		if (lexer.consumeIf(TokenType.not)) {
			Node node = parseBrackets(lexer);
			return equipment -> !node.eval(equipment);
		}
		else {
			return parseBrackets(lexer);
		}
	}

	private Node parseBrackets(Lexer lexer) throws ParseException {
		if (lexer.consumeIf(TokenType.open)) {
			Node result = parseStart(lexer);
			lexer.consume(TokenType.close);
			return result;
		}
		else {
			return parseCompare(lexer);
		}
	}

	private Node parseCompare(Lexer lexer) throws ParseException {
		// check for boolean literals
		if (lexer.consumeIf(TokenType.lit_true)) return TRUE_NODE;
		if (lexer.consumeIf(TokenType.lit_false)) return FALSE_NODE;

		// otherwise consume normal compare con
		String left = Strings.unquote(lexer.consume(TokenType.string, TokenType.number, TokenType.nil).text, '"', '\'');
		Token operator = lexer.consume(TokenType.compare);
		Token right = lexer.next();
		switch (operator.text) {
			case "<":
				return new CompareNode(left, right, true, false, false);
			case "<=":
				return new CompareNode(left, right, true, true, false);
			case ">":
				return new CompareNode(left, right, false, false, true);
			case ">=":
				return new CompareNode(left, right, false, true, true);
			case "=":
			case "==":
				return new CompareNode(left, right, false, true, false);
			case "!=":
				return new CompareNode(left, right, true, false, true);
			case "~=":
				return new RegexNode(left, right);
			default:
				throw new ParseException("unknown operator " + operator.text, lexer.position());
		}
	}

	/**
	 * Interface that represents a parsed logical expression, that is capable to evaluate itself to true or false, based
	 * on a given binding of the variables.
	 */
	@FunctionalInterface
	public interface Node {
		boolean eval(ValueProvider equipment);
	}

	/**
	 * Interface that represents a binding of variables, returning the set of values for each given variable name. If a
	 * variable is unbound (aka 'null' or 'nil'), the method should return an empty collection. Otherwise it returns the
	 * value(s) the variable is bound to.
	 * <p>
	 * In most cases, the returned collection contains a single value, representing the bound value of the variable, but
	 * the logical evaluator allows multiple values for each element, using an implicit 'or', so that the expression
	 * becomes true it it matches to at least one value.
	 */
	@FunctionalInterface
	public interface ValueProvider {
		@NotNull
		Collection<String> get(@NotNull String variable);
	}

	/**
	 * Exception that signals an invalid input string that cannot be parsed correctly.
	 */
	public static class ParseException extends IOException {
		private static final long serialVersionUID = 8968251113043826482L;

		private final String text;
		private final int position;

		public ParseException(String text, int position) {
			super(text + "; column=" + position);
			this.text = text;
			this.position = position;
		}

		/**
		 * Returns the plain error message, that caused the exception, with no position information.
		 *
		 * @return the plain error message
		 */
		public String getErrorText() {
			return text;
		}

		/**
		 * Returns the index (column, position) of the input string that has been parsed, where the parse exception
		 * occurred.
		 *
		 * @return the error position of the parsed input string
		 */
		public int getPosition() {
			return position;
		}
	}

	private enum TokenType {
		eof("^()$"),
		whitespace("^(\\s+)"),
		open("^(\\()"), close("^(\\))"), compare("^(<=?|>=?|==?|!=|~=)"),
		or("^(OR|\\|\\|?)[^\\w]"), and("^(AND|&&?)[^\\w]"), not("^(!|NOT)[^\\w]"),
		nil("^(null)"),
		lit_true("^(true)"), lit_false("^(false)"),
		number("^([+\\-]?\\d+(\\.\\d+)?)"),
		string("^(\\w[^\\s()!|&\"']*|\"[^\n\"]*\"|'[^\n']*')");

		private final Pattern pattern;

		TokenType(String pattern) {
			assert pattern.charAt(0) == '^';
			this.pattern = Pattern.compile(pattern);
		}
	}

	private class Lexer {

		private final String expression;
		private final List<Token> tokens = new ArrayList<>();
		private int index = 0;

		public Lexer(String expression) throws ParseException {
			this.expression = expression;
			int offset = 0;
			while (true) {
				// stop at custom stop token, simulating eof
				if (stopToken != null && expression.startsWith(stopToken, offset)) {
					tokens.add(new Token(TokenType.eof, stopToken, offset, offset + stopToken.length()));
					break;
				}

				// otherwise find next token
				Token token = consumeSingleToken(offset);
				offset = token.end;
				// skip whitespaces
				if (token.tokenType != TokenType.whitespace) {
					tokens.add(token);
				}
				// stop after eof
				if (token.tokenType == TokenType.eof) {
					break;
				}
			}
		}

		private Token consumeSingleToken(int offset) throws ParseException {
			for (TokenType tokenType : TokenType.values()) {
				Matcher matcher = tokenType.pattern.matcher(expression);
				matcher.region(offset, expression.length());
				if (matcher.find()) {
					int start = matcher.start(1)/* + offset*/;
					int end = matcher.end(1) /*+ offset*/;
					return new Token(tokenType, matcher.group(1), start, end);
				}
			}
			throw new ParseException("unknown symbol: " + Strings.ellipsis(expression.substring(offset), 20), offset);
		}

		public Token peek() {
			return tokens.get(index);
		}

		public Token next() throws ParseException {
			if (index >= tokens.size()) {
				throw new ParseException("cannot read beyond eof", position());
			}
			return tokens.get(index++);
		}

		public Token consume(TokenType... allowedTypes) throws ParseException {
			Token next = next();
			for (TokenType type : allowedTypes) {
				if (next.tokenType == type) return next;
			}
			throw new ParseException("unexpected token, '" + next.text + "' is not of type " +
					Strings.concat(", ", allowedTypes), next.start);
		}

		public boolean consumeIf(TokenType type) throws ParseException {
			if (peek().tokenType == type) {
				next();
				return true;
			}
			return false;
		}

		public int position() {
			return peek().start;
		}
	}

	private static class Token {
		private final TokenType tokenType;
		private final String text;
		private final int start;
		private final int end;

		public Token(TokenType tokenType, String text, int start, int end) {
			this.tokenType = tokenType;
			this.text = text;
			this.start = start;
			this.end = end;
		}
	}

	private static class CompareNode implements Node {
		private final String variable;
		private final String textValue;
		private final Number numValue;
		private final boolean acceptLess, acceptEqual, acceptGreater;

		private CompareNode(String variable, Token value, boolean acceptLess, boolean acceptEqual, boolean acceptGreater) throws ParseException {
			this.variable = variable;
			this.textValue = (value.tokenType == TokenType.nil) ? null : Strings.unquote(value.text, '"', '\'');
			this.numValue = (value.tokenType == TokenType.number) ? Double.parseDouble(value.text) : null;
			this.acceptLess = acceptLess;
			this.acceptEqual = acceptEqual;
			this.acceptGreater = acceptGreater;
			if (value.tokenType == TokenType.nil) {
				if (acceptLess != acceptGreater) {
					throw new ParseException("'null' can only be compared with '==' or '!='", value.start);
				}
			}
			else if (value.tokenType != TokenType.number && value.tokenType != TokenType.string) {
				throw new ParseException("string or  number expected, but not a " + value.tokenType, value.start);
			}
		}

		@Override
		public boolean eval(ValueProvider valueProvider) {
			Collection<String> values = valueProvider.get(variable);
			// if we test for null (== or !=),
			// we compare against the fact if the set if empty or not
			if (textValue == null) {
				// if the values are empty and we accept '== null', or not empty and '!= null', return true
				return values.isEmpty() == acceptEqual;
			}
			// if we test for '!=', all items must have an other value, or the set is empty
			else if (acceptLess && acceptGreater && !acceptEqual) {
				return values.stream().allMatch(this::accept);
			}
			// otherwise test if any of the values is accepted
			return values.stream().anyMatch(this::accept);
		}

		private boolean accept(String value) {
			if (numValue != null && TokenType.number.pattern.matcher(value).matches()) {
				double num = Double.parseDouble(value);
				return accept(Double.compare(num, numValue.doubleValue()));
			}
			else {
				return accept(NumberAwareComparator.CASE_INSENSITIVE.compare(value, textValue));
			}
		}

		private boolean accept(int compare) {
			return (acceptLess && compare < 0) || (acceptEqual && compare == 0) || (acceptGreater && compare > 0);
		}
	}

	private static class RegexNode implements Node {
		private final String variable;
		private final Pattern pattern;

		private RegexNode(String variable, Token value) throws ParseException {
			String regex = Strings.unquote(value.text, '"', '\'');
			try {
				this.variable = variable;
				this.pattern = Pattern.compile(regex);
			}
			catch (PatternSyntaxException e) {
				throw new ParseException("invalid regular expression " + regex, value.start);
			}
		}

		@Override
		public boolean eval(ValueProvider valueProvider) {
			// get the values and test if any matches
			for (String value : valueProvider.get(variable)) {
				if (pattern.matcher(value).matches()) {
					return true;
				}
			}
			// if no value returns true (or there is no value)
			return false;
		}
	}

	private static class ConstantNode implements Node {
		private final boolean value;

		private ConstantNode(boolean value) {
			this.value = value;
		}

		@Override
		public boolean eval(ValueProvider valueProvider) {
			return value;
		}
	}

	private static final ConstantNode TRUE_NODE = new ConstantNode(true);
	private static final ConstantNode FALSE_NODE = new ConstantNode(false);
}
