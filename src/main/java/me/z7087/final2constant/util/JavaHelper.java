package me.z7087.final2constant.util;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.SerializedLambda;

public final class JavaHelper {
    private JavaHelper() {}

    public static String[][] getNamesAndDescriptors(
            MethodHandles.Lookup lookup,
            Serializable... getterMethodReferences
    ) throws Throwable {
        final String[][] namesAndDescriptors = new String[2][];
        {
            final int length = getterMethodReferences.length;
            final String[] names = new String[length];
            final String[] descriptors = new String[length];
            for (int i = 0; i < length; ++i) {
                final Serializable getterRef = getterMethodReferences[i];
                final Object object = lookup.findVirtual(
                        getterRef.getClass(),
                        "writeReplace",
                        MethodType.methodType(
                                Object.class
                        )
                ).asType(
                        MethodType.methodType(
                                Object.class,
                                Serializable.class
                        )
                ).invokeExact(
                        getterRef
                );
                final SerializedLambda serializedLambda = (SerializedLambda) object;
                names[i] = serializedLambda.getImplMethodName();
                final String implMethodSignature = serializedLambda.getImplMethodSignature();
                if (!implMethodSignature.startsWith("()"))
                    throw new IllegalArgumentException("Getter " + serializedLambda.getImplMethodName() + " cannot has >=1 parameters");
                descriptors[i] = implMethodSignature.substring(2);
            }
            namesAndDescriptors[0] = names;
            namesAndDescriptors[1] = descriptors;
        }
        return namesAndDescriptors;
    }
}
