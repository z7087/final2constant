package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

public interface LongConstantArray extends AbstractPrimitiveConstantArray<Long> {

    @Deprecated
    @Override
    default Long get(int index) {
        return getLong(index);
    }

    long getLong(int index);

    @Deprecated
    @Override
    default void set(int index, Long value) {
        setLong(index, value);
    }

    void setLong(int index, long value);

    default long[] toLongArray(IntFunction<long[]> generator) {
        final int size = size();
        final long[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getLong(i);
        }
        return array;
    }
}
