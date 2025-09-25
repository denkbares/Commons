/*
 * Copyright (C) 2019 denkbares GmbH, Germany
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

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Data structure to identify and match terms in d3web.
 *
 * @author Albrecht Striffler
 * @created 08.01.2011
 */
public class Identifier implements Comparable<Identifier> {

	private static final char SEPARATOR = '#';
	private static final String SEPARATOR_STRING = String.valueOf(SEPARATOR);

	private static final String CONTROL_CHARS_STRING = " ()[]{}<>\"'=&|\\*+-,.\t" + SEPARATOR;
	private static final char[] CONTROL_CHARS = CONTROL_CHARS_STRING.toCharArray();

	private static final char PRETTY_PRINT_SEPARATOR = '.';
	private static final String PRETTY_PRINT_CHARS_STRING = " .\"";
	private static final char[] PRETTY_PRINT_CHARS = PRETTY_PRINT_CHARS_STRING.toCharArray();

	private final String[] pathElements;

	private String externalForm = null;
	private String externalFormLowerCase = null;
	private String prettyPrint = null;
	private final boolean caseSensitive;

	public Identifier(String... pathElements) {
		this(false, pathElements);
	}

	/**
	 * Creates a new identifier with the given path elements.
	 *
	 * @param caseSensitive decides whether the identifier should match case sensitive or not
	 * @param pathElements  the elements used
	 */
	public Identifier(boolean caseSensitive, String... pathElements) {
		this.caseSensitive = caseSensitive;
		if (pathElements.length >= 1 && pathElements[0] == null) {
			throw new IllegalArgumentException("Cannot create TermIdentifier with null");
		}
		this.pathElements = pathElements;
	}

	/**
	 * Creates a new identifier with the given path elements.
	 *
	 * @param caseSensitive decides whether the identifier should match case sensitive or not
	 * @param pathElements  the elements used
	 */
	public Identifier(boolean caseSensitive, List<String> pathElements) {
		this(caseSensitive, pathElements.toArray(new String[0]));
	}

	/**
	 * Creates a new Identifier based on an existing identifier by appending additional path elements.
	 *
	 * @param parent                 the existing parent identifier
	 * @param additionalPathElements the additional path elements
	 */
	public Identifier(Identifier parent, String... additionalPathElements) {
		int parentLen = parent.pathElements.length;
		int additionalLen = additionalPathElements.length;
		this.caseSensitive = parent.caseSensitive;
		this.pathElements = Arrays.copyOf(parent.pathElements, parentLen + additionalLen);
		System.arraycopy(additionalPathElements, 0, pathElements, parentLen, additionalLen);
	}

	public static boolean needsQuotes(String text) {
		return Strings.containsAny(text, CONTROL_CHARS);
	}

	/**
	 * @see Strings#concatParsable(String, String[])
	 */
	public static String concatParsable(String separator, String[] strings) {
		return Strings.concatParsable(separator, strings);
	}

	/**
	 * @see Strings#parseConcat(String, String)
	 */
	public static String[] parseConcat(String separator, String concatenatedString) {
		return Strings.parseConcat(separator, concatenatedString);
	}

	@Override
	public int hashCode() {
		return toExternalFormLowerCase().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Identifier other = (Identifier) obj;
		if (isCaseSensitive() && other.isCaseSensitive()
				|| isCaseSensitive() != other.isCaseSensitive()) {
			return Arrays.equals(this.pathElements, other.pathElements);
		}
		else {
			if (this.pathElements.length != other.pathElements.length) return false;
			for (int i = 0; i < this.pathElements.length; i++) {
				if (!this.pathElements[i].equalsIgnoreCase(other.pathElements[i])) {
					return false;
				}

			}
			return true;
		}
	}

	private static String getParsableString(String[] pathElements) {
		return Strings.concatParsable(SEPARATOR, CONTROL_CHARS, pathElements);
	}

	/**
	 * Returns whether this Identifier should match case sensitive or not.
	 */
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	/**
	 * Returns the external form of this {@link Identifier}.
	 *
	 * @see Identifier#toExternalForm()
	 */
	@Override
	public String toString() {
		return toPrettyPrint();
	}

	@Override
	public int compareTo(@NotNull Identifier o) {
		int len1 = this.pathElements.length;
		int len2 = o.pathElements.length;
		int len = Math.min(len1, len2);
		for (int i = 0; i < len; i++) {
			int comp = this.pathElements[i].compareToIgnoreCase(o.pathElements[i]);
			if (comp != 0) return comp;
		}
		if (len1 < len2) return -1;
		if (len1 > len2) return 1;
		// both are case sensitive equal, so use compare of external form
		return toExternalFormLowerCase().compareTo(o.toExternalFormLowerCase());
	}

	public int compareToNumberAware(@NotNull Identifier o) {
		int len1 = this.pathElements.length;
		int len2 = o.pathElements.length;
		int len = Math.min(len1, len2);
		for (int i = 0; i < len; i++) {
			int comp = NumberAwareComparator.CASE_INSENSITIVE.compare(this.pathElements[i], o.pathElements[i]);
			if (comp != 0) return comp;
		}
		if (len1 < len2) return -1;
		if (len1 > len2) return 1;
		// both are case sensitive equal, so use compare of external form
		return toExternalFormLowerCase().compareTo(o.toExternalFormLowerCase());
	}

	/**
	 * Returns whether this {@link Identifier} starts with the given {@link Identifier}'s path.
	 *
	 * @param identifier the identifier to check
	 * @created 23.04.2012
	 */
	public boolean startsWith(Identifier identifier) {
		if (identifier.pathElements.length > this.pathElements.length) return false;
		for (int i = 0; i < identifier.pathElements.length; i++) {
			if (!identifier.pathElements[i].equalsIgnoreCase(this.pathElements[i])) return false;
		}
		return true;
	}

	/**
	 * Returns whether the specified {@link Identifier} starts with this {@link Identifier}'s path.
	 * <p>
	 * Note: This is the inverse method of {@link #startsWith(Identifier)}:<br>
	 * <code>a.startsWith(b) === b.isPrefixOf(a)</code>
	 *
	 * @param identifier the identifier to check
	 * @created 23.04.2012
	 */
	public boolean isPrefixOf(Identifier identifier) {
		return identifier.startsWith(this);
	}

	/**
	 * Returns the last element of the path given to create this {@link Identifier}.
	 *
	 * @created 23.04.2012
	 */
	public String getLastPathElement() {
		if (pathElements.length == 0) return "";
		return pathElements[pathElements.length - 1];
	}

	/**
	 * Returns an nth path element of this identifiers path.
	 *
	 * @param index the index of the path element to access
	 * @created 23.04.2012
	 */
	public String getPathElementAt(int index) {
		return pathElements[index];
	}

	/**
	 * Returns a copy of the path elements of this {@link Identifier}.
	 *
	 * @created 25.04.2012
	 */
	public String[] getPathElements() {
		return Arrays.copyOf(this.pathElements, this.pathElements.length);
	}

	/**
	 * Returns the parent identifier of this identifier. It is the identifier with the same path elements except the
	 * last past element. If this identifier is already empty / root, the method returns null.
	 *
	 * @return the parent identifier
	 */
	public Identifier getParent() {
		if (this.pathElements.length == 0) return null;
		return new Identifier(caseSensitive, Arrays.copyOf(this.pathElements, this.pathElements.length - 1));
	}

	/**
	 * Returns a new {@link Identifier} consisting of the identifier elements of the given {@link Identifier} appended
	 * to the identifier elements of this {@link Identifier}. The case sensitivity flag of this instance is preserved,
	 * regardless of the case sensitivity of the specified termIdentifier.
	 *
	 * @param termIdentifier the {@link Identifier} to append
	 * @created 23.04.2012
	 */
	public Identifier append(Identifier termIdentifier) {
		return append(termIdentifier.pathElements);
	}

	/**
	 * Returns a new {@link Identifier} consisting of the identifier elements of the given pathElements appended to the
	 * identifier elements of this {@link Identifier}. The case sensitivity flag of this instance is preserved.
	 *
	 * @param pathElements the path elements to append
	 * @created 23.04.2012
	 */
	public Identifier append(String... pathElements) {
		int newLength = this.pathElements.length + pathElements.length;
		String[] newIdentifierElements = new String[newLength];
		System.arraycopy(this.pathElements, 0, newIdentifierElements, 0, this.pathElements.length);
		System.arraycopy(pathElements, 0, newIdentifierElements, this.pathElements.length, pathElements.length);
		return new Identifier(this.caseSensitive, newIdentifierElements);
	}

	/**
	 * Generates and returns the external representation or form of this {@link Identifier}. It is a String that can be
	 * transformed back into an identical {@link Identifier} as the originating one by using {@link
	 * Identifier#fromExternalForm(String)}.<br/> Basically the external form is the path elements connected with a
	 * separator and proper quoting if the separator icon is contained in one of the path elements.
	 *
	 * @created 07.05.2012
	 */
	public String toExternalForm() {
		// use local variable to avoid NPEs when
		String externalForm = this.externalForm;
		if (externalForm == null) {
			externalForm = (this.isCaseSensitive() ? "C" : "c") + SEPARATOR + getParsableString(pathElements);
			this.externalForm = externalForm;
		}
		return externalForm;
	}

	private String toExternalFormLowerCase() {
		if (this.externalFormLowerCase == null) {
			this.externalFormLowerCase = toExternalForm().toLowerCase();
		}
		return this.externalFormLowerCase;
	}

	/**
	 * Generates and returns the pretty print representation of this {@link Identifier}.
	 * This String can not be transformed back into an identical {@link Identifier}. In this case {@link
	 * Identifier#toExternalForm()} must be used.
	 * <br/> The pretty print form is the path elements connected with a separator and proper quoting if the separator
	 * icon is contained in one of the path elements.
	 */
	public String toPrettyPrint() {
		if (this.prettyPrint == null) {
			this.prettyPrint = Strings.concatParsable(PRETTY_PRINT_SEPARATOR, PRETTY_PRINT_CHARS, pathElements);
		}
		return this.prettyPrint;
	}

	/**
	 * Returns the {@link Identifier} of this identifier that represents the rest of the path defined by the specified
	 * parameter "startIdentifier". The method checks if the specified "startIdentifier" is the beginning of this
	 * identifier, otherwise null is returned. If the startIdentifier is an accepted starting of this identifier, a new
	 * identifier is created that represents the rest of this identifier. Thus <code>a.append(b).rest(a)</code> will
	 * result to
	 * <code>b</code>.
	 *
	 * @param startIdentifier the prefix identifier to be skipped
	 * @return the {@link Identifier} appended to the specified startIdentifier that will together make this identifier,
	 * or null if not possible (because this identifier does not start with parameter startIdentifier)
	 * @created 15.05.2012
	 */
	public Identifier rest(Identifier startIdentifier) {
		if (!startsWith(startIdentifier)) return null;
		int fromIndex = startIdentifier.pathElements.length;
		return rest(fromIndex);
	}

	/**
	 * Returns the {@link Identifier} that represents the rest of the path defined by the specified index parameter
	 * "fromIndex". A new identifier is created that represents the rest of this identifier, from the specified index,
	 * inclusively.
	 *
	 * @param fromIndex the path element index to start from
	 * @return the {@link Identifier} the rest identifier
	 * @throws java.lang.IndexOutOfBoundsException if the index is to high or below 0
	 * @created 01.09.2014
	 */
	public Identifier rest(int fromIndex) {
		String[] newPath = Arrays.copyOfRange(this.pathElements, fromIndex, this.pathElements.length);
		return new Identifier(newPath);
	}

	/**
	 * Returns the {@link Identifier} that represents the sub range of the path defined by the specified index parameter
	 * "fromIndex" to "toIndex". A new identifier is created that represents the range of this identifier, from the
	 * specified fromIndex inclusively, to the specified toIndex, exclusively.
	 *
	 * @param fromIndex the path element index to start from
	 * @return the {@link Identifier} the rest identifier
	 * @throws java.lang.IndexOutOfBoundsException if the indices are not in the range of elements of this identifier
	 * @created 01.09.2014
	 */
	public Identifier sub(int fromIndex, int toIndex) {
		String[] newPath = Arrays.copyOfRange(this.pathElements, fromIndex, toIndex);
		return new Identifier(newPath);
	}

	/**
	 * Returns if the path of this identifier is empty
	 *
	 * @return if path is empty
	 * @created 15.05.2012
	 */
	public boolean isEmpty() {
		return this.getPathElements().length <= 0;
	}

	/**
	 * Generates a {@link Identifier} from the external form of a {@link Identifier}. Do not confuse this with creating
	 * a {@link Identifier} with the constructor using the path of identifier Strings.<br/> Per definition, if you have
	 * a {@link Identifier} and generate the external form for that {@link Identifier} and then generate another {@link
	 * Identifier} from that external form, both TermIdentifiers will be equal.
	 *
	 * @param externalForm the external form of a {@link Identifier} created by using {@link
	 *                     Identifier#toExternalForm()}
	 * @return a {@link Identifier} representing the given external form
	 * @created 07.05.2012
	 */
	public static Identifier fromExternalForm(String externalForm) {
		String[] pathElements = Strings.parseConcat(SEPARATOR_STRING, externalForm);
		boolean isCaseSensitive = false;
		String[] elements = pathElements;
		if (pathElements.length > 1 && pathElements[0].matches("[Cc]")) {
			isCaseSensitive = pathElements[0].equals("C");
			elements = new String[pathElements.length - 1];
			System.arraycopy(pathElements, 1, elements, 0, elements.length);
		}
		return new Identifier(isCaseSensitive, elements);
	}

	/**
	 * Returns the number of path elements of this Identifier.
	 *
	 * @return the number of path elements
	 * @created 23.08.2013
	 */
	public int countPathElements() {
		return this.pathElements.length;
	}
}
