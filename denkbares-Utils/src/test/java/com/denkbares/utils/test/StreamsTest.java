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
package com.denkbares.utils.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.utils.Streams;

/**
 * This test does only test methods which are not used very frequently and are
 * therefore not tested by other tests already (like Headless-App-Tests).
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 18.10.2013
 */
public class StreamsTest {

	@Test
	public void readFiles() throws IOException {
		checkBinarySize("src/test/resources/exampleFiles/faust.jpg");
		checkBinarySize("src/test/resources/exampleFiles/faust.txt");

		Assert.assertEquals(
				"check text length of 'faust.txt'",
				219369,
				Streams.getTextAndClose(
						new FileInputStream("src/test/resources/exampleFiles/faust.txt")).replace("\r", "").length());
	}

	@SuppressWarnings("resource")
	public void checkBinarySize(String filename) throws IOException {
		File file = new File(filename);
		Assert.assertEquals(
				"check file lenght of '" + file + "'",
				file.length(),
				Streams.getBytesAndClose(new FileInputStream(file)).length);
	}

	private record Person(String name, String city) {}

	@Test
	public void groupsByKeyAndMapsValues() {
		List<Person> people = List.of(
				new Person("Ada", "London"),
				new Person("Grace", "New York"),
				new Person("Alan", "London"),
				new Person("Edsger", "Amsterdam"),
				new Person("Barbara", "New York")
		);

		Map<String, List<String>> result = people.stream().collect(
				Streams.groupBy(Person::city, Person::name)
		);

		Assert.assertEquals(Set.of("London", "New York", "Amsterdam"), result.keySet());
		Assert.assertEquals(List.of("Ada", "Alan"), result.get("London"));
		Assert.assertEquals(List.of("Grace", "Barbara"), result.get("New York"));
		Assert.assertEquals(List.of("Edsger"), result.get("Amsterdam"));
	}
}
