package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

public interface DoubleConstantArray extends AbstractPrimitiveConstantArray<Double> {

    @Deprecated
    @Override
    default Double get(int index) {
        return getDouble(index);
    }

    double getDouble(int index);

    @Deprecated
    @Override
    default void set(int index, Double value) {
        setDouble(index, value);
    }

    void setDouble(int index, double value);

    default double[] toDoubleArray(IntFunction<double[]> generator) {
        final int size = size();
        final double[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getDouble(i);
        }
        return array;
    }
}
