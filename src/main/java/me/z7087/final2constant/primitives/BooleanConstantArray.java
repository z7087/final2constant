package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

public interface BooleanConstantArray extends AbstractPrimitiveConstantArray<Boolean> {

    @Deprecated
    @Override
    default Boolean get(int index) {
        return getBoolean(index);
    }

    boolean getBoolean(int index);

    @Deprecated
    @Override
    default void set(int index, Boolean value) {
        setBoolean(index, value);
    }

    void setBoolean(int index, boolean value);

    default boolean[] toBooleanArray(IntFunction<boolean[]> generator) {
        final int size = size();
        final boolean[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getBoolean(i);
        }
        return array;
    }
}
