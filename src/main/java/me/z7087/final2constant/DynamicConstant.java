package me.z7087.final2constant;

public interface DynamicConstant<T> extends Constant<T> {
    void set(T value);

    default void sync() {}
}
