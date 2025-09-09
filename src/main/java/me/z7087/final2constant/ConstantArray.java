package me.z7087.final2constant;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public interface ConstantArray<T> {
    T get(int index);

    void set(int index, T value);

    int size();

    default boolean isPresent(int index) {
        return get(index) != null;
    }

    default boolean isEmpty(int index) {
        return get(index) == null;
    }

    default void ifPresent(int index, Consumer<? super T> action) {
        T value = get(index);
        if (value != null)
            action.accept(value);
    }

    default void ifPresentOrElse(int index, Consumer<? super T> action, Runnable emptyAction) {
        T value = get(index);
        if (value != null)
            action.accept(value);
        else
            emptyAction.run();
    }

    default T orElse(int index, T other) {
        T value = get(index);
        if (value != null)
            return value;
        return other;
    }

    default T orElseGet(int index, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        T value = get(index);
        if (value != null)
            return value;
        return supplier.get();
    }

    default T orElseThrow(int index) {
        T value = get(index);
        if (value != null)
            return value;
        throw new NoSuchElementException("No value present");
    }

    default <X extends Throwable> T orElseThrow(int index, Supplier<? extends X> exceptionSupplier) throws X {
        T value = get(index);
        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    default void orElseRaceSet(int index, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        if (get(index) == null) {
            final T value = Objects.requireNonNull(supplier.get());
            synchronized (this) {
                if (get(index) == null) {
                    set(index, value);
                }
            }
        }
    }

    default void orElseLockSet(int index, Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        if (get(index) == null) {
            synchronized (this) {
                if (get(index) == null) {
                    set(index, Objects.requireNonNull(supplier.get()));
                }
            }
        }
    }

    default boolean setIfEmpty(int index, T value) {
        Objects.requireNonNull(value);
        if (get(index) == null) {
            synchronized (this) {
                if (get(index) == null) {
                    set(index, value);
                    return true;
                }
            }
        }
        return false;
    }

    default void setIfEmptyOrThrow(int index, T value) {
        Objects.requireNonNull(value);
        if (get(index) == null) {
            synchronized (this) {
                if (get(index) == null) {
                    set(index, value);
                    return;
                }
            }
        }
        throw new IllegalStateException("Not empty");
    }

    default T[] toArray(IntFunction<T[]> generator) {
        final int size = size();
        final T[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = get(i);
        }
        return array;
    }
}
