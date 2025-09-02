package me.z7087.final2constant.primitives;

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
}
