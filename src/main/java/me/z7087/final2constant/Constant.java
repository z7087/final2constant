package me.z7087.final2constant;

import me.z7087.final2constant.util.JavaHelper;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Constant<T> {
    ConstantFactory factory = createFactory();

    static ConstantFactory createFactory() {
        if (JavaHelper.CACHED_JAVA_VERSION < 15) {
            return new UnsafeAnonymousConstantFactory();
        } else {
            return new LookupHiddenConstantFactory();
        }
    }

    T get();

    default boolean isPresent() {
        return get() != null;
    }

    default boolean isEmpty() {
        return get() == null;
    }

    default void ifPresent(Consumer<? super T> action) {
        T value = get();
        if (value != null)
            action.accept(value);
    }

    default void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        T value = get();
        if (value != null)
            action.accept(value);
        else
            emptyAction.run();
    }

    default T orElse(T other) {
        T value = get();
        if (value != null)
            return value;
        return other;
    }

    default T orElseGet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        T value = get();
        if (value != null)
            return value;
        return supplier.get();
    }

    default T orElseThrow() {
        T value = get();
        if (value != null)
            return value;
        throw new NoSuchElementException("No value present");
    }

    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        T value = get();
        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }
}
