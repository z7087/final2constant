package me.z7087.final2constant.primitives;

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
}
