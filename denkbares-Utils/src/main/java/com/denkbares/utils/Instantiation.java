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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

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

		// create a subset of constructors with the expected number of arguments
		List<Constructor> constructors = Arrays.stream(clazz.getConstructors())
				.filter(c -> c.getParameterCount() == parameters.size())
				.collect(Collectors.toCollection(LinkedList::new)); // grants mutability

		try {
			// in the remaining constructors, search for the matching ones
			List<Collection<Object>> valueSets = getParameterValues(parameters, constructors);
			for (Constructor constructor : constructors) {
				Object[] values = findParameterValues(constructor, valueSets);
				if (values != null) {
					return constructor.newInstance(values);
				}
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

	/**
	 * Returns the parameters
	 */
	@NotNull
	private List<Collection<Object>> getParameterValues(List<String> parameters, List<? extends Executable> executables) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
		// for each parameter, create a set of possible values that matches a constructor parameter
		List<Collection<Object>> valueSets = new ArrayList<>(parameters.size());
		for (int i = 0; i < parameters.size(); i++) {
			// if parameter is "null", add a singleton set
			// and remove all executables with primitive types at that position
			String parameter = parameters.get(i);
			if (Strings.equals(parameter, "null")) {
				valueSets.add(Collections.singleton(null));
				int index = i;
				executables.removeIf(c -> c.getParameterTypes()[index].isPrimitive());
				continue;
			}

			// create all values for the parameters (but only one per type)
			Map<Class, Object> values = new LinkedHashMap<>();
			Iterator<? extends Executable> iterator = executables.iterator();
			while (iterator.hasNext()) {
				Executable executable = iterator.next();
				Class type = executable.getParameterTypes()[i];
				if (values.containsKey(type)) continue;

				// create value, if not possible remove executable, otherwise add value
				Object value = createValue(parameter, type);
				if (value == null) {
					iterator.remove();
				}
				else {
					values.put(type, value);
				}
			}
			if (values.isEmpty()) {
				throw new IllegalArgumentException(context.getOrigin() +
						": parameter does not match the expected constructor parameter: " + parameter);
			}
			valueSets.add(values.values());
		}
		return valueSets;
	}

	/**
	 * Creates a instance of the specified type by parsing / interpreting the specified parameter. The method returns
	 * null if no value can be created.
	 */
	private Object createValue(String parameter, Class<?> type) throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
		// parse quoted String
		if (Strings.isQuoted(parameter)) {
			return (type.isAssignableFrom(String.class)) ? Strings.unquote(parameter) : null;
		}

		// parse character constant
		if (Strings.isQuoted(parameter, Strings.QUOTE_SINGLE)) {
			String unquoted = Strings.unquote(parameter, Strings.QUOTE_SINGLE);
			if (unquoted.length() != 1) {
				throw new FormatException("character constant does not contain exactly one character");
			}
			return (type == Character.TYPE || type.isAssignableFrom(Character.class)) ? unquoted.charAt(0) : null;
		}

		// parse boolean constant
		if (BOOLEAN.matcher(parameter).matches()) {
			return (type == Boolean.TYPE || type.isAssignableFrom(Boolean.class))
					? Strings.equalsIgnoreCase(parameter, "true") : null;
		}

		// test for java field reference (enum or constant)
		Matcher constantReference = CONSTANT_REFERENCE.matcher(parameter);
		if (constantReference.matches()) {
			String className = constantReference.group(1);
			String constantName = constantReference.group(2);

			// full qualified enums or public static fields
			if (!Strings.isBlank(className)) {
				Class<?> enclosingClass = classLoader.loadClass(className);
				Field field = enclosingClass.getField(constantName);
				if (Modifier.isStatic(field.getModifiers())) {
					return field.get(null);
				}
			}
			// enum names without qualified name
			else if (Enum.class.isAssignableFrom(type)) {
				// find constant by name
				return Strings.parseEnum(constantName, (Class) type);
			}

			// referenced a constant, but not found
			return null;
		}

		// test for java method call
		Matcher methodCall = METHOD_CALL.matcher(parameter);
		if (methodCall.matches()) {
			String className = methodCall.group(1);
			String methodName = methodCall.group(2);
			List<String> methodParameters = splitParameterList(methodCall.group(3));

			// carefully load class, because if no such class exists,
			// we can still use it as a constructor call later on
			Class<?> enclosingClass = classLoader.loadClass(className);
			List<Method> methods = Arrays.stream(enclosingClass.getMethods())
					.filter(m -> Strings.equals(methodName, m.getName()))
					.filter(m -> Modifier.isStatic(m.getModifiers()))
					.collect(Collectors.toList());
			if (methods.isEmpty()) {
				throw new NoSuchMethodException(context.getOrigin() +
						": no public static method matches the specified method name: " + parameter);
			}

			// in the remaining constructors, search for the matching ones
			List<Collection<Object>> valueSets = getParameterValues(methodParameters, methods);
			for (Method method : methods) {
				Object[] values = findParameterValues(method, valueSets);
				if (values != null) {
					return method.invoke(null, values);
				}
			}

			// if no constructor remains, throw an exception
			throw new NoSuchMethodException(context.getOrigin() +
					": no public static method matches the specified parameters: " + parameter);
		}

		// parse integer number constants
		Number number = null;
		try {
			number = Long.decode(parameter.replaceAll("[lL]$", ""));
			if (type == Long.TYPE || type == Long.class) return number.longValue();
			if (type == Integer.TYPE || type == Integer.class) return number.intValue();
			if (type == Short.TYPE || type == Short.class) return number.shortValue();
			if (type == Byte.TYPE || type == Byte.class) return number.byteValue();
			if (type == Character.TYPE || type == Character.class) return (char) number.intValue();
			// if not matched here, ignore, because it may be a floating point number
		}
		catch (NumberFormatException ignore) {
			// if no number format, continue
		}

		// parse floating number constants
		try {
			// only parse if not already successfully parsed as a integer
			if (number == null) {
				number = Double.parseDouble(parameter.replaceAll("[fFdD]$", ""));
			}
			if (type == Double.TYPE || type == Double.class) return number.doubleValue();
			if (type == Float.TYPE || type == Float.class) return number.floatValue();

			// if Object or Number is expected, use best matching one, where int and double are the java defaults
			if (type.isAssignableFrom(Number.class)) {
				if (Strings.endsWithIgnoreCase(parameter, "f")) return number.floatValue();
				if (Strings.endsWithIgnoreCase(parameter, "d")) return number.doubleValue();
				if (Strings.endsWithIgnoreCase(parameter, "l")) return number.longValue();
				return (number instanceof Long) ? new Integer(number.intValue()) : number;
			}

			// if we reach here, a number format could be parsed, but no number is expected, so return null
			return null;
		}
		catch (NumberFormatException ignored) {
			// if no number format, continue
		}

		// an other (recursive) constructor call
		if (CONSTRUCTOR_CALL.matcher(parameter).matches()) {
			Object value = newInstance(parameter);
			return (type.isInstance(value)) ? value : null;
		}

		// nothing above matched
		return null;
	}

	/**
	 * Selects a single value from each collection of the specified list, so that the values fit to the arguments of the
	 * specified constructor. The first value of the returned list is selected from the first collection of the
	 * valueSets parameter, matching the first parameter of teh constructor, and so on. If multiple values of a
	 * collection matches the type, the first one is selected. The method returns null if no argument set is matching
	 * the constructor.
	 */
	private Object[] findParameterValues(Executable constructor, List<Collection<Object>> valueSets) {
		Class[] expectedTypes = constructor.getParameterTypes();
		Object[] result = new Object[expectedTypes.length];

		nextParam:
		for (int i = 0; i < expectedTypes.length; i++) {
			Class expectedType = expectedTypes[i];
			for (Object value : valueSets.get(i)) {
				if (isMatchingParameter(value, expectedType)) {
					result[i] = value;
					continue nextParam;
				}
			}
			// if the previous value iteration not found any match, we are here
			return null;
		}

		// we found a parameter value for each parameter
		return result;
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
		public FormatException(String message) {
			super(message);
		}
	}
}
