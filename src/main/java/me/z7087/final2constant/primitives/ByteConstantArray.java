package me.z7087.final2constant.primitives;

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
}
