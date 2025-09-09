package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

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

    default float[] toFloatArray(IntFunction<float[]> generator) {
        final int size = size();
        final float[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getFloat(i);
        }
        return array;
    }
}
