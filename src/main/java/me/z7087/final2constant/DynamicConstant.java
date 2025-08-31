package me.z7087.final2constant;

import java.util.Objects;
import java.util.function.Supplier;

public interface DynamicConstant<T> extends Constant<T> {
    void set(T value);

    default void sync() {}

    // StableValue-like stuff

    default void orElseRaceSet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        if (get() == null) {
            final T value = Objects.requireNonNull(supplier.get());
            synchronized (this) {
                if (get() == null) {
                    set(value);
                    sync();
                }
            }
        }
    }
    
    default void orElseLockSet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier);
        if (get() == null) {
            synchronized (this) {
                if (get() == null) {
                    set(Objects.requireNonNull(supplier.get()));
                    sync();
                }
            }
        }
    }

    default boolean setIfEmpty(T value) {
        Objects.requireNonNull(value);
        if (get() == null) {
            synchronized (this) {
                if (get() == null) {
                    set(value);
                    sync();
                    return true;
                }
            }
        }
        return false;
    }

    default void setIfEmptyOrThrow(T value) {
        Objects.requireNonNull(value);
        if (get() == null) {
            synchronized (this) {
                if (get() == null) {
                    set(value);
                    sync();
                    return;
                }
            }
        }
        throw new IllegalStateException("Not empty");
    }
}
