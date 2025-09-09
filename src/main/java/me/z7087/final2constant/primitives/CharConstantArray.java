package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

public interface CharConstantArray extends AbstractPrimitiveConstantArray<Character> {

    @Deprecated
    @Override
    default Character get(int index) {
        return getChar(index);
    }

    char getChar(int index);

    @Deprecated
    @Override
    default void set(int index, Character value) {
        setChar(index, value);
    }

    void setChar(int index, char value);

    default char[] toCharArray(IntFunction<char[]> generator) {
        final int size = size();
        final char[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getChar(i);
        }
        return array;
    }
}
