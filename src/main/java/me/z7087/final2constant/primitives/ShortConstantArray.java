package me.z7087.final2constant.primitives;

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
}
