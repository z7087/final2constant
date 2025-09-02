package me.z7087.final2constant.primitives;

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
}
