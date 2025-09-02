package me.z7087.final2constant.primitives;

public interface FloatConstantArray extends AbstractPrimitiveConstantArray<Float> {

    @Deprecated
    @Override
    default Float get(int index) {
        return getFloat(index);
    }

    float getFloat(int index);

    @Deprecated
    @Override
    default void set(int index, Float value) {
        setFloat(index, value);
    }

    void setFloat(int index, float value);
}
