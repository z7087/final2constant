package me.z7087.final2constant.primitives;

import me.z7087.final2constant.ConstantArray;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface AbstractPrimitiveConstantArray<T> extends ConstantArray<T> {

    @Deprecated
    @Override
    T get(int index);

    @Deprecated
    @Override
    void set(int index, T value);

    @Deprecated
    @Override
    default boolean isPresent(int index) {
        return true;
    }

    @Deprecated
    @Override
    default boolean isEmpty(int index) {
        return false;
    }

    @Deprecated
    @Override
    default void ifPresent(int index, Consumer<? super T> action) {
        action.accept(get(index));
    }

    @Deprecated
    @Override
    default void ifPresentOrElse(int index, Consumer<? super T> action, Runnable emptyAction) {
        action.accept(get(index));
    }

    @Deprecated
    @Override
    default T orElse(int index, T other) {
        return get(index);
    }

    @Deprecated
    @Override
    default T orElseGet(int index, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        return get(index);
    }

    @Deprecated
    @Override
    default T orElseThrow(int index) {
        return get(index);
    }

    @Deprecated
    @Override
    default <X extends Throwable> T orElseThrow(int index, Supplier<? extends X> exceptionSupplier) {
        return get(index);
    }

    @Deprecated
    @Override
    default void orElseRaceSet(int index, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
    }

    @Deprecated
    @Override
    default void orElseLockSet(int index, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
    }

    @Deprecated
    @Override
    default boolean setIfEmpty(int index, T value) {
        Objects.requireNonNull(value);
        return false;
    }

    @Deprecated
    @Override
    default void setIfEmptyOrThrow(int index, T value) {
        Objects.requireNonNull(value);
        throw new IllegalStateException("Not empty");
    }
}
