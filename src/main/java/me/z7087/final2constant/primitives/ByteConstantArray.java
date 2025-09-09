package me.z7087.final2constant.primitives;

import java.util.function.IntFunction;

public interface ByteConstantArray extends AbstractPrimitiveConstantArray<Byte> {

    @Deprecated
    @Override
    default Byte get(int index) {
        return getByte(index);
    }

    byte getByte(int index);

    @Deprecated
    @Override
    default void set(int index, Byte value) {
        setByte(index, value);
    }

    void setByte(int index, byte value);

    default byte[] toByteArray(IntFunction<byte[]> generator) {
        final int size = size();
        final byte[] array = generator.apply(size);
        for (int i = 0; i < size; ++i) {
            array[i] = getByte(i);
        }
        return array;
    }
}
