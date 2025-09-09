package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

public interface ShortConstantArray extends AbstractPrimitiveConstantArray<Short> {

    @Deprecated
    @Override
    default Short get(int index) {
        return getShort(index);
    }

    short getShort(int index);

    @Deprecated
    @Override
    default void set(int index, Short value) {
        setShort(index, value);
    }

    void setShort(int index, short value);

    default short[] toShortArray(IntFunction<short[]> generator) {
        final int size = size();
        final short[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getShort(i);
        }
        return array;
    }
}
