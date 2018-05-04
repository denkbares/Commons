/*
 * Copyright (C) 2015 denkbares GmbH
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
package com.denkbares.utils;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.denkbares.strings.Strings;

/**
 * Utility class for instantiating textual constructor calls using a specific {@link ClassLoader}. The {@link
 * ClassLoader} that shall has to be specified in the constructor. The {@link #newInstance} method does the actual
 * instantiation of constructor calls. The instantiation supports the definition of primitive arguments {@link String},
 * double, int.
 *
 * @author Sebastian Furth (denkbares GmbH)
 * @created 08.06.15
 */
public class Instantiation {

	private final ClassLoader classLoader;
	private InstantiationContext context = () -> "--unknown--";

	public Instantiation(ClassLoader classLoader) {
		this.classLoader = Objects.requireNonNull(classLoader);
	}

	public InstantiationContext getContext() {
		return context;
	}

	public void setContext(InstantiationContext context) {
		this.context = Objects.requireNonNull(context);
	}

	private static final String IDENTIFIER = "\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*";
	private static final String FULL_QUALIFIED_IDENTIFIER = IDENTIFIER + "(?:\\." + IDENTIFIER + ")*";

	private static final Pattern CONSTRUCTOR_CALL = Pattern.compile(
			"^\\s*(?:new\\s+)?(" + FULL_QUALIFIED_IDENTIFIER + ")\\s*(?:\\((.*)\\))?\\s*$");

	private static final Pattern METHOD_CALL = Pattern.compile(
			"^\\s*(" + FULL_QUALIFIED_IDENTIFIER + ")\\.(" + IDENTIFIER + ")\\s*\\((.*)\\)\\s*$");

	private static final Pattern CONSTANT_REFERENCE = Pattern.compile(
			"^\\s*(?:(" + FULL_QUALIFIED_IDENTIFIER + ")\\.)?(" + IDENTIFIER + ")\\s*$");

	private static final Pattern BOOLEAN = Pattern.compile("true|false");

	/**
	 * Tries to invoke a constructor of the specified constructor call . The arguments are parsed from the constructor
	 * call expression. If there is no such constructor or if the constructor cannot be accessed, null is returned. If
	 * the constructor can be called, but fails with an exception, an InvocationTargetException is thrown.
	 * <p/>
	 * Example constructor calls are: <ul> <li>java.util.ArrayList</li> <li>java.util.ArrayList(5)</li> </ul>
	 *
	 * @param constructorCall A constructor call that may contain primitive arguments.
	 * @return the created instance
	 * @throws FormatException          if the constructor syntax of the specified constructorCall is not correct
	 * @throws IllegalArgumentException if the parameters of the constructor call does not match the expected arguments
	 *                                  valid
	 */
	public Object newInstance(String constructorCall) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {

		Matcher matcher = CONSTRUCTOR_CALL.matcher(constructorCall);
		if (!matcher.find()) {
			throw new FormatException(context.getOrigin() +
					": invalid constructor format: " + constructorCall);
		}

		String className = matcher.group(1);
		Class<?> clazz = classLoader.loadClass(className);
		List<String> parameters = splitParameterList(matcher.group(2));

		try {
			Stream<Constructor> constructors = Arrays.stream(clazz.getConstructors());
			ParameterValueFactory<Constructor> factory = new ParameterValueFactory<>(parameters, constructors);
			// in the constructors, search for the matching ones
			if (factory.detect()) {
				return factory.getExecutable().newInstance(factory.getParameterValues());
			}
		}
		catch (NoSuchFieldException e) {
			throw new IllegalArgumentException("referenced a non-existing field", e);
		}

		// if no constructor remains, throw an exception
		throw new NoSuchMethodException(context.getOrigin() +
				": no public constructor matches the specified parameters: " + constructorCall);
	}

	/**
	 * Extracts the class object denoted by the specified constructor call.
	 *
	 * @param constructorCall A constructor call that may contain primitive arguments.
	 * @return the extracted class object
	 * @throws FormatException        if the constructor syntax of the specified constructorCall is not correct
	 * @throws ClassNotFoundException if the class could not been found or loaded
	 */
	public Class findClass(String constructorCall) throws ClassNotFoundException, FormatException {
		Matcher matcher = CONSTRUCTOR_CALL.matcher(constructorCall);
		if (!matcher.find()) {
			throw new FormatException(context.getOrigin() +
					": invalid constructor format: " + constructorCall);
		}

		String className = matcher.group(1);
		return classLoader.loadClass(className);
	}

	private static List<String> splitParameterList(String parameters) throws FormatException {
		if (Strings.isBlank(parameters)) return Collections.emptyList();
		parameters = Strings.trim(parameters);
		List<String> result = new ArrayList<>();

		LinkedList<Character> open = new LinkedList<>();
		StringBuilder current = new StringBuilder();

		for (int i = 0; i < parameters.length(); i++) {
			char c = parameters.charAt(i);
			char context = open.isEmpty() ? 0 : open.peek();

			// if escaping is open, always use character as is and consume the escape
			if (isEscape(context)) {
				current.append(c);
				open.pop();
				continue;
			}

			// if escape is detected and we are inside a string, open the escape
			if (isEscape(c) && isQuote(context)) {
				open.push(c);
				continue;
			}

			// if quote is detected
			if (isQuote(c)) {
				current.append(c);

				// if this type of quote is already open, close it
				if (context == c) {
					open.pop();
				}
				// otherwise, we are not inside any type of quote, open quote
				else if (isQuote(context)) {
					open.push(c);
				}
				continue;
			}

			// if we are in a quote, we simply consume the char
			if (isQuote(context)) {
				current.append(c);
				continue;
			}

			// if any type of opening bracket is detected, open bracket
			if (isOpen(c)) {
				current.append(c);
				open.push(c);
				continue;
			}

			// if any type of closing bracket is detected, close bracket, checking for matching brackets
			if (isClose(c)) {
				if (!isBalanced(context, c)) {
					throw new FormatException("not matching bracket at index " + i + ": " + current);
				}
				current.append(c);
				open.pop();
				continue;
			}

			// if ',' is detected, a argument is completed, start new one
			if (c == ',' && context == 0) {
				result.add(Strings.trim(current.toString()));
				current.setLength(0);
				continue;
			}

			// finally simply consume the character
			current.append(c);
		}

		// if we are done, add the current to the list
		// we can always to this, because empty parameters are already handled at the top of the method
		result.add(Strings.trim(current.toString()));

		// also check that the string has not left an open context
		if (!open.isEmpty()) {
			throw new FormatException("unclosed '" + open.peek() + "': " + current);
		}
		return result;
	}

	private static boolean isOpen(char c) {
		return isAny(c, "([{");
	}

	private static boolean isClose(char c) {
		return isAny(c, ")]}");
	}

	private static boolean isBalanced(char open, char close) {
		return (open == '(' && close == ')') ||
				(open == '[' && close == ']') ||
				(open == '{' && close == '}');
	}

	private static boolean isEscape(char c) {
		return c == '\\';
	}

	private static boolean isQuote(char c) {
		return isAny(c, "\"'");
	}

	private static boolean isAny(char c, String characters) {
		return characters.indexOf(c) >= 0;
	}

	public static class FormatException extends InstantiationException {
		private static final long serialVersionUID = 2068803705860670941L;

		public FormatException(String message) {
			super(message);
		}
	}

	/**
	 * Class to parse a (already split) list of parameter and create parameter objects to match any of a set of
	 * executables.
	 */
	private class ParameterValueFactory<T extends Executable> {

		private final List<String> parameters;
		private final List<T> executables;

		private final Map<Pair<String, Class>, Object> valueCache = new HashMap<>();
		private final Object NULL_VALUE = new Object();
		private final Object CANNOT_CREATE = new Object();

		private T detectedExecutable = null;
		private Object[] detectedParameterValues = null;

		private ParameterValueFactory(List<String> parameters, Stream<? extends T> executables) {
			this.parameters = parameters;
			// prefer non-vararg methods, and if varargs, prefer the methods with more (non-vararg) parameters,
			// so that the varargs will be filled last with the remaining parameters only
			Comparator<T> varArgsLast = Comparator
					.comparing((T m) -> m.getParameterCount() >= 1 && m.getParameters()[m.getParameterCount() - 1].isVarArgs())
					.thenComparing(Comparator.comparing(Executable::getParameterCount).reversed());
			this.executables = executables
					.filter(this::hasMatchingParameterCount)
					.sorted(varArgsLast).collect(Collectors.toList());
		}

		public boolean detect() throws NoSuchFieldException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
			if (detectedExecutable != null) return true;
			for (T executable : executables) {
				Object[] parameterValues = createParameterValues(executable);
				if (parameterValues != null) {
					detectedExecutable = executable;
					detectedParameterValues = parameterValues;
					return true;
				}
			}
			return false;
		}

		public T getExecutable() {
			if (detectedExecutable == null) {
				throw new IllegalStateException("not detected");
			}
			return detectedExecutable;
		}

		public Object[] getParameterValues() {
			if (detectedExecutable == null) {
				throw new IllegalStateException("not detected");
			}
			return detectedParameterValues;
		}

		private boolean hasMatchingParameterCount(Executable method) {
			int paramCount = parameters.size();
			int expectedCount = method.getParameterCount();
			if (expectedCount == paramCount) return true;
			if (paramCount < expectedCount - 1) return false;

			// if the last method parameter is a varagrs,
			// paramCount can be one less (empty varargs), or more
			return expectedCount >= 1 && method.getParameters()[expectedCount - 1].isVarArgs();
		}

		/**
		 * Returns the parameters values for the executable, or null if the value strings cannot be converted into
		 * matching parameter values.
		 */
		private Object[] createParameterValues(Executable executable) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
			int parameterCount = executable.getParameterCount();
			Object[] values = new Object[parameterCount];

			for (int i = 0; i < parameterCount; i++) {
				boolean isVarArgs = executable.getParameters()[i].isVarArgs();
				Class type = executable.getParameterTypes()[i];

				// if we are at the varargs parameter, use the rest list of parameters
				Object value = isVarArgs
						? createVarArgValue(parameters.subList(i, parameters.size()), type)
						: createValue(parameters.get(i), type);

				if (value == CANNOT_CREATE) return null;
				values[i] = (value == NULL_VALUE) ? null : value;
			}
			return values;
		}

		private Object createVarArgValue(List<String> parameters, Class<?> arrayType) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
			Class<?> itemType = arrayType.getComponentType();
			Object array = Array.newInstance(itemType, parameters.size());
			for (int i = 0; i < parameters.size(); i++) {
				String parameter = parameters.get(i);
				// if not explicitly null, try to create value for that parameter
				Object value = createValue(parameter, itemType);
				if (value == CANNOT_CREATE) return CANNOT_CREATE;
				if (value == NULL_VALUE) {
					if (itemType.isPrimitive()) return CANNOT_CREATE;
					value = null;
				}
				Array.set(array, i, value);
			}
			return array;
		}

		/**
		 * Creates a instance of the specified type by parsing / interpreting the specified parameter. The method
		 * returns CANNOT_CREATE if no value can be created, and NULL_VALUE if value is null. It uses the cache to avoid
		 * duplicate creations.
		 */
		@NotNull
		private Object createValue(String parameter, Class<?> type) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
			// check if value is already cached
			Pair<String, Class> key = new Pair<>(parameter, type);
			Object result = valueCache.get(key);
			if (result == null) {
				result = evalValue(parameter, type);
				if (result == null) result = NULL_VALUE;
				valueCache.put(key, result);
			}
			return result;
		}

		/**
		 * Creates a instance of the specified type by parsing / interpreting the specified parameter. The method
		 * returns null if no value can be created.
		 */
		private Object evalValue(String parameter, Class<?> type) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

			// test for null constant
			if (Strings.equals(parameter, "null")) return null;

			// parse quoted String
			if (Strings.isQuoted(parameter)) {
				return (type.isAssignableFrom(String.class)) ? Strings.unquote(parameter) : CANNOT_CREATE;
			}

			// parse character constant
			if (Strings.isQuoted(parameter, Strings.QUOTE_SINGLE)) {
				String unquoted = Strings.unquote(parameter, Strings.QUOTE_SINGLE);
				if (unquoted.length() != 1) {
					throw new FormatException("character constant does not contain exactly one character");
				}
				return (type == Character.TYPE || type.isAssignableFrom(Character.class)) ? unquoted.charAt(0) : CANNOT_CREATE;
			}

			// parse boolean constant
			if (BOOLEAN.matcher(parameter).matches()) {
				return (type == Boolean.TYPE || type.isAssignableFrom(Boolean.class))
						? Strings.equalsIgnoreCase(parameter, "true") : CANNOT_CREATE;
			}

			// test for java field reference (enum or constant or ".class")
			Matcher constantReference = CONSTANT_REFERENCE.matcher(parameter);
			if (constantReference.matches()) {
				String className = constantReference.group(1);
				String constantName = constantReference.group(2);
				return evalConstant(type, className, constantName);
			}

			// test for java method call
			Matcher methodCall = METHOD_CALL.matcher(parameter);
			if (methodCall.matches()) {
				String className = methodCall.group(1);
				String methodName = methodCall.group(2);
				List<String> methodParameters = splitParameterList(methodCall.group(3));
				Object value = evalStaticMethod(className, methodName, methodParameters);
				if (value == CANNOT_CREATE) {
					// if no method call matches, throw an exception
					throw new NoSuchMethodException(context.getOrigin() +
							": no public static method matches the specified parameters: " + parameter);
				}
				return ((value == null) || type.isInstance(value)) ? value : CANNOT_CREATE;
			}

			// parse integer number constants
			Object number = evalNumber(type, parameter);
			if (number != CANNOT_CREATE) return number;

			// an other (recursive) constructor call
			if (CONSTRUCTOR_CALL.matcher(parameter).matches()) {
				Object value = newInstance(parameter);
				return (type.isInstance(value)) ? value : CANNOT_CREATE;
			}

			// nothing above matched
			return CANNOT_CREATE;
		}

		private Object evalConstant(Class<?> expectedType, String className, String constantName) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
			// full qualified enums or public static fields
			if (!Strings.isBlank(className)) {
				Class<?> enclosingClass = classLoader.loadClass(className);
				if ("class".equals(constantName)) {
					return enclosingClass;
				}
				Field field = enclosingClass.getField(constantName);
				if (Modifier.isStatic(field.getModifiers())) {
					Object value = field.get(null);
					return isMatchingParameter(value, expectedType) ? value : CANNOT_CREATE;
				}
			}
			// enum names without qualified name
			else if (Enum.class.isAssignableFrom(expectedType)) {
				// find constant by name
				@SuppressWarnings("unchecked")
				Enum enumValue = Strings.parseEnum(constantName, (Class) expectedType);
				return (enumValue == null) ? CANNOT_CREATE : enumValue;
			}

			// referenced a constant, but not found
			return CANNOT_CREATE;
		}

		private Object evalStaticMethod(String className, String methodName, List<String> methodParameters) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException, InvocationTargetException, InstantiationException, IllegalAccessException {
			// carefully load class, because if no such class exists,
			// we can still use it as a constructor call later on
			Class<?> enclosingClass = classLoader.loadClass(className);
			Stream<Method> methods = Arrays.stream(enclosingClass.getMethods())
					.filter(m -> Strings.equals(methodName, m.getName()))
					.filter(m -> Modifier.isStatic(m.getModifiers()));

			// in the remaining constructors, search for the matching ones
			ParameterValueFactory<Method> factory = new ParameterValueFactory<>(methodParameters, methods);
			if (factory.executables.isEmpty()) {
				throw new NoSuchMethodException(context.getOrigin() +
						": no public static method matches the specified method name: " + className + "#" + methodName);
			}

			if (factory.detect()) {
				// if (static) method is found, call the method and use value
				return factory.getExecutable().invoke(null, factory.getParameterValues());
			}
			// if no method call matches
			return CANNOT_CREATE;
		}

		@Nullable
		private Object evalNumber(Class<?> expectedType, String numString) {
			Number number = null;
			try {
				number = Long.decode(numString.replaceAll("[lL]$", ""));
				if (expectedType == Long.TYPE || expectedType == Long.class) return number.longValue();
				if (expectedType == Integer.TYPE || expectedType == Integer.class) return number.intValue();
				if (expectedType == Short.TYPE || expectedType == Short.class) return number.shortValue();
				if (expectedType == Byte.TYPE || expectedType == Byte.class) return number.byteValue();
				if (expectedType == Character.TYPE || expectedType == Character.class) return (char) number.intValue();
				// if not matched here, ignore, because it may be a floating point number
			}
			catch (NumberFormatException ignore) {
				// if no number format, continue
			}

			// parse floating number constants
			try {
				// only parse if not already successfully parsed as a integer
				if (number == null) {
					number = Double.parseDouble(numString.replaceAll("[fFdD]$", ""));
				}
				if (expectedType == Double.TYPE || expectedType == Double.class) return number.doubleValue();
				if (expectedType == Float.TYPE || expectedType == Float.class) return number.floatValue();

				// if Object or Number is expected, use best matching one, where int and double are the java defaults
				if (expectedType.isAssignableFrom(Number.class)) {
					if (Strings.endsWithIgnoreCase(numString, "f")) return number.floatValue();
					if (Strings.endsWithIgnoreCase(numString, "d")) return number.doubleValue();
					if (Strings.endsWithIgnoreCase(numString, "l")) return number.longValue();
					return (number instanceof Long) ? new Integer(number.intValue()) : number;
				}
			}
			catch (NumberFormatException ignored) {
				// if no number format, continue
			}

			// if we reach here, a number format could be parsed, but no number is expected, so return CANNOT_CREATE
			return CANNOT_CREATE;
		}

		private boolean isMatchingParameter(Object value, Class type) {
			return type.isInstance(value) ||
					(!type.isPrimitive() && (value == null)) ||
					(type == Boolean.TYPE && (value instanceof Boolean)) ||
					(type == Double.TYPE && (value instanceof Double)) ||
					(type == Float.TYPE && (value instanceof Float)) ||
					(type == Long.TYPE && (value instanceof Long)) ||
					(type == Integer.TYPE && (value instanceof Integer)) ||
					(type == Character.TYPE && (value instanceof Character)) ||
					(type == Short.TYPE && (value instanceof Short)) ||
					(type == Byte.TYPE && (value instanceof Byte));
		}
	}
}
