package me.z7087.final2constant.util;

import java.io.Serializable;
import java.lang.invoke.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public final class JavaHelper {
    private JavaHelper() {}

    public static final int CACHED_JAVA_VERSION = getJavaVersionFromRuntime();

    private static final MethodHandle MHS_PRIVATE_LOOKUP_IN_MH;
    static {
        MethodHandle _methodHandlesPrivateLookupInMH;
        try {
            _methodHandlesPrivateLookupInMH = MethodHandles.publicLookup().findStatic(MethodHandles.class,
                    "privateLookupIn",
                    MethodType.methodType(MethodHandles.Lookup.class, Class.class, MethodHandles.Lookup.class)
            );
        } catch (NoSuchMethodException e) {
            _methodHandlesPrivateLookupInMH = null;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        MHS_PRIVATE_LOOKUP_IN_MH = _methodHandlesPrivateLookupInMH;
    }

    public static int getJavaVersionFromRuntime() {
        int version;
        try {
            final Object versionObj = Objects.requireNonNull(Runtime.class.getMethod("version").invoke(null, (Object[]) null));
            try {
                version = (int) versionObj.getClass().getMethod("major").invoke(versionObj, (Object[]) null);
            } catch (NoSuchMethodException | InvocationTargetException e) {
                version = (int) versionObj.getClass().getMethod("feature").invoke(versionObj, (Object[]) null);
            }
        } catch (NoSuchMethodException | InvocationTargetException e) {
            version = 8;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return version;
    }

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
                final SerializedLambda serializedLambda;
                if (MHS_PRIVATE_LOOKUP_IN_MH != null) {
                    final Class<?> getterRefClass = getterRef.getClass();
                    final MethodHandles.Lookup newLookup = (MethodHandles.Lookup) MHS_PRIVATE_LOOKUP_IN_MH.invokeExact(getterRefClass, lookup);
                    final Object object = newLookup.findVirtual(
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
                    serializedLambda = (SerializedLambda) object;
                } else {
                    Method writeReplaceMethod = getterRef.getClass().getDeclaredMethod("writeReplace", (Class<?>[]) null);
                    writeReplaceMethod.setAccessible(true);
                    serializedLambda = (SerializedLambda) writeReplaceMethod.invoke(getterRef);
                }
                names[i] = serializedLambda.getImplMethodName();
                final String implMethodSignature = serializedLambda.getImplMethodSignature();
                if (!implMethodSignature.startsWith("()")) {
                    if (serializedLambda.getImplMethodKind() == MethodHandleInfo.REF_invokeStatic
                            && serializedLambda.getCapturedArgCount() == 1
                    ) {
                        throw new IllegalArgumentException("Method " + serializedLambda.getImplMethodName() + " is likely a lambda wrapper for the real getter, we don't know how to unwrap it");
                    }
                    throw new IllegalArgumentException("Getter " + serializedLambda.getImplMethodName() + " cannot has >=1 parameters");
                }
                descriptors[i] = implMethodSignature.substring(2);
            }
            namesAndDescriptors[0] = names;
            namesAndDescriptors[1] = descriptors;
        }
        return namesAndDescriptors;
    }

    @Deprecated
    public static MethodHandle eventBusMH(Class<?> eventClass,
                                          Class<?> resultClass,
                                          MethodHandle[] handlers,
                                          MethodHandle defaultResultSupplier,
                                          MethodHandle shouldBreakTest,
                                          Class<? extends Throwable> exceptionType,
                                          MethodHandle exceptionHandler
    ) {
        defaultResultSupplier = defaultResultSupplier.asType(MethodType.methodType(resultClass)); // ()resultClass
        // if no handlers, just return (event) -> defaultResultSupplier.get();
        if (handlers == null || handlers.length == 0) {
            return MethodHandles.dropArguments(defaultResultSupplier, 0, eventClass);
        }
        shouldBreakTest = shouldBreakTest.asType(MethodType.methodType(boolean.class, resultClass));
        if (exceptionHandler != null) {
            // should be (exceptionType, eventClass)resultClass at final
            final MethodType exceptionHandlerMT = exceptionHandler.type();
            switch (exceptionHandlerMT.parameterCount()) {
                case 0:
                    // ()?
                    exceptionHandler = MethodHandles.dropArguments(exceptionHandler, 0, exceptionType, eventClass);
                    // (exceptionType, eventClass)?
                    break;
                case 1:
                    // (?)?
                    if (exceptionHandlerMT.parameterType(0).isAssignableFrom(exceptionType)) {
                        // (? extends exceptionType)?
                        exceptionHandler = exceptionHandler.asType(exceptionHandlerMT.changeParameterType(0, exceptionType));
                        // (exceptionType)?
                        exceptionHandler = MethodHandles.dropArguments(exceptionHandler, 1, eventClass);
                        // (exceptionType, eventClass)?
                    } else {
                        exceptionHandler = exceptionHandler.asType(exceptionHandlerMT.changeParameterType(0, eventClass));
                        // (eventClass)?
                        exceptionHandler = MethodHandles.dropArguments(exceptionHandler, 0, exceptionType);
                        // (exceptionType, eventClass)?
                    }
                    break;
                case 2:
                    // (?, ?)?
                    exceptionHandler = exceptionHandler.asType(MethodType.methodType(exceptionHandlerMT.returnType(), exceptionType, eventClass));
                    // (exceptionType, eventClass)?
                    break;
                default:
                    throw new IllegalArgumentException("don't know how to convert non-null exception handler with " + exceptionHandlerMT.parameterCount() + " args");
            }
            // (exceptionType, eventClass)?
            if (exceptionHandlerMT.returnType() != void.class) {
                exceptionHandler = exceptionHandler.asType(exceptionHandler.type().changeReturnType(resultClass));
                // (exceptionType, eventClass)resultClass
            } else {
                // (exceptionType, eventClass)V
                exceptionHandler = MethodHandles.filterReturnValue(
                        exceptionHandler,
                        defaultResultSupplier // ()resultClass
                );
                // (exceptionType, eventClass)resultClass
            }
        }
        final MethodType handlerMT = MethodType.methodType(resultClass, eventClass);
        final MethodHandle resultClassIdentityMH = MethodHandles.identity(resultClass); // (resultClass)resultClass
        final MethodHandle resultClassIdentityUnusedEventClassMH = MethodHandles.dropArguments(resultClassIdentityMH, 1, eventClass); // (resultClass, eventClass)resultClass
        MethodHandle finalHandlerMH = null; // (eventClass)resultClass
        for (int i = handlers.length - 1; i >= 0; --i) {
            MethodHandle handler = handlers[i];
            handler = handler.asType(handlerMT); // (eventClass)resultClass
            if (exceptionHandler != null)
                handler = MethodHandles.catchException( // (eventClass)resultClass
                        handler, // (eventClass)resultClass
                        exceptionType,
                        exceptionHandler // (exceptionType, eventClass)resultClass
                );
            if (finalHandlerMH != null) {
                final MethodHandle tester = MethodHandles.guardWithTest( // (resultClass, eventClass)resultClass
                        shouldBreakTest, // (resultClass)Z
                        resultClassIdentityUnusedEventClassMH, // (resultClass, eventClass)resultClass
                        MethodHandles.dropArguments(finalHandlerMH, 0, resultClass) // (resultClass, eventClass)resultClass
                );
                finalHandlerMH = MethodHandles.foldArguments( // (eventClass)resultClass
                        tester, // (resultClass, eventClass)resultClass
                        handler // (eventClass)resultClass
                );
            } else {
                finalHandlerMH = handler;
            }
        }
        return finalHandlerMH;
    }
}
