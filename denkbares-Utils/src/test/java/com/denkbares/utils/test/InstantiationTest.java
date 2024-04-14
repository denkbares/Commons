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

import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Test;

import com.denkbares.utils.Instantiation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Volker Belli (denkbares GmbH)
 * @created 20.01.2016
 */
public class InstantiationTest {

	@Test
	public void enums() throws Exception {
		MyClass instance = newMyClass("MyClass(com.denkbares.utils.test.InstantiationTest$MyEnum.value)");
		assertEquals(MyEnum.value, instance.myValue);

		// unqualified enum
		instance = newMyClass("MyClass(my)");
		assertEquals(MyEnum.my, instance.myValue);
	}

	@Test
	public void variousParameters() throws Exception {
		MyClass instance = newMyClass("MyClass(17, 14.5, 24, 999L)");
		assertEquals(17, instance.i);
		assertEquals(14.5, instance.f, 0.0001);
		assertEquals(24.0, instance.d, 0.0001);
		assertEquals(999L, instance.o);
		assertNotNull(instance.myObjectVarArgs);
		assertEquals(0, instance.myObjectVarArgs.length);

		assertEquals('c', newMyClass("MyClass('c', 999)").c.charValue());
		assertEquals(999, newMyClass("MyClass('c', 999)").o);
		assertEquals("test", newMyClass("MyClass('c', \"test\")").o);
		assertEquals('x', newMyClass("MyClass('c', 'x')").o);
		assertEquals(Boolean.TRUE, newMyClass("MyClass('c', true)").o);
		assertEquals(Boolean.FALSE, newMyClass("MyClass('c', false)").o);
		assertEquals(null, newMyClass("MyClass('c', null)").o);
		assertEquals("t{est", newMyClass("MyClass('c', \"t{est\")").o);
	}

	@Test
	public void varArgs() throws Exception {
		int[] ints = newMyClass("MyClass(\"foo\", 1,3,5)").myIntVarArgs;
		assertEquals(3, ints.length);
		assertEquals(1, ints[0]);
		assertEquals(3, ints[1]);
		assertEquals(5, ints[2]);

		MyEnum[] enums = newMyClass("MyClass(my, value, my)").myEnumVarArgs;
		assertEquals(3, enums.length);
		assertEquals(MyEnum.my, enums[0]);
		assertEquals(MyEnum.value, enums[1]);
		assertEquals(MyEnum.my, enums[2]);

		Object[] objects = newMyClass("MyClass(1,1f,1d, null, null, 2, null, \"text\", null)").myObjectVarArgs;
		assertEquals(5, objects.length);
		assertEquals(null, objects[0]);
		assertEquals(2, objects[1]);
		assertEquals(null, objects[2]);
		assertEquals("text", objects[3]);
		assertEquals(null, objects[4]);

		// also test empty varargs (not default constructor is provided)
		assertEquals(0, newMyClass("MyClass()").myEnumVarArgs.length);
	}

	@Test
	public void nestedConstructors() throws Exception {
		assertEquals(MyEnum.my, ((MyClass) newMyClass("MyClass('c', " +
				"new com.denkbares.utils.test.InstantiationTest$MyClass(my))").o).myValue);

		assertEquals("", ((MyClass) newMyClass("MyClass('c', " +
				"new com.denkbares.utils.test.InstantiationTest$MyClass('x', new java.lang.String()))").o).o);
		assertEquals("foo", ((MyClass) newMyClass("MyClass('c', " +
				"new com.denkbares.utils.test.InstantiationTest$MyClass('x', new java.lang.String(\"foo\")))").o).o);
	}

	@Test
	public void staticMethods() throws Exception {
		assertEquals(17, newMyClass("MyClass('c', java.lang.Integer.parseInt(\"17\"))").o);
	}

	@Test
	public void nestedParameterParsing() throws Exception {
		MyClass myClass = newMyClass("MyClass('c', new java.lang.Exception(\"outer\", new java.lang.IllegalArgumentException(\"inner\", null)))");

		Exception actual = (Exception) myClass.o;
		assertEquals(Exception.class, actual.getClass());
		assertEquals("outer", actual.getMessage());

		Throwable inner = actual.getCause();
		assertEquals(IllegalArgumentException.class, inner.getClass());
		assertEquals("inner", inner.getMessage());
		assertEquals(null, inner.getCause());
	}

	@Test
	public void referConstants() throws Exception {
		assertEquals(0, (int) newMyClass(
				"MyClass(java.lang.Character.MIN_VALUE, java.lang.String.CASE_INSENSITIVE_ORDER)").c);
		assertEquals(String.CASE_INSENSITIVE_ORDER, newMyClass(
				"MyClass(java.lang.Character.MIN_VALUE, java.lang.String.CASE_INSENSITIVE_ORDER)").o);
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongConstant() throws Exception {
		newMyClass("MyClass(com.denkbares.utils.test.InstantiationTest$MyEnum.value1)");
		Assert.fail();
	}

	@Test(expected = IllegalArgumentException.class)
	public void wrongClass() throws Exception {
		newMyClass("MyClass(com.denkbares.utils.test.InstantiationTest.value)");
		Assert.fail();
	}

	@Test(expected = ClassNotFoundException.class)
	public void missingClass() throws Exception {
		newMyClass("NoClass()");
		Assert.fail();
	}

	private MyClass newMyClass(String constructorCall) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
		return (MyClass) new Instantiation(getClass().getClassLoader()).newInstance(
				"com.denkbares.utils.test.InstantiationTest$" + constructorCall);
	}

	@Test
	public void registeredClassName() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
		var instantiation = new Instantiation(getClass().getClassLoader());
		instantiation.registerKnownClass("ShortMyClass", MyClass.class);
		instantiation.registerKnownClass("ShortMyEnum", MyEnum.class);
		var myClassInt = (MyClass) instantiation.newInstance("ShortMyClass(\"\", 17, 19)");
		assertEquals(2, myClassInt.myIntVarArgs.length);
		assertEquals(17, myClassInt.myIntVarArgs[0]);
		assertEquals(19, myClassInt.myIntVarArgs[1]);

		var myClassEnum = (MyClass) instantiation.newInstance("ShortMyClass(ShortMyEnum.value)");
		assertEquals(MyEnum.value, myClassEnum.myValue);
	}

	public static class MyClass {
		private MyEnum myValue;
		private int i;
		private float f;
		private double d;
		private Character c;
		private Object o;
		private MyEnum[] myEnumVarArgs;
		private Object[] myObjectVarArgs;
		private int[] myIntVarArgs;

		@SuppressWarnings("unused") // constructor used by reflections
		public MyClass(MyEnum myValue) {
			this.myValue = myValue;
		}

		public MyClass(MyEnum... myEnumVarArgs) {
			this.myEnumVarArgs = myEnumVarArgs;
		}

		public MyClass(String foo, int... myIntVarArgs) {
			this.myIntVarArgs = myIntVarArgs;
		}

		@SuppressWarnings("unused") // constructor used by reflections
		public MyClass(Character c, Object o) {
			this.c = c;
			this.o = o;
		}

		@SuppressWarnings("unused") // constructor used by reflections
		public MyClass(int i, float f, double d, Object o, Object... myObjectVarArgs) {
			this.i = i;
			this.f = f;
			this.d = d;
			this.o = o;
			this.myObjectVarArgs = myObjectVarArgs;
		}
	}

	public enum MyEnum {
		my, value
	}
}
