package me.z7087.final2constant.primitives;

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
}
