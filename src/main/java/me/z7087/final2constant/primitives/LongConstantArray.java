package me.z7087.final2constant.primitives;

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
}
