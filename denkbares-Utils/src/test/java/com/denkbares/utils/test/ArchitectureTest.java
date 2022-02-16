package com.denkbares.utils.test;

import java.util.stream.Stream;

import org.junit.Test;

import com.denkbares.utils.Architecture;

import static org.junit.Assert.*;

/**
 * @author Rüdiger Hain (denkbares GmbH)
 * @created 16.02.22
 */
public class ArchitectureTest {

	@Test
	public void testSystemArchAvailable() {
		var archProp = System.getProperty("os.arch");
		var current = Architecture.getSystemArch();
		assertNotNull(current);
		assertEquals(archProp, current.getName());
	}

	@Test
	public void testSystemArchArm64() {
		testSystemArch("aarch64",Architecture.AARCH64);
	}

	@Test
	public void testSystemArchX86_64() {
		testSystemArch("x86_64",Architecture.X86_64);
	}

	private void testSystemArch(String archName, Architecture expectedArch) {
		System.setProperty("os.arch",archName);
		var current = Architecture.getSystemArch();
		assertEquals(current, expectedArch);
	}
}
