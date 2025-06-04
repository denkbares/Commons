package com.denkbares.utils.test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Test;

import com.denkbares.utils.Either;


public class EitherTest {

	@Test
	public void testLeftCreation() {
		Either<String, Integer> left = Either.left("error");

		Assert.assertTrue(left.isLeft());
		Assert.assertFalse(left.isRight());
		Assert.assertEquals("error", left.getLeft());
		Assert.assertEquals(Optional.of("error"), left.left());
		Assert.assertEquals(Optional.empty(), left.right());
	}

	@Test
	public void testRightCreation() {
		Either<String, Integer> right = Either.right(42);

		Assert.assertFalse(right.isLeft());
		Assert.assertTrue(right.isRight());
		Assert.assertEquals(Integer.valueOf(42), right.getRight());
		Assert.assertEquals(Optional.empty(), right.left());
		Assert.assertEquals(Optional.of(42), right.right());
	}

	@Test(expected = IllegalStateException.class)
	public void testGetRightOnLeft() {
		Either<String, Integer> left = Either.left("error");
		left.getRight();
	}

	@Test(expected = IllegalStateException.class)
	public void testGetLeftOnRight() {
		Either<String, Integer> right = Either.right(42);
		right.getLeft();
	}

	@Test
	public void testMapOnRight() {
		Either<String, Integer> right = Either.right(10);
		Either<String, String> mapped = right.map(x -> "Value: " + x);

		Assert.assertTrue(mapped.isRight());
		Assert.assertEquals("Value: 10", mapped.getRight());
	}

	@Test
	public void testMapOnLeft() {
		Either<String, Integer> left = Either.left("error");
		Either<String, String> mapped = left.map(x -> "Value: " + x);

		Assert.assertTrue(mapped.isLeft());
		Assert.assertEquals("error", mapped.getLeft());
	}

	@Test
	public void testMapLeftOnLeft() {
		Either<String, Integer> left = Either.left("error");
		Either<Integer, Integer> mapped = left.mapLeft(String::length);

		Assert.assertTrue(mapped.isLeft());
		Assert.assertEquals(Integer.valueOf(5), mapped.getLeft()); // "error".length() = 5
	}

	@Test
	public void testMapLeftOnRight() {
		Either<String, Integer> right = Either.right(42);
		Either<Integer, Integer> mapped = right.mapLeft(String::length);

		Assert.assertTrue(mapped.isRight());
		Assert.assertEquals(Integer.valueOf(42), mapped.getRight());
	}

	@Test
	public void testFlatMapOnRight() {
		Either<String, Integer> right = Either.right(10);
		Either<String, Integer> result = right.flatMap(x -> Either.right(x * 2));

		Assert.assertTrue(result.isRight());
		Assert.assertEquals(Integer.valueOf(20), result.getRight());
	}

	@Test
	public void testFlatMapOnRightReturningLeft() {
		Either<String, Integer> right = Either.right(10);
		Either<String, Integer> result = right.flatMap(x -> Either.left("converted to error"));

		Assert.assertTrue(result.isLeft());
		Assert.assertEquals("converted to error", result.getLeft());
	}

	@Test
	public void testFlatMapOnLeft() {
		Either<String, Integer> left = Either.left("error");
		Either<String, Integer> result = left.flatMap(x -> Either.right(x * 2));

		Assert.assertTrue(result.isLeft());
		Assert.assertEquals("error", result.getLeft());
	}

	@Test
	public void testFlatMapLeftOnLeft() {
		Either<String, Integer> left = Either.left("error");
		Either<Integer, Integer> result = left.flatMapLeft(s -> Either.left(s.length()));

		Assert.assertTrue(result.isLeft());
		Assert.assertEquals(Integer.valueOf(5), result.getLeft());
	}

	@Test
	public void testFlatMapLeftOnRight() {
		Either<String, Integer> right = Either.right(42);
		Either<Integer, Integer> result = right.flatMapLeft(s -> Either.left(s.length()));

		Assert.assertTrue(result.isRight());
		Assert.assertEquals(Integer.valueOf(42), result.getRight());
	}

	@Test
	public void testFoldWithConsumers() {
		AtomicReference<String> leftValue = new AtomicReference<>();
		AtomicReference<Integer> rightValue = new AtomicReference<>();

		Either<String, Integer> left = Either.left("error");
		left.fold(leftValue::set, rightValue::set);

		Assert.assertEquals("error", leftValue.get());
		Assert.assertNull(rightValue.get());

		// Reset
		leftValue.set(null);
		rightValue.set(null);

		Either<String, Integer> right = Either.right(42);
		right.fold(leftValue::set, rightValue::set);

		Assert.assertNull(leftValue.get());
		Assert.assertEquals(Integer.valueOf(42), rightValue.get());
	}

	@Test
	public void testFoldWithFunctions() {
		Either<String, Integer> left = Either.left("error");
		String leftResult = left.fold(
				error -> "Error: " + error,
				value -> "Value: " + value
		);
		Assert.assertEquals("Error: error", leftResult);

		Either<String, Integer> right = Either.right(42);
		String rightResult = right.fold(
				error -> "Error: " + error,
				value -> "Value: " + value
		);
		Assert.assertEquals("Value: 42", rightResult);
	}

	@Test
	public void testGetOrElseWithValue() {
		Either<String, Integer> right = Either.right(42);
		Assert.assertEquals(Integer.valueOf(42), right.getOrElse(0));

		Either<String, Integer> left = Either.left("error");
		Assert.assertEquals(Integer.valueOf(0), left.getOrElse(0));
	}

	@Test
	public void testGetOrElseWithFunction() {
		Either<String, Integer> right = Either.right(42);
		Assert.assertEquals(Integer.valueOf(42), right.getOrElse(error -> -1));

		Either<String, Integer> left = Either.left("error");
		Assert.assertEquals(Integer.valueOf(5), left.getOrElse(String::length)); // "error".length() = 5
	}

	@Test
	public void testSwap() {
		Either<String, Integer> left = Either.left("error");
		Either<Integer, String> swappedLeft = left.swap();

		Assert.assertTrue(swappedLeft.isRight());
		Assert.assertEquals("error", swappedLeft.getRight());

		Either<String, Integer> right = Either.right(42);
		Either<Integer, String> swappedRight = right.swap();

		Assert.assertTrue(swappedRight.isLeft());
		Assert.assertEquals(Integer.valueOf(42), swappedRight.getLeft());
	}

	@Test
	public void testEqualsAndHashCode() {
		Either<String, Integer> left1 = Either.left("error");
		Either<String, Integer> left2 = Either.left("error");
		Either<String, Integer> left3 = Either.left("different");
		Either<String, Integer> right1 = Either.right(42);
		Either<String, Integer> right2 = Either.right(42);
		Either<String, Integer> right3 = Either.right(24);

		// Test equals
		Assert.assertEquals(left1, left2);
		Assert.assertNotEquals(left1, left3);
		Assert.assertNotEquals(left1, right1);
		Assert.assertEquals(right1, right2);
		Assert.assertNotEquals(right1, right3);

		// Test hashCode consistency
		Assert.assertEquals(left1.hashCode(), left2.hashCode());
		Assert.assertEquals(right1.hashCode(), right2.hashCode());
	}

	@Test
	public void testToString() {
		Either<String, Integer> left = Either.left("error");
		Assert.assertEquals("Left(error)", left.toString());

		Either<String, Integer> right = Either.right(42);
		Assert.assertEquals("Right(42)", right.toString());
	}

	@Test
	public void testNullValues() {
		Either<String, Integer> leftWithNull = Either.left(null);
		Assert.assertTrue(leftWithNull.isLeft());
		Assert.assertNull(leftWithNull.getLeft());
		Assert.assertEquals(Optional.empty(), leftWithNull.left()); // Optional.ofNullable(null) = empty

		Either<String, Integer> rightWithNull = Either.right(null);
		Assert.assertTrue(rightWithNull.isRight());
		Assert.assertNull(rightWithNull.getRight());
		Assert.assertEquals(Optional.empty(), rightWithNull.right()); // Optional.ofNullable(null) = empty
	}

	@Test
	public void testChainingOperations() {
		// Test successful chain
		Either<?, Integer> result1 = Either.right(10)
				.map(x -> x * 2)
				.flatMap(x -> Either.right(x + 5))
				.map(x -> x / 5);

		Assert.assertTrue(result1.isRight());
		Assert.assertEquals(Integer.valueOf(5), result1.getRight()); // ((10 * 2) + 5) / 5 = 5
	}

	@Test
	public void testTypeInference() {
		// Test that the compiler can infer types correctly
		Either<Exception, String> result = processValue("42");
		Assert.assertTrue(result.isRight());
		Assert.assertEquals("Processed: 42", result.getRight());

		Either<Exception, String> errorResult = processValue("invalid");
		Assert.assertTrue(errorResult.isLeft());
		Assert.assertTrue(errorResult.getLeft() instanceof NumberFormatException);
	}

	// Helper method for type inference test
	private Either<Exception, String> processValue(String input) {
		try {
			int value = Integer.parseInt(input);
			return Either.right("Processed: " + value);
		} catch (NumberFormatException e) {
			return Either.left(e);
		}
	}

	@Test
	public void testReflexivity() {
		Either<String, Integer> either = Either.right(42);
		Assert.assertEquals(either, either);
	}

	@Test
	public void testSymmetry() {
		Either<String, Integer> either1 = Either.left("error");
		Either<String, Integer> either2 = Either.left("error");

		Assert.assertEquals(either1, either2);
		Assert.assertEquals(either2, either1);
	}

	@Test
	public void testTransitivity() {
		Either<String, Integer> either1 = Either.right(42);
		Either<String, Integer> either2 = Either.right(42);
		Either<String, Integer> either3 = Either.right(42);

		Assert.assertEquals(either1, either2);
		Assert.assertEquals(either2, either3);
		Assert.assertEquals(either1, either3);
	}
}
