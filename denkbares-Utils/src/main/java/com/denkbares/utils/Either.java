package com.denkbares.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either<L, R> {

	/**
	 * Creates a Left Either containing the given value.
	 */
	public static <L, R> Either<L, R> left(L value) {
		return new Left<>(value);
	}

	/**
	 * Creates a Right Either containing the given value.
	 */
	public static <L, R> Either<L, R> right(R value) {
		return new Right<>(value);
	}

	/**
	 * Returns true if this is a Left Either.
	 */
	public abstract boolean isLeft();

	/**
	 * Returns true if this is a Right Either.
	 */
	public abstract boolean isRight();

	/**
	 * Returns the Left value if present, otherwise throws IllegalStateException.
	 */
	public abstract L getLeft();

	/**
	 * Returns the Right value if present, otherwise throws IllegalStateException.
	 */
	public abstract R getRight();

	/**
	 * Returns an Optional containing the Left value if present, empty otherwise.
	 */
	public abstract Optional<L> left();

	/**
	 * Returns an Optional containing the Right value if present, empty otherwise.
	 */
	public abstract Optional<R> right();

	/**
	 * Applies the given function to the Right value if present, otherwise returns this Left.
	 */
	public abstract <T> Either<L, T> map(Function<? super R, ? extends T> mapper);

	/**
	 * Applies the given function to the Left value if present, otherwise returns this Right.
	 */
	public abstract <T> Either<T, R> mapLeft(Function<? super L, ? extends T> mapper);

	/**
	 * Applies the given function to the Right value if present, otherwise returns this Left.
	 * The function should return an Either.
	 */
	public abstract <T> Either<L, T> flatMap(Function<? super R, ? extends Either<L, T>> mapper);

	/**
	 * Applies the given function to the Left value if present, otherwise returns this Right.
	 * The function should return an Either.
	 */
	public abstract <T> Either<T, R> flatMapLeft(Function<? super L, ? extends Either<T, R>> mapper);

	/**
	 * Executes the appropriate consumer based on whether this is Left or Right.
	 */
	public abstract void fold(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer);

	/**
	 * Applies the appropriate function based on whether this is Left or Right.
	 */
	public abstract <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper);

	/**
	 * Returns the Right value if present, otherwise returns the given default value.
	 */
	public abstract R getOrElse(R defaultValue);

	/**
	 * Returns the Right value if present, otherwise returns the result of the given supplier.
	 */
	public abstract R getOrElse(Function<? super L, ? extends R> defaultSupplier);

	/**
	 * Swaps Left and Right.
	 */
	public abstract Either<R, L> swap();

	private static final class Left<L, R> extends Either<L, R> {
		private final L value;

		private Left(L value) {
			this.value = value;
		}

		@Override
		public boolean isLeft() {
			return true;
		}

		@Override
		public boolean isRight() {
			return false;
		}

		@Override
		public L getLeft() {
			return value;
		}

		@Override
		public R getRight() {
			throw new IllegalStateException("Cannot get Right value from Left Either");
		}

		@Override
		public Optional<L> left() {
			return Optional.ofNullable(value);
		}

		@Override
		public Optional<R> right() {
			return Optional.empty();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Either<L, T> map(Function<? super R, ? extends T> mapper) {
			return (Either<L, T>) this;
		}

		@Override
		public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> mapper) {
			return Either.left(mapper.apply(value));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Either<L, T> flatMap(Function<? super R, ? extends Either<L, T>> mapper) {
			return (Either<L, T>) this;
		}

		@Override
		public <T> Either<T, R> flatMapLeft(Function<? super L, ? extends Either<T, R>> mapper) {
			return mapper.apply(value);
		}

		@Override
		public void fold(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer) {
			leftConsumer.accept(value);
		}

		@Override
		public <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper) {
			return leftMapper.apply(value);
		}

		@Override
		public R getOrElse(R defaultValue) {
			return defaultValue;
		}

		@Override
		public R getOrElse(Function<? super L, ? extends R> defaultSupplier) {
			return defaultSupplier.apply(value);
		}

		@Override
		public Either<R, L> swap() {
			return Either.right(value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Left<?, ?> other)) return false;
			return Objects.equals(value, other.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash("Left", value);
		}

		@Override
		public String toString() {
			return "Left(" + value + ")";
		}
	}

	// Right implementation
	private static final class Right<L, R> extends Either<L, R> {
		private final R value;

		private Right(R value) {
			this.value = value;
		}

		@Override
		public boolean isLeft() {
			return false;
		}

		@Override
		public boolean isRight() {
			return true;
		}

		@Override
		public L getLeft() {
			throw new IllegalStateException("Cannot get Left value from Right Either");
		}

		@Override
		public R getRight() {
			return value;
		}

		@Override
		public Optional<L> left() {
			return Optional.empty();
		}

		@Override
		public Optional<R> right() {
			return Optional.ofNullable(value);
		}

		@Override
		public <T> Either<L, T> map(Function<? super R, ? extends T> mapper) {
			return Either.right(mapper.apply(value));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Either<T, R> mapLeft(Function<? super L, ? extends T> mapper) {
			return (Either<T, R>) this;
		}

		@Override
		public <T> Either<L, T> flatMap(Function<? super R, ? extends Either<L, T>> mapper) {
			return mapper.apply(value);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> Either<T, R> flatMapLeft(Function<? super L, ? extends Either<T, R>> mapper) {
			return (Either<T, R>) this;
		}

		@Override
		public void fold(Consumer<? super L> leftConsumer, Consumer<? super R> rightConsumer) {
			rightConsumer.accept(value);
		}

		@Override
		public <T> T fold(Function<? super L, ? extends T> leftMapper, Function<? super R, ? extends T> rightMapper) {
			return rightMapper.apply(value);
		}

		@Override
		public R getOrElse(R defaultValue) {
			return value;
		}

		@Override
		public R getOrElse(Function<? super L, ? extends R> defaultSupplier) {
			return value;
		}

		@Override
		public Either<R, L> swap() {
			return Either.left(value);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof Right<?, ?> other)) return false;
			return Objects.equals(value, other.value);
		}

		@Override
		public int hashCode() {
			return Objects.hash("Right", value);
		}

		@Override
		public String toString() {
			return "Right(" + value + ")";
		}
	}
}
