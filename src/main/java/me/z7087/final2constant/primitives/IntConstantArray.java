package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

public interface IntConstantArray extends AbstractPrimitiveConstantArray<Integer> {

    @Deprecated
    @Override
    default Integer get(int index) {
        return getInt(index);
    }

    int getInt(int index);

    @Deprecated
    @Override
    default void set(int index, Integer value) {
        setInt(index, value);
    }

    void setInt(int index, int value);

    default int[] toIntArray(IntFunction<int[]> generator) {
        final int size = size();
        final int[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getInt(i);
        }
        return array;
    }
}
