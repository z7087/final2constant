package me.z7087.final2constant;

import me.z7087.final2constant.primitives.*;
import me.z7087.final2constant.util.StringReuseHelper;
import org.objectweb.asm.*;

import java.lang.invoke.*;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;

public abstract class ConstantFactory {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    protected abstract Class<?> defineClassAt(MethodHandles.Lookup hostClass, byte[] classBytes) throws Throwable;
    protected abstract Class<?> defineClassWithPrivilegeAt(MethodHandles.Lookup hostClass, byte[] classBytes) throws Throwable;

    public abstract <T> Constant<T> of(T value);

    public abstract <T> DynamicConstant<T> ofMutable(T value);

    public abstract <T> DynamicConstant<T> ofVolatile(T value);

    public abstract <T> Constant<T> ofLazy(Supplier<? extends T> supplier);

    public <T> MethodHandle ofRecordConstructor(MethodHandles.Lookup hostClass,
                                                Class<T> recordInterfaceClass,
                                                String[] recordImmutableArgMethodNames,
                                                String[] recordImmutableArgMethodTypes
    ) {
        return this.ofRecordConstructor(hostClass, recordInterfaceClass, recordImmutableArgMethodNames, recordImmutableArgMethodTypes, true, false);
    }

    public <T> MethodHandle ofRecordConstructor(MethodHandles.Lookup hostClass,
                                                Class<T> recordInterfaceClass,
                                                String[] recordImmutableArgMethodNames,
                                                String[] recordImmutableArgMethodTypes,
                                                boolean generateToStringHashCodeEquals,
                                                boolean generateSetterForFinalFields
    ) {
        return this.ofRecordConstructor(hostClass, recordInterfaceClass, true, recordImmutableArgMethodNames, recordImmutableArgMethodTypes, null, null, generateToStringHashCodeEquals, generateSetterForFinalFields);
    }

    public <T> MethodHandle ofRecordConstructor(MethodHandles.Lookup hostClass,
                                                Class<T> recordAbstractOrInterfaceClass,
                                                boolean useInterface,
                                                String[] recordImmutableArgMethodNames,
                                                String[] recordImmutableArgMethodTypes,
                                                String[] recordMutableArgMethodNames,
                                                String[] recordMutableArgMethodTypes,
                                                boolean generateToStringHashCodeEquals,
                                                boolean generateSetterForFinalFields
    ) {
        if (recordImmutableArgMethodNames == null) recordImmutableArgMethodNames = EMPTY_STRING_ARRAY;
        if (recordImmutableArgMethodTypes == null) recordImmutableArgMethodTypes = EMPTY_STRING_ARRAY;
        if (recordMutableArgMethodNames == null) recordMutableArgMethodNames = EMPTY_STRING_ARRAY;
        if (recordMutableArgMethodTypes == null) recordMutableArgMethodTypes = EMPTY_STRING_ARRAY;
        final String simpleClassName = recordAbstractOrInterfaceClass.getSimpleName() + "$RecordImpl";
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(
                    hostClass,
                    generateRecordImpl(
                            hostClass.lookupClass().getName().replace('.', '/') + "$$" + simpleClassName,
                            simpleClassName,
                            recordAbstractOrInterfaceClass,
                            useInterface,
                            recordImmutableArgMethodNames,
                            recordImmutableArgMethodTypes,
                            recordMutableArgMethodNames,
                            recordMutableArgMethodTypes,
                            generateToStringHashCodeEquals,
                            generateSetterForFinalFields
                    )
            );
            final MethodHandle MHGetConstructorMH = hostClass.findStatic(clazz, "getConstructorMH", MethodType.methodType(MethodHandle.class, int.class));
            return (MethodHandle) MHGetConstructorMH.invokeExact(0);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T ofEmptyInterfaceImplInstance(MethodHandles.Lookup hostClass,
                                              Class<T> interfaceClass
    ) {
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(
                    hostClass,
                    generateEmptyImpl(
                            hostClass.lookupClass().getName().replace('.', '/') + "$$" + interfaceClass.getSimpleName() + "$EmptyImpl",
                            true,
                            interfaceClass
                    )
            );
            final MethodHandle ConstructorMH = hostClass.findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(Object.class));
            return interfaceClass.cast(ConstructorMH.invokeExact());
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T ofEmptyAbstractImplInstance(MethodHandles.Lookup hostClass,
                                             Class<T> abstractClass
    ) {
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(
                    hostClass,
                    generateEmptyImpl(
                            hostClass.lookupClass().getName().replace('.', '/') + "$$" + abstractClass.getSimpleName() + "$EmptyImpl",
                            false,
                            abstractClass
                    )
            );
            final MethodHandle ConstructorMH = hostClass.findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(Object.class));
            return abstractClass.cast(ConstructorMH.invokeExact());
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> DynamicConstant<T> ofBase(T value) {
        try {
            final Class<?> clazz = defineClassAt(MethodHandles.lookup(), generateConstantBaseImpl(ConstantFactory.class.getName().replace('.', '/') + "$ConstantBaseImpl"));
            final MethodHandle constructorMH = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, Object.class)).asType(MethodType.methodType(DynamicConstant.class, Object.class));
            //noinspection unchecked
            return (DynamicConstant<T>) constructorMH.invokeExact((Object) value);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T makeBase(
            MethodHandles.Lookup hostClass,
            Method delegateMethod,
            MethodHandle mh,
            boolean sneakyThrows
    ) {
        @SuppressWarnings("unchecked")
        final Class<T> delegateAbstractOrInterfaceClass = (Class<T>) delegateMethod.getDeclaringClass();
        return makeBase(
                hostClass,
                delegateAbstractOrInterfaceClass,
                delegateAbstractOrInterfaceClass.isInterface(),
                delegateMethod.getName(),
                mh,
                sneakyThrows
        );
    }

    public <T> T makeBase(
            MethodHandles.Lookup hostClass,
            Method delegateMethod,
            MethodHandle mh,
            String setterMethodName,
            boolean sneakyThrows
    ) {
        @SuppressWarnings("unchecked")
        final Class<T> delegateAbstractOrInterfaceClass = (Class<T>) delegateMethod.getDeclaringClass();
        return makeBase(
                hostClass,
                delegateAbstractOrInterfaceClass,
                delegateAbstractOrInterfaceClass.isInterface(),
                delegateMethod.getName(),
                mh,
                setterMethodName,
                sneakyThrows
        );
    }

    public <T> T makeBase(
            MethodHandles.Lookup hostClass,
            Class<T> delegateAbstractOrInterfaceClass,
            boolean useInterface,
            String delegateMethodName,
            MethodHandle mh,
            boolean sneakyThrows
    ) {
        return makeBase(
                hostClass,
                delegateAbstractOrInterfaceClass,
                useInterface,
                delegateMethodName,
                mh,
                null,
                sneakyThrows
        );
    }

    public <T> T makeBase(
            MethodHandles.Lookup hostClass,
            Class<T> delegateAbstractOrInterfaceClass,
            boolean useInterface,
            String delegateMethodName,
            MethodHandle mh,
            String setterMethodName,
            boolean sneakyThrows
    ) {
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(hostClass, generateMethodHandleBaseImpl(hostClass.lookupClass().getName().replace('.', '/') + "$MethodHandleBaseImpl", delegateAbstractOrInterfaceClass, useInterface, delegateMethodName, mh.type(), setterMethodName, sneakyThrows));
            final MethodHandle constructorMH = hostClass.findConstructor(clazz, MethodType.methodType(void.class, MethodHandle.class)).asType(MethodType.methodType(Object.class, MethodHandle.class));
            return delegateAbstractOrInterfaceClass.cast((Object) constructorMH.invokeExact(mh));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public MethodHandle ofArrayConstructor(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("array size too small: " + size);
        }
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(MethodHandles.lookup(), generateArrayImpl(ConstantFactory.class.getName().replace('.', '/') + "$ArrayImpl", size));
            return MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(ConstantArray.class));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public MethodHandle ofPrimitiveArrayConstructor(int size, Class<?> primitiveType) {
        if (size <= 0) {
            throw new IllegalArgumentException("array size too small: " + size);
        }
        if (!primitiveType.isPrimitive() || primitiveType == void.class) {
            throw new IllegalArgumentException("don't know how to use type as primitive array: " + primitiveType);
        }
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(MethodHandles.lookup(), generatePrimitiveArrayImpl(ConstantFactory.class.getName().replace('.', '/') + "$" + getPrimitiveTypeName(primitiveType) + "ArrayImpl", size, primitiveType));
            return MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(AbstractPrimitiveConstantArray.class));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> ConstantArray<T> ofArrayBase(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("array size too small: " + size);
        }
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(MethodHandles.lookup(), generateArrayBaseImpl(ConstantFactory.class.getName().replace('.', '/') + "$ArrayBaseImpl", size));
            final MethodHandle constructorMH = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(ConstantArray.class));
            @SuppressWarnings("unchecked")
            final ConstantArray<T> result = (ConstantArray<T>) constructorMH.invokeExact();
            return result;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public <T> AbstractPrimitiveConstantArray<T> ofPrimitiveArrayBase(int size, Class<?> primitiveType) {
        if (size <= 0) {
            throw new IllegalArgumentException("array size too small: " + size);
        }
        if (!primitiveType.isPrimitive() || primitiveType == void.class) {
            throw new IllegalArgumentException("don't know how to use type as primitive array: " + primitiveType);
        }
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(MethodHandles.lookup(), generatePrimitiveArrayBaseImpl(ConstantFactory.class.getName().replace('.', '/') + "$" + getPrimitiveTypeName(primitiveType) + "ArrayBaseImpl", size, primitiveType));
            final MethodHandle constructorMH = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(AbstractPrimitiveConstantArray.class));
            @SuppressWarnings("unchecked")
            final AbstractPrimitiveConstantArray<T> result = (AbstractPrimitiveConstantArray<T>) constructorMH.invokeExact();
            return result;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    // super slow for unknown reason
    @Deprecated
    public <T, E, R, TE extends Throwable> T ofEventBus(MethodHandles.Lookup hostClass,
                                                                 Class<T> interfaceEventBusClass,
                                                                 String interfaceMethodName,
                                                                 MethodType interfaceMethodType,
                                                                 Class<E> eventClass,
                                                                 Class<R> resultClass,
                                                                 MethodHandle[] handlers, // (E)R
                                                                 MethodHandle defaultResultSupplier, // ()R
                                                                 MethodHandle shouldBreakTest, // (R)boolean
                                                                 Class<TE> exceptionType,
                                                                 MethodHandle exceptionHandler, // (TE, E)R
                                                                 boolean handleExceptionForEveryHandler,
                                                                 int maxMethodBytecodeSize
    ) {
        try {
            final Class<?> clazz = defineClassWithPrivilegeAt(
                    hostClass,
                    generateEventBusImplParts(
                            hostClass.lookupClass().getName().replace('.', '/') + "$$" + interfaceEventBusClass.getSimpleName() + "$EventBusImpl",
                            interfaceEventBusClass,
                            interfaceMethodName,
                            interfaceMethodType,
                            eventClass,
                            resultClass,
                            handlers.length,
                            exceptionType,
                            handleExceptionForEveryHandler,
                            maxMethodBytecodeSize
                    )
            );
            MethodType constructorMT = MethodType.methodType(void.class, MethodHandle[].class);
            ArrayList<Object> constructorArgs = new ArrayList<>();
            {
                final int handlerCount = handlers.length;
                MethodHandle[] newHandlers = new MethodHandle[handlerCount];
                final MethodType handlerType = MethodType.methodType(resultClass, eventClass);
                for (int i = 0; i < handlerCount; ++i) {
                    newHandlers[i] = handlers[i].asType(handlerType);
                }
                constructorArgs.add(newHandlers);
            }
            {
                int mhArgCount = 0;
                if (resultClass != void.class) {
                    mhArgCount += 2;
                    constructorArgs.add(defaultResultSupplier.asType(MethodType.methodType(resultClass)));
                    constructorArgs.add(shouldBreakTest.asType(MethodType.methodType(boolean.class, resultClass)));
                }
                if (exceptionType != null) {
                    mhArgCount += 1;
                    constructorArgs.add(exceptionHandler.asType(MethodType.methodType(resultClass, exceptionType, eventClass)));
                }
                if (mhArgCount != 0) {
                    Class<?>[] appendArgs = new Class[mhArgCount];
                    for (int i = 0; i < mhArgCount; ++i) {
                        appendArgs[i] = MethodHandle.class;
                    }
                    constructorMT = constructorMT.appendParameterTypes(appendArgs);
                }
            }
            final MethodHandle ConstructorMH = hostClass.findConstructor(clazz, constructorMT).asType(constructorMT.changeReturnType(Object.class));
            return interfaceEventBusClass.cast(ConstructorMH.invokeWithArguments(constructorArgs));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    protected static byte[] generateConstantImpl(String className) {
        // no lazy so prob a final field is enough
        final ClassWriter cwConstantImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String ConstantClassName = Constant.class.getName().replace('.', '/');
        cwConstantImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                "<T:Ljava/lang/Object;>Ljava/lang/Object;L" + ConstantClassName + "<TT;>;",
                "java/lang/Object",
                new String[] {
                        ConstantClassName
                });
        {
            final FieldVisitor fvValue = cwConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "value", "Ljava/lang/Object;", "TT;", null);
            fvValue.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwConstantImpl.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", "(TT;)V", null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitVarInsn(ALOAD, 1);
            mvInit.visitFieldInsn(PUTFIELD, className, "value", "Ljava/lang/Object;");

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(2, 2);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwConstantImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get", "()Ljava/lang/Object;", "()TT;", null);
            mvGetter.visitCode();
            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "value", "Ljava/lang/Object;");
            mvGetter.visitInsn(ARETURN);
            mvGetter.visitMaxs(1, 1);
            mvGetter.visitEnd();
        }
        cwConstantImpl.visitEnd();
        return cwConstantImpl.toByteArray();
    }

    protected static byte[] generateLazyConstantImpl(String className) {
        final ClassWriter cwLazyConstantImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String ConstantClassName = Constant.class.getName().replace('.', '/');
        cwLazyConstantImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                "<T:Ljava/lang/Object;>Ljava/lang/Object;L" + ConstantClassName + "<TT;>;",
                "java/lang/Object",
                new String[] {
                        ConstantClassName
                });

        {
            final FieldVisitor fvAlwaysFalse = cwLazyConstantImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "ALWAYS_FALSE", "Ljava/lang/invoke/MethodHandle;", null, null);
            fvAlwaysFalse.visitEnd();
        }
        {
            final FieldVisitor fvAlwaysTrue = cwLazyConstantImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "ALWAYS_TRUE", "Ljava/lang/invoke/MethodHandle;", null, null);
            fvAlwaysTrue.visitEnd();
        }
        {
            final FieldVisitor fvSupplier = cwLazyConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "supplier", "Ljava/util/function/Supplier;", "Ljava/util/function/Supplier<+TT;>;", null);
            fvSupplier.visitEnd();
        }
        {
            final FieldVisitor fvValueCache = cwLazyConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "valueCache", "Ljava/lang/invoke/MutableCallSite;", null, null);
            fvValueCache.visitEnd();
        }
        {
            final FieldVisitor fvValueCacheInvoker = cwLazyConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "valueCacheInvoker", "Ljava/lang/invoke/MethodHandle;", null, null);
            fvValueCacheInvoker.visitEnd();
        }
        {
            final FieldVisitor fvInitLock = cwLazyConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "initializedLock", "Ljava/lang/invoke/MutableCallSite;", null, null);
            fvInitLock.visitEnd();
        }
        {
            final FieldVisitor fvInitLockInvoker = cwLazyConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "initializedLockInvoker", "Ljava/lang/invoke/MethodHandle;", null, null);
            fvInitLockInvoker.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwLazyConstantImpl.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/util/function/Supplier;)V", "(Ljava/util/function/Supplier<+TT;>;)V", null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
            mvInit.visitInsn(DUP);
            mvInit.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
            mvInit.visitInsn(ACONST_NULL);
            mvInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
            mvInit.visitFieldInsn(PUTFIELD, className, "valueCache", "Ljava/lang/invoke/MutableCallSite;");

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitInsn(DUP);
            mvInit.visitFieldInsn(GETFIELD, className, "valueCache", "Ljava/lang/invoke/MutableCallSite;");
            mvInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MutableCallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
            mvInit.visitFieldInsn(PUTFIELD, className, "valueCacheInvoker", "Ljava/lang/invoke/MethodHandle;");

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitTypeInsn(NEW, "java/lang/invoke/MutableCallSite");
            mvInit.visitInsn(DUP);
            mvInit.visitFieldInsn(GETSTATIC, className, "ALWAYS_FALSE", "Ljava/lang/invoke/MethodHandle;");
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/MutableCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
            mvInit.visitFieldInsn(PUTFIELD, className, "initializedLock", "Ljava/lang/invoke/MutableCallSite;");

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitInsn(DUP);
            mvInit.visitFieldInsn(GETFIELD, className, "initializedLock", "Ljava/lang/invoke/MutableCallSite;");
            mvInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MutableCallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
            mvInit.visitFieldInsn(PUTFIELD, className, "initializedLockInvoker", "Ljava/lang/invoke/MethodHandle;");

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitVarInsn(ALOAD, 1);
            mvInit.visitFieldInsn(PUTFIELD, className, "supplier", "Ljava/util/function/Supplier;");

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(5, 2);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwLazyConstantImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get", "()Ljava/lang/Object;", "()TT;", null);
            mvGetter.visitCode();

            Label MonitorAfterEnter = new Label();
            Label MonitorAfterExit = new Label();
            Label MonitorThrowHandler = new Label();
            Label MonitorThrowHandlerEnd = new Label();
            // cache all throwable and rethrow them after monitor exit
            mvGetter.visitTryCatchBlock(MonitorAfterEnter, MonitorAfterExit, MonitorThrowHandler, null);
            // if throwable throws during monitor exit, go infinite retry until no more throwable, and throw last cached
            mvGetter.visitTryCatchBlock(MonitorThrowHandler, MonitorThrowHandlerEnd, MonitorThrowHandler, null);

            Label MHLockInvokeStart = new Label();
            Label MHLockInvokeEnd = new Label();
            Label MonitorAfterExitRethrowStart = new Label();
            Label MonitorAfterExitRethrowEnd = new Label();
            Label MHValueCacheInvokeStart = new Label();
            Label MHValueCacheInvokeEnd = new Label();
            Label UncheckedCatchStart = new Label();
            Label CheckedCatchStart = new Label();
            mvGetter.visitTryCatchBlock(MHLockInvokeStart, MHLockInvokeEnd, UncheckedCatchStart, "java/lang/RuntimeException");
            mvGetter.visitTryCatchBlock(MHLockInvokeStart, MHLockInvokeEnd, UncheckedCatchStart, "java/lang/Error");
            mvGetter.visitTryCatchBlock(MHLockInvokeStart, MHLockInvokeEnd, CheckedCatchStart, "java/lang/Throwable");
            mvGetter.visitTryCatchBlock(MonitorAfterExitRethrowStart, MonitorAfterExitRethrowEnd, UncheckedCatchStart, "java/lang/RuntimeException");
            mvGetter.visitTryCatchBlock(MonitorAfterExitRethrowStart, MonitorAfterExitRethrowEnd, UncheckedCatchStart, "java/lang/Error");
            mvGetter.visitTryCatchBlock(MonitorAfterExitRethrowStart, MonitorAfterExitRethrowEnd, CheckedCatchStart, "java/lang/Throwable");
            mvGetter.visitTryCatchBlock(MHValueCacheInvokeStart, MHValueCacheInvokeEnd, UncheckedCatchStart, "java/lang/RuntimeException");
            mvGetter.visitTryCatchBlock(MHValueCacheInvokeStart, MHValueCacheInvokeEnd, UncheckedCatchStart, "java/lang/Error");
            mvGetter.visitTryCatchBlock(MHValueCacheInvokeStart, MHValueCacheInvokeEnd, CheckedCatchStart, "java/lang/Throwable");

            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "initializedLockInvoker", "Ljava/lang/invoke/MethodHandle;");
            mvGetter.visitLabel(MHLockInvokeStart);
            mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Z", false);
            mvGetter.visitLabel(MHLockInvokeEnd);
            Label AfterInitCheck = new Label();
            mvGetter.visitJumpInsn(IFNE, AfterInitCheck);

            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "initializedLockInvoker", "Ljava/lang/invoke/MethodHandle;");
            mvGetter.visitInsn(DUP);
            mvGetter.visitVarInsn(ASTORE, 1);
            mvGetter.visitInsn(MONITORENTER);

            mvGetter.visitLabel(MonitorAfterEnter);

            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "initializedLockInvoker", "Ljava/lang/invoke/MethodHandle;");
            mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Z", false);
            Label MonitorNormalExit = new Label();
            mvGetter.visitJumpInsn(IFNE, MonitorNormalExit);

            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "valueCache", "Ljava/lang/invoke/MutableCallSite;");
            mvGetter.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "supplier", "Ljava/util/function/Supplier;");
            mvGetter.visitMethodInsn(INVOKEINTERFACE, "java/util/function/Supplier", "get", "()Ljava/lang/Object;", true);
            mvGetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
            mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);

            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "initializedLock", "Ljava/lang/invoke/MutableCallSite;");
            mvGetter.visitFieldInsn(GETSTATIC, className, "ALWAYS_TRUE", "Ljava/lang/invoke/MethodHandle;");
            mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MutableCallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);

            mvGetter.visitInsn(ICONST_2);
            mvGetter.visitTypeInsn(ANEWARRAY, "java/lang/invoke/MutableCallSite");
            mvGetter.visitInsn(DUP);
            mvGetter.visitInsn(ICONST_0);
            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "valueCache", "Ljava/lang/invoke/MutableCallSite;");
            mvGetter.visitInsn(AASTORE);
            mvGetter.visitInsn(DUP);
            mvGetter.visitInsn(ICONST_1);
            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "initializedLock", "Ljava/lang/invoke/MutableCallSite;");
            mvGetter.visitInsn(AASTORE);
            mvGetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MutableCallSite", "syncAll", "([Ljava/lang/invoke/MutableCallSite;)V", false);

            mvGetter.visitLabel(MonitorNormalExit);

            mvGetter.visitVarInsn(ALOAD, 1);
            mvGetter.visitInsn(MONITOREXIT);

            mvGetter.visitLabel(MonitorAfterExit);
            mvGetter.visitJumpInsn(GOTO, AfterInitCheck);

            mvGetter.visitLabel(MonitorThrowHandler);
            mvGetter.visitVarInsn(ASTORE, 2);
            mvGetter.visitVarInsn(ALOAD, 1);
            mvGetter.visitInsn(MONITOREXIT);
            mvGetter.visitLabel(MonitorThrowHandlerEnd);

            mvGetter.visitVarInsn(ALOAD, 2);
            mvGetter.visitLabel(MonitorAfterExitRethrowStart);
            mvGetter.visitInsn(ATHROW);
            mvGetter.visitLabel(MonitorAfterExitRethrowEnd);

            mvGetter.visitLabel(AfterInitCheck);

            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "valueCacheInvoker", "Ljava/lang/invoke/MethodHandle;");
            mvGetter.visitLabel(MHValueCacheInvokeStart);
            mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Ljava/lang/Object;", false);
            mvGetter.visitLabel(MHValueCacheInvokeEnd);
            mvGetter.visitInsn(ARETURN);

            mvGetter.visitLabel(CheckedCatchStart);
            mvGetter.visitVarInsn(ASTORE, 1);
            mvGetter.visitTypeInsn(NEW, "java/lang/RuntimeException");
            mvGetter.visitInsn(DUP);
            mvGetter.visitVarInsn(ALOAD, 1);
            mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
            mvGetter.visitLabel(UncheckedCatchStart);
            mvGetter.visitInsn(ATHROW);

            mvGetter.visitMaxs(4, 3);
            mvGetter.visitEnd();
        }
        {
            final MethodVisitor mvClassInit = cwLazyConstantImpl.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mvClassInit.visitCode();

            mvClassInit.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
            mvClassInit.visitInsn(ICONST_0);
            mvClassInit.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            mvClassInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
            mvClassInit.visitFieldInsn(PUTSTATIC, className, "ALWAYS_FALSE", "Ljava/lang/invoke/MethodHandle;");

            mvClassInit.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
            mvClassInit.visitInsn(ICONST_0);
            mvClassInit.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            mvClassInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
            mvClassInit.visitFieldInsn(PUTSTATIC, className, "ALWAYS_TRUE", "Ljava/lang/invoke/MethodHandle;");

            mvClassInit.visitInsn(RETURN);
            mvClassInit.visitMaxs(2, 0);
            mvClassInit.visitEnd();
        }
        cwLazyConstantImpl.visitEnd();

        return cwLazyConstantImpl.toByteArray();
    }

    protected static byte[] generateDynamicConstantImpl(String className, boolean canSync) {
        final ClassWriter cwDynamicConstantImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String DynamicConstantClassName = DynamicConstant.class.getName().replace('.', '/');
        cwDynamicConstantImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                "<T:Ljava/lang/Object;>Ljava/lang/Object;L" + DynamicConstantClassName + "<TT;>;",
                "java/lang/Object",
                new String[] {
                        DynamicConstantClassName
                });
        {
            final FieldVisitor fvCallSite = cwDynamicConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "callSite", "Ljava/lang/invoke/CallSite;", null, null);
            fvCallSite.visitEnd();
        }
        {
            final FieldVisitor fvDynamicInvoker = cwDynamicConstantImpl.visitField(ACC_PRIVATE | ACC_FINAL, "dynamicInvoker", "Ljava/lang/invoke/MethodHandle;", null, null);
            fvDynamicInvoker.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwDynamicConstantImpl.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/invoke/CallSite;)V", null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitVarInsn(ALOAD, 1);
            mvInit.visitFieldInsn(PUTFIELD, className, "callSite", "Ljava/lang/invoke/CallSite;");

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitVarInsn(ALOAD, 1);
            mvInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
            mvInit.visitFieldInsn(PUTFIELD, className, "dynamicInvoker", "Ljava/lang/invoke/MethodHandle;");

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(2, 2);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwDynamicConstantImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get", "()Ljava/lang/Object;", "()TT;", null);
            mvGetter.visitCode();

            Label tryStart = new Label();
            Label tryEnd = new Label();
            Label UncheckedCatchStart = new Label();
            Label CheckedCatchStart = new Label();
            mvGetter.visitTryCatchBlock(tryStart, tryEnd, UncheckedCatchStart, "java/lang/RuntimeException");
            mvGetter.visitTryCatchBlock(tryStart, tryEnd, UncheckedCatchStart, "java/lang/Error");
            mvGetter.visitTryCatchBlock(tryStart, tryEnd, CheckedCatchStart, "java/lang/Throwable");

            mvGetter.visitVarInsn(ALOAD, 0);
            mvGetter.visitFieldInsn(GETFIELD, className, "dynamicInvoker", "Ljava/lang/invoke/MethodHandle;");
            mvGetter.visitLabel(tryStart);
            mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Ljava/lang/Object;", false);
            mvGetter.visitLabel(tryEnd);
            mvGetter.visitInsn(ARETURN);

            mvGetter.visitLabel(CheckedCatchStart);
            mvGetter.visitVarInsn(ASTORE, 1);
            mvGetter.visitTypeInsn(NEW, "java/lang/RuntimeException");
            mvGetter.visitInsn(DUP);
            mvGetter.visitVarInsn(ALOAD, 1);
            mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
            mvGetter.visitLabel(UncheckedCatchStart);
            mvGetter.visitInsn(ATHROW);

            mvGetter.visitMaxs(3, 2);
            mvGetter.visitEnd();
        }
        {
            final MethodVisitor mvSetter = cwDynamicConstantImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "set", "(Ljava/lang/Object;)V", "(TT;)V", null);
            mvSetter.visitCode();

            mvSetter.visitVarInsn(ALOAD, 0);
            mvSetter.visitFieldInsn(GETFIELD, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvSetter.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
            mvSetter.visitVarInsn(ALOAD, 1);
            mvSetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
            mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);
            mvSetter.visitInsn(RETURN);

            mvSetter.visitMaxs(3, 2);
            mvSetter.visitEnd();
        }
        if (canSync) {
            final MethodVisitor mvSync = cwDynamicConstantImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "sync", "()V", null, null);
            mvSync.visitCode();

            mvSync.visitInsn(ICONST_1);
            mvSync.visitTypeInsn(ANEWARRAY, "java/lang/invoke/MutableCallSite");
            mvSync.visitInsn(DUP);
            mvSync.visitInsn(ICONST_0);
            mvSync.visitVarInsn(ALOAD, 0);
            mvSync.visitFieldInsn(GETFIELD, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvSync.visitTypeInsn(CHECKCAST, "java/lang/invoke/MutableCallSite");
            mvSync.visitInsn(AASTORE);
            mvSync.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MutableCallSite", "syncAll", "([Ljava/lang/invoke/MutableCallSite;)V", false);

            mvSync.visitInsn(RETURN);
            mvSync.visitMaxs(4, 1);
            mvSync.visitEnd();
        }
        cwDynamicConstantImpl.visitEnd();
        return cwDynamicConstantImpl.toByteArray();
    }
    
    protected static <T> byte[] generateRecordImpl(String className,
                                                   String simpleClassName,
                                                   Class<T> recordAbstractOrInterfaceClass,
                                                   boolean useInterface,
                                                   String[] recordImmutableArgMethodNames,
                                                   String[] recordImmutableArgMethodReturnTypes,
                                                   String[] recordMutableArgMethodNames,
                                                   String[] recordMutableArgMethodReturnTypes,
                                                   boolean generateToStringHashCodeEquals,
                                                   boolean generateSetterForFinalFields
    ) {
        final int recordImmutableArgCount = recordImmutableArgMethodNames.length;
        final int recordMutableArgCount = recordMutableArgMethodNames.length;
        final ClassWriter cwRecordImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String recordAbstractOrInterfaceClassName = recordAbstractOrInterfaceClass.getName().replace('.', '/');
        cwRecordImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                null,
                useInterface ? "java/lang/Object" : recordAbstractOrInterfaceClassName,
                useInterface ? new String[] { recordAbstractOrInterfaceClassName } : null);
        generateRecordImpl_visitImmutableFields(cwRecordImpl, recordImmutableArgCount, recordImmutableArgMethodNames, recordImmutableArgMethodReturnTypes);
        generateRecordImpl_visitMutableFields(cwRecordImpl, recordMutableArgCount, recordMutableArgMethodNames, recordMutableArgMethodReturnTypes);
        // no need to init mutable fields in the constructor
        generateRecordImpl_visitInitAndGetConstructorMH(
                cwRecordImpl,
                className,
                recordAbstractOrInterfaceClass,
                useInterface,
                recordImmutableArgCount,
                recordImmutableArgMethodNames,
                recordImmutableArgMethodReturnTypes
        );
        for (int i = 0; i < recordImmutableArgCount; ++i) {
            final String recordArgMethodName = recordImmutableArgMethodNames[i];
            final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[i];
            generateRecordImpl_visitGetter(cwRecordImpl, className, recordArgMethodName, recordArgMethodReturnType);
            if (generateSetterForFinalFields)
                generateRecordImpl_visitSetter(cwRecordImpl, className, recordArgMethodName, recordArgMethodReturnType);
        }
        for (int i = 0; i < recordMutableArgCount; ++i) {
            final String recordArgMethodName = recordMutableArgMethodNames[i];
            final String recordArgMethodReturnType = recordMutableArgMethodReturnTypes[i];
            generateRecordImpl_visitGetter(cwRecordImpl, className, recordArgMethodName, recordArgMethodReturnType);
            generateRecordImpl_visitSetter(cwRecordImpl, className, recordArgMethodName, recordArgMethodReturnType);
        }
        if (generateToStringHashCodeEquals)
            generateRecordImpl_generateToStringHashCodeEquals(
                    cwRecordImpl,
                    className,
                    simpleClassName,
                    recordImmutableArgCount, recordImmutableArgMethodNames, recordImmutableArgMethodReturnTypes,
                    recordMutableArgCount, recordMutableArgMethodNames, recordMutableArgMethodReturnTypes
            );
        cwRecordImpl.visitEnd();
        return cwRecordImpl.toByteArray();
    }

    private static void generateRecordImpl_visitImmutableFields(ClassWriter cwRecordImpl,
                                                                int recordImmutableArgCount,
                                                                String[] recordImmutableArgMethodNames,
                                                                String[] recordImmutableArgMethodReturnTypes) {
        for (int i = 0; i < recordImmutableArgCount; ++i) {
            final FieldVisitor fvCallSite = cwRecordImpl.visitField(ACC_PRIVATE | ACC_FINAL, recordImmutableArgMethodNames[i], recordImmutableArgMethodReturnTypes[i], null, null);
            fvCallSite.visitEnd();
        }
    }

    private static void generateRecordImpl_visitMutableFields(ClassWriter cwRecordImpl,
                                                              int recordMutableArgCount,
                                                              String[] recordMutableArgMethodNames,
                                                              String[] recordMutableArgMethodReturnTypes) {
        for (int i = 0; i < recordMutableArgCount; ++i) {
            final FieldVisitor fvCallSite = cwRecordImpl.visitField(ACC_PRIVATE, recordMutableArgMethodNames[i], recordMutableArgMethodReturnTypes[i], null, null);
            fvCallSite.visitEnd();
        }
    }

    private static <T> void generateRecordImpl_visitInitAndGetConstructorMH(ClassWriter cwRecordImpl,
                                                                            String className,
                                                                            Class<T> recordAbstractOrInterfaceClass,
                                                                            boolean useInterface,
                                                                            int recordImmutableArgCount,
                                                                            String[] recordImmutableArgMethodNames,
                                                                            String[] recordImmutableArgMethodReturnTypes
    ) {
        final String mergedInitDesc = getMergedInitDesc(recordImmutableArgMethodReturnTypes);
        {
            final MethodVisitor mvInit = cwRecordImpl.visitMethod(ACC_PUBLIC, "<init>", mergedInitDesc, null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(
                    INVOKESPECIAL,
                    useInterface
                            ? "java/lang/Object"
                            : recordAbstractOrInterfaceClass.getName().replace('.', '/')
                    ,
                    "<init>",
                    "()V",
                    false
            );

            int varIndex = 1;
            for (int i = 0; i < recordImmutableArgCount; ++i) {
                final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[i];
                mvInit.visitVarInsn(ALOAD, 0);
                switch (recordArgMethodReturnType) {
                    case "V":
                        throw new IllegalArgumentException("Found void in recordArgMethodReturnTypes");
                    case "Z":
                    case "C":
                    case "B":
                    case "S":
                    case "I":
                        mvInit.visitVarInsn(ILOAD, varIndex++);
                        break;
                    case "F":
                        mvInit.visitVarInsn(FLOAD, varIndex++);
                        break;
                    case "J":
                        mvInit.visitVarInsn(LLOAD, varIndex);
                        varIndex += 2;
                        break;
                    case "D":
                        mvInit.visitVarInsn(DLOAD, varIndex);
                        varIndex += 2;
                        break;
                    default:
                        if (recordArgMethodReturnType.charAt(0) != 'L' && recordArgMethodReturnType.charAt(0) != '[')
                            throw new IllegalArgumentException("Unexpected descriptor: " + recordArgMethodReturnType);
                        mvInit.visitVarInsn(ALOAD, varIndex++);
                }
                mvInit.visitFieldInsn(PUTFIELD, className, recordImmutableArgMethodNames[i], recordArgMethodReturnType);
            }

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(-1, -1);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvConstructorMHGetter = cwRecordImpl.visitMethod(ACC_PUBLIC | ACC_STATIC, "getConstructorMH", "(I)Ljava/lang/invoke/MethodHandle;", null, null);
            mvConstructorMHGetter.visitCode();

            final Handle constructorHandle = new Handle(H_NEWINVOKESPECIAL, className, "<init>", mergedInitDesc, false);

            // return handle.asType(MethodType.methodType(clazz, handle.type()));
            mvConstructorMHGetter.visitLdcInsn(constructorHandle);
            mvConstructorMHGetter.visitLdcInsn(Type.getType(recordAbstractOrInterfaceClass));
            mvConstructorMHGetter.visitLdcInsn(constructorHandle);
            mvConstructorMHGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "type", "()Ljava/lang/invoke/MethodType;", false);
            mvConstructorMHGetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodType", "methodType", "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodType;", false);
            mvConstructorMHGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "asType", "(Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;", false);

            mvConstructorMHGetter.visitInsn(ARETURN);

            mvConstructorMHGetter.visitMaxs(3, 1);
            mvConstructorMHGetter.visitEnd();
        }
    }

    private static void generateRecordImpl_visitGetter(ClassWriter cwRecordImpl,
                                                       String className,
                                                       String recordArgMethodName,
                                                       String recordArgMethodReturnType) {
        final MethodVisitor mvGetter = cwRecordImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, recordArgMethodName, "()" + recordArgMethodReturnType, null, null);
        mvGetter.visitCode();

        mvGetter.visitVarInsn(ALOAD, 0);
        mvGetter.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
        switch (recordArgMethodReturnType) {
            case "Z":
            case "C":
            case "B":
            case "S":
            case "I":
                mvGetter.visitInsn(IRETURN);
                break;
            case "F":
                mvGetter.visitInsn(FRETURN);
                break;
            case "J":
                mvGetter.visitInsn(LRETURN);
                break;
            case "D":
                mvGetter.visitInsn(DRETURN);
                break;
            default:
                mvGetter.visitInsn(ARETURN);
        }

        mvGetter.visitMaxs(-1, 1);
        mvGetter.visitEnd();
    }

    private static void generateRecordImpl_visitSetter(ClassWriter cwRecordImpl,
                                                       String className,
                                                       String recordArgMethodName,
                                                       String recordArgMethodReturnType) {
        final MethodVisitor mvSetter = cwRecordImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, recordArgMethodName, "(" + recordArgMethodReturnType + ")V", null, null);
        mvSetter.visitCode();

        mvSetter.visitVarInsn(ALOAD, 0);
        switch (recordArgMethodReturnType) {
            case "Z":
            case "C":
            case "B":
            case "S":
            case "I":
                mvSetter.visitVarInsn(ILOAD, 1);
                break;
            case "F":
                mvSetter.visitVarInsn(FLOAD, 1);
                break;
            case "J":
                mvSetter.visitVarInsn(LLOAD, 1);
                break;
            case "D":
                mvSetter.visitVarInsn(DLOAD, 1);
                break;
            default:
                mvSetter.visitVarInsn(ALOAD, 1);
        }
        mvSetter.visitFieldInsn(PUTFIELD, className, recordArgMethodName, recordArgMethodReturnType);
        mvSetter.visitInsn(RETURN);

        mvSetter.visitMaxs(-1, -1);
        mvSetter.visitEnd();
    }

    private static void generateRecordImpl_generateToStringHashCodeEquals(ClassWriter cwRecordImpl,
                                                                          String className,
                                                                          String simpleClassName,
                                                                          int recordImmutableArgCount,
                                                                          String[] recordImmutableArgMethodNames,
                                                                          String[] recordImmutableArgMethodReturnTypes,
                                                                          int recordMutableArgCount,
                                                                          String[] recordMutableArgMethodNames,
                                                                          String[] recordMutableArgMethodReturnTypes) {
        {
            // only use immutable fields here
            final MethodVisitor mvToString = cwRecordImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "toString", "()Ljava/lang/String;", null, null);
            mvToString.visitCode();

            if (recordImmutableArgCount != 0
//                    || recordMutableArgCount != 0
            ) {
                // return "SimpleClassName[field0=${field0}, field1=${field1}, ...]";
                mvToString.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvToString.visitInsn(DUP);
                mvToString.visitLdcInsn(simpleClassName + "[");
                mvToString.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
                boolean isFirst = true;
                //noinspection ConstantValue
                if (recordImmutableArgCount != 0) {
                    for (int i = 0; i < recordImmutableArgCount; ++i) {
                        final String recordArgMethodName = recordImmutableArgMethodNames[i];
                        final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[i];
                        mvToString.visitLdcInsn((isFirst ? "" : ", ") + recordArgMethodName + "=");
                        isFirst = false;
                        mvToString.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                        mvToString.visitVarInsn(ALOAD, 0);
                        mvToString.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                        pushToString(mvToString, recordArgMethodReturnType);
                        mvToString.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                    }
                }
                /*
                if (recordMutableArgCount != 0) {
                    for (int i = 0; i < recordMutableArgCount; ++i) {
                        final String recordArgMethodName = recordMutableArgMethodNames[i];
                        final String recordArgMethodReturnType = recordMutableArgMethodReturnTypes[i];
                        mvToString.visitLdcInsn((isFirst ? "" : ", ") + recordArgMethodName + "=");
                        isFirst = false;
                        mvToString.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                        mvToString.visitVarInsn(ALOAD, 0);
                        mvToString.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                        pushToString(mvToString, recordArgMethodReturnType);
                        mvToString.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                    }
                }
                */
                mvToString.visitLdcInsn("]");
                mvToString.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvToString.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            } else {
                // return "SimpleClassName[]";
                mvToString.visitLdcInsn(simpleClassName + "[]");
            }
            mvToString.visitInsn(ARETURN);

            mvToString.visitMaxs(-1, -1);
            mvToString.visitEnd();
        }
        {
            // only use immutable fields here
            final MethodVisitor mvHashCode = cwRecordImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "hashCode", "()I", null, null);
            mvHashCode.visitCode();

            switch (recordImmutableArgCount) {
                case 0: {
                    // return 0;
                    mvHashCode.visitInsn(ICONST_0);
                    mvHashCode.visitInsn(IRETURN);
                    break;
                }
                case 1: {
                    // return getHash(this.field0);
                    final String recordArgMethodName = recordImmutableArgMethodNames[0];
                    final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[0];
                    mvHashCode.visitVarInsn(ALOAD, 0);
                    mvHashCode.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                    pushHashCode(mvHashCode, recordArgMethodReturnType);
                    mvHashCode.visitInsn(IRETURN);
                    break;
                }
                case 2: {
                    // int hash = getHash(this.field0);
                    // return hash * 31 + getHash(this.field1);
                    {
                        final String recordArgMethodName = recordImmutableArgMethodNames[0];
                        final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[0];
                        mvHashCode.visitVarInsn(ALOAD, 0);
                        mvHashCode.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                        pushHashCode(mvHashCode, recordArgMethodReturnType);
                        mvHashCode.visitVarInsn(ISTORE, 1);
                    }
                    {
                        mvHashCode.visitVarInsn(ILOAD, 1);
                        mvHashCode.visitIntInsn(BIPUSH, 31);
                        mvHashCode.visitInsn(IMUL);
                        final String recordArgMethodName = recordImmutableArgMethodNames[1];
                        final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[1];
                        mvHashCode.visitVarInsn(ALOAD, 0);
                        mvHashCode.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                        pushHashCode(mvHashCode, recordArgMethodReturnType);
                        mvHashCode.visitInsn(IADD);
                        mvHashCode.visitInsn(IRETURN);
                    }
                    break;
                }
                default: {
                    // int hash = getHash(this.field0);
                    // for field in fields[1:fields.length-1]
                    //     hash = hash * 31 + getHash(field);
                    // return hash * 31 + getHash(this.fieldLast);
                    {
                        final String recordArgMethodName = recordImmutableArgMethodNames[0];
                        final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[0];
                        mvHashCode.visitVarInsn(ALOAD, 0);
                        mvHashCode.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                        pushHashCode(mvHashCode, recordArgMethodReturnType);
                        mvHashCode.visitVarInsn(ISTORE, 1);
                    }
                    int i = 1;
                    for (int countSub1 = recordImmutableArgCount - 1; i < countSub1; ++i) {
                        mvHashCode.visitVarInsn(ILOAD, 1);
                        mvHashCode.visitIntInsn(BIPUSH, 31);
                        mvHashCode.visitInsn(IMUL);
                        final String recordArgMethodName = recordImmutableArgMethodNames[i];
                        final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[i];
                        mvHashCode.visitVarInsn(ALOAD, 0);
                        mvHashCode.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                        pushHashCode(mvHashCode, recordArgMethodReturnType);
                        mvHashCode.visitInsn(IADD);
                        mvHashCode.visitVarInsn(ISTORE, 1);
                    }
                    {
                        mvHashCode.visitVarInsn(ILOAD, 1);
                        mvHashCode.visitIntInsn(BIPUSH, 31);
                        mvHashCode.visitInsn(IMUL);
                        final String recordArgMethodName = recordImmutableArgMethodNames[i];
                        final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[i];
                        mvHashCode.visitVarInsn(ALOAD, 0);
                        mvHashCode.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                        pushHashCode(mvHashCode, recordArgMethodReturnType);
                        mvHashCode.visitInsn(IADD);
                        mvHashCode.visitInsn(IRETURN);
                    }
                    break;
                }
            }

            mvHashCode.visitMaxs(-1, -1);
            mvHashCode.visitEnd();
        }
        {
            final MethodVisitor mvEquals = cwRecordImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "equals", "(Ljava/lang/Object;)Z", null, null);
            mvEquals.visitCode();
            Label CmpFailed = new Label();
            Label FastCmpFailed = new Label();

            // if (this == other) return true;
            mvEquals.visitVarInsn(ALOAD, 0);
            mvEquals.visitVarInsn(ALOAD, 1);
            mvEquals.visitJumpInsn(IF_ACMPNE, FastCmpFailed);
            mvEquals.visitInsn(ICONST_1);
            mvEquals.visitInsn(IRETURN);
            mvEquals.visitLabel(FastCmpFailed);

            // if (!(other instanceof ThisClass)) goto CmpFailed;
            mvEquals.visitVarInsn(ALOAD, 1);
            mvEquals.visitTypeInsn(INSTANCEOF, className);
            mvEquals.visitJumpInsn(IFEQ, CmpFailed);

            // skip compare if no fields
            if (recordImmutableArgCount != 0 || recordMutableArgCount != 0) {
                // ThisClass other = (ThisClass) other;
                mvEquals.visitVarInsn(ALOAD, 1);
                mvEquals.visitTypeInsn(CHECKCAST, className);
                mvEquals.visitVarInsn(ASTORE, 2);

                // for field in fields
                //     if cmp_ne(this.field, other.field)
                //         goto CmpTailed;
                for (int i = 0; i < recordImmutableArgCount; ++i) {
                    final String recordArgMethodName = recordImmutableArgMethodNames[i];
                    final String recordArgMethodReturnType = recordImmutableArgMethodReturnTypes[i];
                    mvEquals.visitVarInsn(ALOAD, 0);
                    mvEquals.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                    mvEquals.visitVarInsn(ALOAD, 2);
                    mvEquals.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                    switch (recordArgMethodReturnType) {
                        case "Z":
                        case "C":
                        case "B":
                        case "S":
                        case "I":
                            mvEquals.visitJumpInsn(IF_ICMPNE, CmpFailed);
                            break;
                        case "F":
                            mvEquals.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "compare", "(FF)I", false);
                            mvEquals.visitJumpInsn(IFNE, CmpFailed);
                            break;
                        case "J":
                            mvEquals.visitInsn(LCMP);
                            mvEquals.visitJumpInsn(IFNE, CmpFailed);
                            break;
                        case "D":
                            mvEquals.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "compare", "(DD)I", false);
                            mvEquals.visitJumpInsn(IFNE, CmpFailed);
                            break;
                        default:
                            mvEquals.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                            mvEquals.visitJumpInsn(IFEQ, CmpFailed);
                    }
                }
                for (int i = 0; i < recordMutableArgCount; ++i) {
                    final String recordArgMethodName = recordMutableArgMethodNames[i];
                    final String recordArgMethodReturnType = recordMutableArgMethodReturnTypes[i];
                    mvEquals.visitVarInsn(ALOAD, 0);
                    mvEquals.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                    mvEquals.visitVarInsn(ALOAD, 2);
                    mvEquals.visitFieldInsn(GETFIELD, className, recordArgMethodName, recordArgMethodReturnType);
                    switch (recordArgMethodReturnType) {
                        case "Z":
                        case "C":
                        case "B":
                        case "S":
                        case "I":
                            mvEquals.visitJumpInsn(IF_ICMPNE, CmpFailed);
                            break;
                        case "F":
                            mvEquals.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "compare", "(FF)I", false);
                            mvEquals.visitJumpInsn(IFNE, CmpFailed);
                            break;
                        case "J":
                            mvEquals.visitInsn(LCMP);
                            mvEquals.visitJumpInsn(IFNE, CmpFailed);
                            break;
                        case "D":
                            mvEquals.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "compare", "(DD)I", false);
                            mvEquals.visitJumpInsn(IFNE, CmpFailed);
                            break;
                        default:
                            mvEquals.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
                            mvEquals.visitJumpInsn(IFEQ, CmpFailed);
                    }
                }
            }

            // return true;
            mvEquals.visitInsn(ICONST_1);
            mvEquals.visitInsn(IRETURN);

            // CmpTailed:
            // return false;
            mvEquals.visitLabel(CmpFailed);
            mvEquals.visitInsn(ICONST_0);
            mvEquals.visitInsn(IRETURN);

            mvEquals.visitMaxs(-1, -1);
            mvEquals.visitEnd();
        }
    }

    protected static byte[] generateEmptyImpl(String className,
                                              boolean useInterface,
                                              Class<?> abstractOrInterfaceClass
    ) {
        final ClassWriter cwEmptyImpl = new ClassWriter(0);
        final String abstractOrInterfaceClassName = abstractOrInterfaceClass.getName().replace('.', '/');
        cwEmptyImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                null,
                useInterface ? "java/lang/Object" : abstractOrInterfaceClassName,
                useInterface ? new String[] { abstractOrInterfaceClassName } : null);
        {
            final MethodVisitor mvInit = cwEmptyImpl.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(
                    INVOKESPECIAL,
                    useInterface
                            ? "java/lang/Object"
                            : abstractOrInterfaceClassName
                    ,
                    "<init>",
                    "()V",
                    false
            );

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(1, 1);
            mvInit.visitEnd();
        }
        return cwEmptyImpl.toByteArray();
    }

    protected static byte[] generateConstantBaseImpl(String className) {
        final ClassWriter cwConstantBaseImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String DynamicConstantClassName = DynamicConstant.class.getName().replace('.', '/');
        cwConstantBaseImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                "<T:Ljava/lang/Object;>Ljava/lang/Object;L" + DynamicConstantClassName + "<TT;>;",
                "java/lang/Object",
                new String[] {
                        DynamicConstantClassName
                });
        {
            final FieldVisitor fvCallSite = cwConstantBaseImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "callSite", "Ljava/lang/invoke/CallSite;", null, null);
            fvCallSite.visitEnd();
        }
        {
            final FieldVisitor fvDynamicInvoker = cwConstantBaseImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "dynamicInvoker", "Ljava/lang/invoke/MethodHandle;", null, null);
            fvDynamicInvoker.visitEnd();
        }
        {
            final MethodVisitor mvClInit = cwConstantBaseImpl.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mvClInit.visitCode();

            mvClInit.visitTypeInsn(NEW, "java/lang/invoke/VolatileCallSite");
            mvClInit.visitInsn(DUP);
            mvClInit.visitLdcInsn(Type.getType(Object.class));
            mvClInit.visitInsn(ACONST_NULL);
            mvClInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
            mvClInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/VolatileCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
            mvClInit.visitInsn(DUP);
            mvClInit.visitFieldInsn(PUTSTATIC, className, "callSite", "Ljava/lang/invoke/CallSite;");

            mvClInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
            mvClInit.visitFieldInsn(PUTSTATIC, className, "dynamicInvoker", "Ljava/lang/invoke/MethodHandle;");

            mvClInit.visitInsn(RETURN);
            mvClInit.visitMaxs(4, 0);
            mvClInit.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwConstantBaseImpl.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", "(TT;)V", null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitVarInsn(ALOAD, 1);
            mvInit.visitMethodInsn(INVOKEVIRTUAL, className, "set", "(Ljava/lang/Object;)V", false);

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(2, 2);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwConstantBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get", "()Ljava/lang/Object;", "()TT;", null);
            mvGetter.visitCode();

            Label tryStart = new Label();
            Label tryEnd = new Label();
            Label UncheckedCatchStart = new Label();
            Label CheckedCatchStart = new Label();
            mvGetter.visitTryCatchBlock(tryStart, tryEnd, UncheckedCatchStart, "java/lang/RuntimeException");
            mvGetter.visitTryCatchBlock(tryStart, tryEnd, UncheckedCatchStart, "java/lang/Error");
            mvGetter.visitTryCatchBlock(tryStart, tryEnd, CheckedCatchStart, "java/lang/Throwable");

            mvGetter.visitFieldInsn(GETSTATIC, className, "dynamicInvoker", "Ljava/lang/invoke/MethodHandle;");
            mvGetter.visitLabel(tryStart);
            mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Ljava/lang/Object;", false);
            mvGetter.visitLabel(tryEnd);
            mvGetter.visitInsn(ARETURN);

            mvGetter.visitLabel(CheckedCatchStart);
            mvGetter.visitVarInsn(ASTORE, 1);
            mvGetter.visitTypeInsn(NEW, "java/lang/RuntimeException");
            mvGetter.visitInsn(DUP);
            mvGetter.visitVarInsn(ALOAD, 1);
            mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
            mvGetter.visitLabel(UncheckedCatchStart);
            mvGetter.visitInsn(ATHROW);

            mvGetter.visitMaxs(3, 2);
            mvGetter.visitEnd();
        }
        {
            final MethodVisitor mvSetter = cwConstantBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "set", "(Ljava/lang/Object;)V", "(TT;)V", null);
            mvSetter.visitCode();

            mvSetter.visitFieldInsn(GETSTATIC, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvSetter.visitLdcInsn(Type.getType(Object.class));
            mvSetter.visitVarInsn(ALOAD, 1);
            mvSetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
            mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);

            mvSetter.visitInsn(RETURN);
            mvSetter.visitMaxs(3, 2);
            mvSetter.visitEnd();
        }
        cwConstantBaseImpl.visitEnd();
        return cwConstantBaseImpl.toByteArray();
    }

    protected static byte[] generateMethodHandleBaseImpl(
            String className,
            Class<?> delegateAbstractOrInterfaceClass,
            boolean useInterface,
            String delegateMethodName,
            MethodType delegateMethodAndMethodHandleType,
            String setterMethodName,
            boolean sneakyThrows
    ) {
        final ClassWriter cwMethodHandleBaseImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String delegateAbstractOrInterfaceClassName = delegateAbstractOrInterfaceClass.getName().replace('.', '/');
        final String delegateMethodAndMethodHandleDescriptor = delegateMethodAndMethodHandleType.descriptorString();
        cwMethodHandleBaseImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                null,
                useInterface ? "java/lang/Object" : delegateAbstractOrInterfaceClassName,
                useInterface
                        ? new String[] { delegateAbstractOrInterfaceClassName }
                        : null
        );
        {
            final FieldVisitor fvCallSite = cwMethodHandleBaseImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "callSite", "Ljava/lang/invoke/CallSite;", null, null);
            fvCallSite.visitEnd();
        }
        {
            final MethodVisitor mvClInit = cwMethodHandleBaseImpl.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mvClInit.visitCode();

            mvClInit.visitTypeInsn(NEW, "java/lang/invoke/VolatileCallSite");
            mvClInit.visitInsn(DUP);
            mvClInit.visitLdcInsn(Type.getMethodType(delegateMethodAndMethodHandleDescriptor));
            mvClInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/VolatileCallSite", "<init>", "(Ljava/lang/invoke/MethodType;)V", false);
            mvClInit.visitFieldInsn(PUTSTATIC, className, "callSite", "Ljava/lang/invoke/CallSite;");

            mvClInit.visitInsn(RETURN);
            mvClInit.visitMaxs(3, 0);
            mvClInit.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwMethodHandleBaseImpl.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/invoke/MethodHandle;)V", null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, useInterface ? "java/lang/Object" : delegateAbstractOrInterfaceClassName, "<init>", "()V", false);

            mvInit.visitFieldInsn(GETSTATIC, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvInit.visitVarInsn(ALOAD, 1);
            mvInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(2, 2);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvInvoker = cwMethodHandleBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, delegateMethodName, delegateMethodAndMethodHandleDescriptor, null, sneakyThrows ? null : new String[] { "java/lang/Throwable" });
            mvInvoker.visitCode();

            mvInvoker.visitFieldInsn(GETSTATIC, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvInvoker.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "getTarget", "()Ljava/lang/invoke/MethodHandle;", false);
            for (int i = 0, slot = 1, count = delegateMethodAndMethodHandleType.parameterCount(); i < count; ++i) {
                final Class<?> parameterType = delegateMethodAndMethodHandleType.parameterType(i);
                if (parameterType.isPrimitive()) {
                    switch (Type.getType(parameterType).getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.INT:
                        case Type.SHORT: {
                            mvInvoker.visitVarInsn(ILOAD, slot);
                            slot += 1;
                            break;
                        }
                        case Type.DOUBLE: {
                            mvInvoker.visitVarInsn(DLOAD, slot);
                            slot += 2;
                            break;
                        }
                        case Type.FLOAT: {
                            mvInvoker.visitVarInsn(FLOAD, slot);
                            slot += 1;
                            break;
                        }
                        case Type.LONG: {
                            mvInvoker.visitVarInsn(LLOAD, slot);
                            slot += 2;
                            break;
                        }
                        default: {
                            throw new AssertionError(parameterType.getName());
                        }
                    }
                } else {
                    mvInvoker.visitVarInsn(ALOAD, slot);
                    slot += 1;
                }
            }
            mvInvoker.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", delegateMethodAndMethodHandleDescriptor, false);
            final Class<?> returnType = delegateMethodAndMethodHandleType.returnType();
            if (returnType.isPrimitive()) {
                switch (Type.getType(returnType).getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.INT:
                    case Type.SHORT: {
                        mvInvoker.visitInsn(IRETURN);
                        break;
                    }
                    case Type.DOUBLE: {
                        mvInvoker.visitInsn(DRETURN);
                        break;
                    }
                    case Type.FLOAT: {
                        mvInvoker.visitInsn(FRETURN);
                        break;
                    }
                    case Type.LONG: {
                        mvInvoker.visitInsn(LRETURN);
                        break;
                    }
                    case Type.VOID: {
                        mvInvoker.visitInsn(RETURN);
                        break;
                    }
                    default: {
                        throw new AssertionError(returnType.getName());
                    }
                }
            } else {
                mvInvoker.visitInsn(ARETURN);
            }

            mvInvoker.visitMaxs(-1, -1);
            mvInvoker.visitEnd();
        }
        if (setterMethodName != null) {
            final MethodVisitor mvSetter = cwMethodHandleBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, setterMethodName, "(Ljava/lang/invoke/MethodHandle;)V", null, null);
            mvSetter.visitCode();

            mvSetter.visitFieldInsn(GETSTATIC, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvSetter.visitVarInsn(ALOAD, 1);
            mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);

            mvSetter.visitInsn(RETURN);
            mvSetter.visitMaxs(2, 2);
            mvSetter.visitEnd();
        }
        cwMethodHandleBaseImpl.visitEnd();
        return cwMethodHandleBaseImpl.toByteArray();
    }

    protected static byte[] generateArrayImpl(
            String className,
            int size
    ) {
        final ClassWriter cwArrayImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String ConstantArrayClassName = ConstantArray.class.getName().replace('.', '/');
        cwArrayImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                "<T:Ljava/lang/Object;>Ljava/lang/Object;L" + ConstantArrayClassName + "<TT;>;",
                "java/lang/Object",
                new String[] { ConstantArrayClassName }
        );
        for (int i = 0; i < size; ++i) {
            final FieldVisitor fvCallSite = cwArrayImpl.visitField(ACC_PRIVATE | ACC_FINAL, "callSite$" + i, "Ljava/lang/invoke/CallSite;", null, null);
            fvCallSite.visitEnd();
            final FieldVisitor fvDynamicInvoker = cwArrayImpl.visitField(ACC_PRIVATE | ACC_FINAL, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;", null, null);
            fvDynamicInvoker.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwArrayImpl.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            for (int i = 0; i < size; ++i) {

                mvInit.visitVarInsn(ALOAD, 0);
                mvInit.visitTypeInsn(NEW, "java/lang/invoke/VolatileCallSite");
                mvInit.visitInsn(DUP);
                mvInit.visitLdcInsn(Type.getType(Object.class));
                mvInit.visitInsn(ACONST_NULL);
                mvInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/VolatileCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvInit.visitFieldInsn(PUTFIELD, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");

                mvInit.visitVarInsn(ALOAD, 0);
                mvInit.visitVarInsn(ALOAD, 0);
                mvInit.visitFieldInsn(GETFIELD, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");
                mvInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
                mvInit.visitFieldInsn(PUTFIELD, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
            }

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(5, 1);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwArrayImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get", "(I)Ljava/lang/Object;", "(I)TT;", null);
            mvGetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvGetter.visitVarInsn(ILOAD, 1);
            mvGetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvGetter.visitLabel(elements[i]);
                mvGetter.visitVarInsn(ALOAD, 0);
                mvGetter.visitFieldInsn(GETFIELD, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Ljava/lang/Object;", false);
                mvGetter.visitInsn(ARETURN);
            }
            {
                mvGetter.visitLabel(elementNotFound);
                mvGetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvGetter.visitInsn(DUP);
                mvGetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvGetter.visitInsn(DUP);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvGetter.visitLdcInsn("Index ");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitVarInsn(ILOAD, 1);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvGetter.visitLdcInsn(" out of bounds for length " + size);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvGetter.visitInsn(ATHROW);
            }

            mvGetter.visitMaxs(4, 2);
            mvGetter.visitEnd();
        }
        {
            final MethodVisitor mvSetter = cwArrayImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "set", "(ILjava/lang/Object;)V", "(ITT;)V", null);
            mvSetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvSetter.visitVarInsn(ILOAD, 1);
            mvSetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvSetter.visitLabel(elements[i]);
                mvSetter.visitVarInsn(ALOAD, 0);
                mvSetter.visitFieldInsn(GETFIELD, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");
                mvSetter.visitLdcInsn(Type.getType(Object.class));
                mvSetter.visitVarInsn(ALOAD, 2);
                mvSetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvSetter.visitInsn(RETURN);
            }
            {
                mvSetter.visitLabel(elementNotFound);
                mvSetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvSetter.visitInsn(DUP);
                mvSetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvSetter.visitInsn(DUP);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvSetter.visitLdcInsn("Index ");
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitVarInsn(ILOAD, 1);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvSetter.visitLdcInsn(" out of bounds for length " + size);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvSetter.visitInsn(ATHROW);
            }

            mvSetter.visitMaxs(4, 3);
            mvSetter.visitEnd();
        }
        {
            final MethodVisitor mvSize = cwArrayImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "size", "()I", null, null);
            mvSize.visitCode();

            switch (size) {
                case 0: {
                    // ???
                    mvSize.visitInsn(ICONST_0);
                    break;
                }
                case 1: {
                    mvSize.visitInsn(ICONST_1);
                    break;
                }
                case 2: {
                    mvSize.visitInsn(ICONST_2);
                    break;
                }
                case 3: {
                    mvSize.visitInsn(ICONST_3);
                    break;
                }
                case 4: {
                    mvSize.visitInsn(ICONST_4);
                    break;
                }
                case 5: {
                    mvSize.visitInsn(ICONST_5);
                    break;
                }
                default: {
                    if (size <= Byte.MAX_VALUE) {
                        mvSize.visitIntInsn(BIPUSH, size);
                    } else if (size <= Short.MAX_VALUE) {
                        mvSize.visitIntInsn(SIPUSH, size);
                    } else {
                        mvSize.visitLdcInsn(size);
                    }
                }
            }
            mvSize.visitInsn(IRETURN);

            mvSize.visitMaxs(1, 1);
            mvSize.visitEnd();
        }
        cwArrayImpl.visitEnd();
        return cwArrayImpl.toByteArray();
    }

    protected static byte[] generatePrimitiveArrayImpl(
            String className,
            int size,
            Class<?> primitiveType
    ) {
        final ClassWriter cwPrimitiveArrayImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final int primitiveTypeSort = Type.getType(primitiveType).getSort();
        final String primitiveTypeDescriptor = Type.getDescriptor(primitiveType);
        final String methodNameSuffix = getPrimitiveTypeName(primitiveType);
        final String PrimitiveConstantArrayClassName = getPrimitiveConstantArrayClass(primitiveType).getName().replace('.', '/');
        cwPrimitiveArrayImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                null,
                "java/lang/Object",
                new String[] { PrimitiveConstantArrayClassName }
        );
        for (int i = 0; i < size; ++i) {
            final FieldVisitor fvCallSite = cwPrimitiveArrayImpl.visitField(ACC_PRIVATE | ACC_FINAL, "callSite$" + i, "Ljava/lang/invoke/CallSite;", null, null);
            fvCallSite.visitEnd();
            final FieldVisitor fvDynamicInvoker = cwPrimitiveArrayImpl.visitField(ACC_PRIVATE | ACC_FINAL, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;", null, null);
            fvDynamicInvoker.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwPrimitiveArrayImpl.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            for (int i = 0; i < size; ++i) {

                mvInit.visitVarInsn(ALOAD, 0);
                mvInit.visitTypeInsn(NEW, "java/lang/invoke/VolatileCallSite");
                mvInit.visitInsn(DUP);
                pushPrimitiveTypeOnStack(primitiveType, mvInit);
                switch (primitiveTypeSort) {
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT: {
                        mvInit.visitInsn(ICONST_0);
                        break;
                    }
                    case Type.FLOAT: {
                        mvInit.visitInsn(FCONST_0);
                        break;
                    }
                    case Type.LONG: {
                        mvInit.visitInsn(LCONST_0);
                        break;
                    }
                    case Type.DOUBLE: {
                        mvInit.visitInsn(DCONST_0);
                        break;
                    }
                    default: {
                        throw new AssertionError();
                    }
                }
                wrapPrimitiveOnStack(primitiveType, mvInit);
                mvInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/VolatileCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvInit.visitFieldInsn(PUTFIELD, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");

                mvInit.visitVarInsn(ALOAD, 0);
                mvInit.visitVarInsn(ALOAD, 0);
                mvInit.visitFieldInsn(GETFIELD, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");
                mvInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
                mvInit.visitFieldInsn(PUTFIELD, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
            }

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(-1, 1);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwPrimitiveArrayImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get" + methodNameSuffix, "(I)" + primitiveTypeDescriptor, null, null);
            mvGetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvGetter.visitVarInsn(ILOAD, 1);
            mvGetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvGetter.visitLabel(elements[i]);
                mvGetter.visitVarInsn(ALOAD, 0);
                mvGetter.visitFieldInsn(GETFIELD, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()" + primitiveTypeDescriptor, false);
                switch (primitiveTypeSort) {
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT: {
                        mvGetter.visitInsn(IRETURN);
                        break;
                    }
                    case Type.FLOAT: {
                        mvGetter.visitInsn(FRETURN);
                        break;
                    }
                    case Type.LONG: {
                        mvGetter.visitInsn(LRETURN);
                        break;
                    }
                    case Type.DOUBLE: {
                        mvGetter.visitInsn(DRETURN);
                        break;
                    }
                    default: {
                        throw new AssertionError();
                    }
                }
            }
            {
                mvGetter.visitLabel(elementNotFound);
                mvGetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvGetter.visitInsn(DUP);
                mvGetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvGetter.visitInsn(DUP);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvGetter.visitLdcInsn("Index ");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitVarInsn(ILOAD, 1);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvGetter.visitLdcInsn(" out of bounds for length " + size);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvGetter.visitInsn(ATHROW);
            }

            mvGetter.visitMaxs(-1, 2);
            mvGetter.visitEnd();
        }
        {
            final MethodVisitor mvSetter = cwPrimitiveArrayImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "set" + methodNameSuffix, "(I" + primitiveTypeDescriptor + ")V", null, null);
            mvSetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvSetter.visitVarInsn(ILOAD, 1);
            mvSetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvSetter.visitLabel(elements[i]);
                mvSetter.visitVarInsn(ALOAD, 0);
                mvSetter.visitFieldInsn(GETFIELD, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");
                pushPrimitiveTypeOnStack(primitiveType, mvSetter);
                switch (primitiveTypeSort) {
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT: {
                        mvSetter.visitVarInsn(ILOAD, 2);
                        break;
                    }
                    case Type.FLOAT: {
                        mvSetter.visitVarInsn(FLOAD, 2);
                        break;
                    }
                    case Type.LONG: {
                        mvSetter.visitVarInsn(LLOAD, 2);
                        break;
                    }
                    case Type.DOUBLE: {
                        mvSetter.visitVarInsn(DLOAD, 2);
                        break;
                    }
                    default: {
                        throw new AssertionError();
                    }
                }
                wrapPrimitiveOnStack(primitiveType, mvSetter);
                mvSetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvSetter.visitInsn(RETURN);
            }
            {
                mvSetter.visitLabel(elementNotFound);
                mvSetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvSetter.visitInsn(DUP);
                mvSetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvSetter.visitInsn(DUP);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvSetter.visitLdcInsn("Index ");
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitVarInsn(ILOAD, 1);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvSetter.visitLdcInsn(" out of bounds for length " + size);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvSetter.visitInsn(ATHROW);
            }

            mvSetter.visitMaxs(-1, -1);
            mvSetter.visitEnd();
        }
        {
            final MethodVisitor mvSize = cwPrimitiveArrayImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "size", "()I", null, null);
            mvSize.visitCode();

            switch (size) {
                case 0: {
                    mvSize.visitInsn(ICONST_0);
                    break;
                }
                case 1: {
                    mvSize.visitInsn(ICONST_1);
                    break;
                }
                case 2: {
                    mvSize.visitInsn(ICONST_2);
                    break;
                }
                case 3: {
                    mvSize.visitInsn(ICONST_3);
                    break;
                }
                case 4: {
                    mvSize.visitInsn(ICONST_4);
                    break;
                }
                case 5: {
                    mvSize.visitInsn(ICONST_5);
                    break;
                }
                default: {
                    if (size <= Byte.MAX_VALUE) {
                        mvSize.visitIntInsn(BIPUSH, size);
                    } else if (size <= Short.MAX_VALUE) {
                        mvSize.visitIntInsn(SIPUSH, size);
                    } else {
                        mvSize.visitLdcInsn(size);
                    }
                }
            }
            mvSize.visitInsn(IRETURN);

            mvSize.visitMaxs(1, 1);
            mvSize.visitEnd();
        }
        cwPrimitiveArrayImpl.visitEnd();
        return cwPrimitiveArrayImpl.toByteArray();
    }

    protected static byte[] generateArrayBaseImpl(
            String className,
            int size
    ) {
        final ClassWriter cwArrayBaseImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final String ConstantArrayClassName = ConstantArray.class.getName().replace('.', '/');
        cwArrayBaseImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                "<T:Ljava/lang/Object;>Ljava/lang/Object;L" + ConstantArrayClassName + "<TT;>;",
                "java/lang/Object",
                new String[] { ConstantArrayClassName }
        );
        for (int i = 0; i < size; ++i) {
            final FieldVisitor fvCallSite = cwArrayBaseImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "callSite$" + i, "Ljava/lang/invoke/CallSite;", null, null);
            fvCallSite.visitEnd();
            final FieldVisitor fvDynamicInvoker = cwArrayBaseImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;", null, null);
            fvDynamicInvoker.visitEnd();
        }
        {
            final MethodVisitor mvClInit = cwArrayBaseImpl.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mvClInit.visitCode();

            for (int i = 0; i < size; ++i) {

                mvClInit.visitTypeInsn(NEW, "java/lang/invoke/VolatileCallSite");
                mvClInit.visitInsn(DUP);
                mvClInit.visitLdcInsn(Type.getType(Object.class));
                mvClInit.visitInsn(ACONST_NULL);
                mvClInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvClInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/VolatileCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvClInit.visitInsn(DUP);
                mvClInit.visitFieldInsn(PUTSTATIC, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");

                mvClInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
                mvClInit.visitFieldInsn(PUTSTATIC, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
            }

            mvClInit.visitInsn(RETURN);
            mvClInit.visitMaxs(4, 0);
            mvClInit.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwArrayBaseImpl.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(1, 1);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwArrayBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get", "(I)Ljava/lang/Object;", "(I)TT;", null);
            mvGetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvGetter.visitVarInsn(ILOAD, 1);
            mvGetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvGetter.visitLabel(elements[i]);
                mvGetter.visitFieldInsn(GETSTATIC, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()Ljava/lang/Object;", false);
                mvGetter.visitInsn(ARETURN);
            }
            {
                mvGetter.visitLabel(elementNotFound);
                mvGetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvGetter.visitInsn(DUP);
                mvGetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvGetter.visitInsn(DUP);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvGetter.visitLdcInsn("Index ");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitVarInsn(ILOAD, 1);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvGetter.visitLdcInsn(" out of bounds for length " + size);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvGetter.visitInsn(ATHROW);
            }

            mvGetter.visitMaxs(4, 2);
            mvGetter.visitEnd();
        }
        {
            final MethodVisitor mvSetter = cwArrayBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "set", "(ILjava/lang/Object;)V", "(ITT;)V", null);
            mvSetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvSetter.visitVarInsn(ILOAD, 1);
            mvSetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvSetter.visitLabel(elements[i]);
                mvSetter.visitFieldInsn(GETSTATIC, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");
                mvSetter.visitLdcInsn(Type.getType(Object.class));
                mvSetter.visitVarInsn(ALOAD, 2);
                mvSetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvSetter.visitInsn(RETURN);
            }
            {
                mvSetter.visitLabel(elementNotFound);
                mvSetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvSetter.visitInsn(DUP);
                mvSetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvSetter.visitInsn(DUP);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvSetter.visitLdcInsn("Index ");
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitVarInsn(ILOAD, 1);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvSetter.visitLdcInsn(" out of bounds for length " + size);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvSetter.visitInsn(ATHROW);
            }

            mvSetter.visitMaxs(4, 3);
            mvSetter.visitEnd();
        }
        {
            final MethodVisitor mvSize = cwArrayBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "size", "()I", null, null);
            mvSize.visitCode();

            switch (size) {
                case 0: {
                    mvSize.visitInsn(ICONST_0);
                    break;
                }
                case 1: {
                    mvSize.visitInsn(ICONST_1);
                    break;
                }
                case 2: {
                    mvSize.visitInsn(ICONST_2);
                    break;
                }
                case 3: {
                    mvSize.visitInsn(ICONST_3);
                    break;
                }
                case 4: {
                    mvSize.visitInsn(ICONST_4);
                    break;
                }
                case 5: {
                    mvSize.visitInsn(ICONST_5);
                    break;
                }
                default: {
                    if (size <= Byte.MAX_VALUE) {
                        mvSize.visitIntInsn(BIPUSH, size);
                    } else if (size <= Short.MAX_VALUE) {
                        mvSize.visitIntInsn(SIPUSH, size);
                    } else {
                        mvSize.visitLdcInsn(size);
                    }
                }
            }
            mvSize.visitInsn(IRETURN);

            mvSize.visitMaxs(1, 1);
            mvSize.visitEnd();
        }
        cwArrayBaseImpl.visitEnd();
        return cwArrayBaseImpl.toByteArray();
    }

    protected static byte[] generatePrimitiveArrayBaseImpl(
            String className,
            int size,
            Class<?> primitiveType
    ) {
        final ClassWriter cwPrimitiveArrayBaseImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        final int primitiveTypeSort = Type.getType(primitiveType).getSort();
        final String primitiveTypeDescriptor = Type.getDescriptor(primitiveType);
        final String methodNameSuffix = getPrimitiveTypeName(primitiveType);
        final String PrimitiveConstantArrayClassName = getPrimitiveConstantArrayClass(primitiveType).getName().replace('.', '/');
        cwPrimitiveArrayBaseImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                null,
                "java/lang/Object",
                new String[] { PrimitiveConstantArrayClassName }
        );
        for (int i = 0; i < size; ++i) {
            final FieldVisitor fvCallSite = cwPrimitiveArrayBaseImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "callSite$" + i, "Ljava/lang/invoke/CallSite;", null, null);
            fvCallSite.visitEnd();
            final FieldVisitor fvDynamicInvoker = cwPrimitiveArrayBaseImpl.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;", null, null);
            fvDynamicInvoker.visitEnd();
        }
        {
            final MethodVisitor mvClInit = cwPrimitiveArrayBaseImpl.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
            mvClInit.visitCode();

            for (int i = 0; i < size; ++i) {

                mvClInit.visitTypeInsn(NEW, "java/lang/invoke/VolatileCallSite");
                mvClInit.visitInsn(DUP);
                pushPrimitiveTypeOnStack(primitiveType, mvClInit);
                switch (primitiveTypeSort) {
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT: {
                        mvClInit.visitInsn(ICONST_0);
                        break;
                    }
                    case Type.FLOAT: {
                        mvClInit.visitInsn(FCONST_0);
                        break;
                    }
                    case Type.LONG: {
                        mvClInit.visitInsn(LCONST_0);
                        break;
                    }
                    case Type.DOUBLE: {
                        mvClInit.visitInsn(DCONST_0);
                        break;
                    }
                    default: {
                        throw new AssertionError();
                    }
                }
                wrapPrimitiveOnStack(primitiveType, mvClInit);
                mvClInit.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvClInit.visitMethodInsn(INVOKESPECIAL, "java/lang/invoke/VolatileCallSite", "<init>", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvClInit.visitInsn(DUP);
                mvClInit.visitFieldInsn(PUTSTATIC, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");

                mvClInit.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "dynamicInvoker", "()Ljava/lang/invoke/MethodHandle;", false);
                mvClInit.visitFieldInsn(PUTSTATIC, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
            }

            mvClInit.visitInsn(RETURN);
            mvClInit.visitMaxs(-1, 0);
            mvClInit.visitEnd();
        }
        {
            final MethodVisitor mvInit = cwPrimitiveArrayBaseImpl.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

            mvInit.visitInsn(RETURN);
            mvInit.visitMaxs(1, 1);
            mvInit.visitEnd();
        }
        {
            final MethodVisitor mvGetter = cwPrimitiveArrayBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "get" + methodNameSuffix, "(I)" + primitiveTypeDescriptor, null, null);
            mvGetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvGetter.visitVarInsn(ILOAD, 1);
            mvGetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvGetter.visitLabel(elements[i]);
                mvGetter.visitFieldInsn(GETSTATIC, className, "dynamicInvoker$" + i, "Ljava/lang/invoke/MethodHandle;");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", "()" + primitiveTypeDescriptor, false);
                switch (primitiveTypeSort) {
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT: {
                        mvGetter.visitInsn(IRETURN);
                        break;
                    }
                    case Type.FLOAT: {
                        mvGetter.visitInsn(FRETURN);
                        break;
                    }
                    case Type.LONG: {
                        mvGetter.visitInsn(LRETURN);
                        break;
                    }
                    case Type.DOUBLE: {
                        mvGetter.visitInsn(DRETURN);
                        break;
                    }
                    default: {
                        throw new AssertionError();
                    }
                }
            }
            {
                mvGetter.visitLabel(elementNotFound);
                mvGetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvGetter.visitInsn(DUP);
                mvGetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvGetter.visitInsn(DUP);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvGetter.visitLdcInsn("Index ");
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitVarInsn(ILOAD, 1);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvGetter.visitLdcInsn(" out of bounds for length " + size);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvGetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvGetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvGetter.visitInsn(ATHROW);
            }

            mvGetter.visitMaxs(-1, 2);
            mvGetter.visitEnd();
        }
        {
            final MethodVisitor mvSetter = cwPrimitiveArrayBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "set" + methodNameSuffix, "(I" + primitiveTypeDescriptor + ")V", null, null);
            mvSetter.visitCode();

            Label elementNotFound = new Label();
            Label[] elements = new Label[size];
            for (int i = 0; i < size; ++i) {
                elements[i] = new Label();
            }
            mvSetter.visitVarInsn(ILOAD, 1);
            mvSetter.visitTableSwitchInsn(0, size - 1, elementNotFound, elements);
            for (int i = 0; i < size; ++i) {
                mvSetter.visitLabel(elements[i]);
                mvSetter.visitFieldInsn(GETSTATIC, className, "callSite$" + i, "Ljava/lang/invoke/CallSite;");
                pushPrimitiveTypeOnStack(primitiveType, mvSetter);
                switch (primitiveTypeSort) {
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT: {
                        mvSetter.visitVarInsn(ILOAD, 2);
                        break;
                    }
                    case Type.FLOAT: {
                        mvSetter.visitVarInsn(FLOAD, 2);
                        break;
                    }
                    case Type.LONG: {
                        mvSetter.visitVarInsn(LLOAD, 2);
                        break;
                    }
                    case Type.DOUBLE: {
                        mvSetter.visitVarInsn(DLOAD, 2);
                        break;
                    }
                    default: {
                        throw new AssertionError();
                    }
                }
                wrapPrimitiveOnStack(primitiveType, mvSetter);
                mvSetter.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MethodHandles", "constant", "(Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/invoke/MethodHandle;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/CallSite", "setTarget", "(Ljava/lang/invoke/MethodHandle;)V", false);
                mvSetter.visitInsn(RETURN);
            }
            {
                mvSetter.visitLabel(elementNotFound);
                mvSetter.visitTypeInsn(NEW, "java/lang/ArrayIndexOutOfBoundsException");
                mvSetter.visitInsn(DUP);
                mvSetter.visitTypeInsn(NEW, "java/lang/StringBuilder");
                mvSetter.visitInsn(DUP);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
                mvSetter.visitLdcInsn("Index ");
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitVarInsn(ILOAD, 1);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
                mvSetter.visitLdcInsn(" out of bounds for length " + size);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mvSetter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
                mvSetter.visitMethodInsn(INVOKESPECIAL, "java/lang/ArrayIndexOutOfBoundsException", "<init>", "(Ljava/lang/String;)V", false);
                mvSetter.visitInsn(ATHROW);
            }

            mvSetter.visitMaxs(-1, -1);
            mvSetter.visitEnd();
        }
        {
            final MethodVisitor mvSize = cwPrimitiveArrayBaseImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "size", "()I", null, null);
            mvSize.visitCode();

            switch (size) {
                case 0: {
                    mvSize.visitInsn(ICONST_0);
                    break;
                }
                case 1: {
                    mvSize.visitInsn(ICONST_1);
                    break;
                }
                case 2: {
                    mvSize.visitInsn(ICONST_2);
                    break;
                }
                case 3: {
                    mvSize.visitInsn(ICONST_3);
                    break;
                }
                case 4: {
                    mvSize.visitInsn(ICONST_4);
                    break;
                }
                case 5: {
                    mvSize.visitInsn(ICONST_5);
                    break;
                }
                default: {
                    if (size <= Byte.MAX_VALUE) {
                        mvSize.visitIntInsn(BIPUSH, size);
                    } else if (size <= Short.MAX_VALUE) {
                        mvSize.visitIntInsn(SIPUSH, size);
                    } else {
                        mvSize.visitLdcInsn(size);
                    }
                }
            }
            mvSize.visitInsn(IRETURN);

            mvSize.visitMaxs(1, 1);
            mvSize.visitEnd();
        }
        cwPrimitiveArrayBaseImpl.visitEnd();
        return cwPrimitiveArrayBaseImpl.toByteArray();
    }

    private static final int FrecInlineSize = 325;
    private static final int FrecInlineSize2 = 175;
    private static final int MaxInlineSize = 35;
    private static final String[] StringThrowableArray = new String[] {"java/lang/Throwable"};

    protected static <T, E, R, TE extends Throwable> byte[] generateEventBusImplParts(
            String className,
            Class<T> interfaceEventBusClass,
            String interfaceMethodName,
            MethodType interfaceMethodType,
            Class<E> eventClass,
            Class<R> resultClass,
            int handlerCount,
            //MethodHandle[] handlers,
            //MethodHandle defaultResultSupplier, // ()R
            //MethodHandle shouldBreakTest, // (R)boolean
            Class<TE> exceptionType,
            //MethodHandle exceptionHandler, // (TE, E)R
            boolean handleExceptionForEveryHandler,
            int maxMethodBytecodeSize
    ) {
        if (maxMethodBytecodeSize < 35)
            throw new IllegalArgumentException("MaxMethodBytecodeSize too small: " + maxMethodBytecodeSize);
        // no idea about how to make multiclass... 5000 for safe
        if (handlerCount > 5000)
            throw new IllegalArgumentException("Handlers too many: " + handlerCount);
        final Type exceptionTypeType;
        final String exceptionTypeName;
        final boolean hasExceptionHandler = exceptionType != null;
        if (!hasExceptionHandler) {
            handleExceptionForEveryHandler = false;
            exceptionTypeType = null;
            exceptionTypeName = null;
        } else {
            exceptionTypeType = Type.getType(exceptionType);
            exceptionTypeName = exceptionTypeType.getInternalName();
        }
        final Type eventClassType = Type.getType(eventClass);
        final String eventClassDesc = eventClassType.getDescriptor();
        final Type resultClassType = Type.getType(resultClass);
        final String resultClassDesc = resultClassType.getDescriptor();
        if (eventClassType.getSort() == Type.VOID || eventClassType.getSort() == Type.METHOD) {
            throw new IllegalArgumentException("Illegal eventClass type: " + eventClassType.getSort());
        } else if (resultClassType.getSort() == Type.METHOD) {
            throw new IllegalArgumentException("Illegal resultClass type: " + resultClassType.getSort());
        }
        final boolean resultClassTypeNotVoid = resultClassType.getSort() != Type.VOID;
        final String funcResultBooleanDesc = resultClassTypeNotVoid ? ("(" + resultClassDesc + ")Z") : null;
        final String supplierResultDesc = resultClassTypeNotVoid ? ("()" + resultClassDesc) : null;
        final String funcEventResultDesc = "(" + eventClassDesc + ")" + resultClassDesc;
        final String funcExceptionEventResultDesc = hasExceptionHandler ? ("(" + exceptionTypeType.getDescriptor() + eventClassDesc + ")" + resultClassDesc) : null;

        //noinspection ConstantValue
        if (handlerCount <= 5000) {
            final StringReuseHelper srh = new StringReuseHelper();
            final ClassWriter cwEventBusImpl = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

            final String interfaceClassName = interfaceEventBusClass.getName().replace('.', '/');
            cwEventBusImpl.visit(V1_8,
                    ACC_PUBLIC | ACC_FINAL,
                    srh.addL(className),
                    null,
                    "java/lang/Object",
                    new String[] {
                            srh.addL(interfaceClassName)
                    }
            );
            final String defaultResultSupplierMHFieldName, shouldBreakTestMHFieldName;
            if (resultClassTypeNotVoid) {
                defaultResultSupplierMHFieldName = srh.poolUnusedPlaceholder();
                shouldBreakTestMHFieldName = srh.poolUnusedPlaceholder();
                {
                    {
                        final FieldVisitor fvDefaultResultSupplierMH = cwEventBusImpl.visitField(ACC_PUBLIC | ACC_FINAL, defaultResultSupplierMHFieldName, "Ljava/lang/invoke/MethodHandle;", null, null);
                        fvDefaultResultSupplierMH.visitEnd();
                    }
                    {
                        final MethodVisitor mvGetDefaultResult = cwEventBusImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, defaultResultSupplierMHFieldName, supplierResultDesc, null, StringThrowableArray);
                        mvGetDefaultResult.visitVarInsn(ALOAD, 0);
                        mvGetDefaultResult.visitFieldInsn(GETFIELD, className, defaultResultSupplierMHFieldName, "Ljava/lang/invoke/MethodHandle;");
                        mvGetDefaultResult.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", supplierResultDesc, false);
                        switch (resultClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvGetDefaultResult.visitInsn(IRETURN);
                                break;
                            case Type.DOUBLE:
                                mvGetDefaultResult.visitInsn(DRETURN);
                                break;
                            case Type.FLOAT:
                                mvGetDefaultResult.visitInsn(FRETURN);
                                break;
                            case Type.LONG:
                                mvGetDefaultResult.visitInsn(LRETURN);
                                break;
                            default:
                                mvGetDefaultResult.visitInsn(ARETURN);
                        }
                        mvGetDefaultResult.visitMaxs(-1, -1);
                        mvGetDefaultResult.visitEnd();
                    }
                }
                {
                    {
                        final FieldVisitor fvShouldBreakTestMH = cwEventBusImpl.visitField(ACC_PUBLIC | ACC_FINAL, shouldBreakTestMHFieldName, "Ljava/lang/invoke/MethodHandle;", null, null);
                        fvShouldBreakTestMH.visitEnd();
                    }
                    {
                        final MethodVisitor mvShouldBreakTest = cwEventBusImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, shouldBreakTestMHFieldName, funcResultBooleanDesc, null, StringThrowableArray);
                        mvShouldBreakTest.visitVarInsn(ALOAD, 0);
                        mvShouldBreakTest.visitFieldInsn(GETFIELD, className, shouldBreakTestMHFieldName, "Ljava/lang/invoke/MethodHandle;");
                        switch (resultClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvShouldBreakTest.visitVarInsn(ILOAD, 1);
                                break;
                            case Type.DOUBLE:
                                mvShouldBreakTest.visitVarInsn(DLOAD, 1);
                                break;
                            case Type.FLOAT:
                                mvShouldBreakTest.visitVarInsn(FLOAD, 1);
                                break;
                            case Type.LONG:
                                mvShouldBreakTest.visitVarInsn(LLOAD, 1);
                                break;
                            default:
                                mvShouldBreakTest.visitVarInsn(ALOAD, 1);
                        }
                        mvShouldBreakTest.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", funcResultBooleanDesc, false);
                        mvShouldBreakTest.visitInsn(IRETURN);
                        mvShouldBreakTest.visitMaxs(-1, -1);
                        mvShouldBreakTest.visitEnd();
                    }
                }
            } else {
                defaultResultSupplierMHFieldName = shouldBreakTestMHFieldName = null;
            }
            final String exceptionHandlerMHFieldName;
            if (hasExceptionHandler) {
                exceptionHandlerMHFieldName = srh.poolUnusedPlaceholder();
                {
                    final FieldVisitor fvExceptionHandlerMH = cwEventBusImpl.visitField(ACC_PUBLIC | ACC_FINAL, exceptionHandlerMHFieldName, "Ljava/lang/invoke/MethodHandle;", null, null);
                    fvExceptionHandlerMH.visitEnd();
                }
                {
                    final MethodVisitor mvExceptionHandler = cwEventBusImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, exceptionHandlerMHFieldName, funcExceptionEventResultDesc, null, StringThrowableArray);
                    mvExceptionHandler.visitVarInsn(ALOAD, 0);
                    mvExceptionHandler.visitFieldInsn(GETFIELD, className, exceptionHandlerMHFieldName, "Ljava/lang/invoke/MethodHandle;");
                    mvExceptionHandler.visitVarInsn(ALOAD, 1);
                    switch (eventClassType.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.INT:
                        case Type.SHORT:
                            mvExceptionHandler.visitVarInsn(ILOAD, 2);
                            break;
                        case Type.DOUBLE:
                            mvExceptionHandler.visitVarInsn(DLOAD, 2);
                            break;
                        case Type.FLOAT:
                            mvExceptionHandler.visitVarInsn(FLOAD, 2);
                            break;
                        case Type.LONG:
                            mvExceptionHandler.visitVarInsn(LLOAD, 2);
                            break;
                        default:
                            mvExceptionHandler.visitVarInsn(ALOAD, 2);
                    }
                    mvExceptionHandler.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", funcExceptionEventResultDesc, false);
                    switch (resultClassType.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.INT:
                        case Type.SHORT:
                            mvExceptionHandler.visitInsn(IRETURN);
                            break;
                        case Type.DOUBLE:
                            mvExceptionHandler.visitInsn(DRETURN);
                            break;
                        case Type.FLOAT:
                            mvExceptionHandler.visitInsn(FRETURN);
                            break;
                        case Type.LONG:
                            mvExceptionHandler.visitInsn(LRETURN);
                            break;
                        case Type.VOID:
                            mvExceptionHandler.visitInsn(RETURN);
                            break;
                        default:
                            mvExceptionHandler.visitInsn(ARETURN);
                    }
                    mvExceptionHandler.visitMaxs(-1, -1);
                    mvExceptionHandler.visitEnd();
                }
            } else {
                exceptionHandlerMHFieldName = null;
            }
            final String[] handlerMHFieldAndMethodNames = new String[handlerCount];
            for (int i = 0; i < handlerCount; ++i) {
                final String name = srh.poolUnusedPlaceholder();
                handlerMHFieldAndMethodNames[i] = name;
                {
                    final FieldVisitor fvHandlerMH = cwEventBusImpl.visitField(ACC_PUBLIC | ACC_FINAL, name, "Ljava/lang/invoke/MethodHandle;", null, null);
                    fvHandlerMH.visitEnd();
                }
                {
                    final MethodVisitor mvCallHandler = cwEventBusImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, name, funcEventResultDesc, null, StringThrowableArray);
                    mvCallHandler.visitCode();

                    mvCallHandler.visitVarInsn(ALOAD, 0);
                    mvCallHandler.visitFieldInsn(GETFIELD, className, name, "Ljava/lang/invoke/MethodHandle;");
                    if (handleExceptionForEveryHandler) {
                        Label tryStart = new Label();
                        Label tryEnd = new Label();
                        Label exceptionHandlerStart = new Label();
                        mvCallHandler.visitTryCatchBlock(tryStart, tryEnd, exceptionHandlerStart, exceptionTypeName);
                        switch (eventClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvCallHandler.visitVarInsn(ILOAD, 1);
                                break;
                            case Type.DOUBLE:
                                mvCallHandler.visitVarInsn(DLOAD, 1);
                                break;
                            case Type.FLOAT:
                                mvCallHandler.visitVarInsn(FLOAD, 1);
                                break;
                            case Type.LONG:
                                mvCallHandler.visitVarInsn(LLOAD, 1);
                                break;
                            default:
                                mvCallHandler.visitVarInsn(ALOAD, 1);
                        }
                        mvCallHandler.visitLabel(tryStart);
                        mvCallHandler.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", funcEventResultDesc, false);
                        mvCallHandler.visitLabel(tryEnd);
                        switch (resultClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvCallHandler.visitInsn(IRETURN);
                                break;
                            case Type.DOUBLE:
                                mvCallHandler.visitInsn(DRETURN);
                                break;
                            case Type.FLOAT:
                                mvCallHandler.visitInsn(FRETURN);
                                break;
                            case Type.LONG:
                                mvCallHandler.visitInsn(LRETURN);
                                break;
                            case Type.VOID:
                                mvCallHandler.visitInsn(RETURN);
                                break;
                            default:
                                mvCallHandler.visitInsn(ARETURN);
                        }
                        mvCallHandler.visitLabel(exceptionHandlerStart);
                        mvCallHandler.visitVarInsn(ALOAD, 0);
                        mvCallHandler.visitInsn(SWAP);
                        switch (eventClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvCallHandler.visitVarInsn(ILOAD, 1);
                                break;
                            case Type.DOUBLE:
                                mvCallHandler.visitVarInsn(DLOAD, 1);
                                break;
                            case Type.FLOAT:
                                mvCallHandler.visitVarInsn(FLOAD, 1);
                                break;
                            case Type.LONG:
                                mvCallHandler.visitVarInsn(LLOAD, 1);
                                break;
                            default:
                                mvCallHandler.visitVarInsn(ALOAD, 1);
                        }
                        mvCallHandler.visitMethodInsn(INVOKEVIRTUAL, className, exceptionHandlerMHFieldName, funcExceptionEventResultDesc, false);
                        switch (resultClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvCallHandler.visitInsn(IRETURN);
                                break;
                            case Type.DOUBLE:
                                mvCallHandler.visitInsn(DRETURN);
                                break;
                            case Type.FLOAT:
                                mvCallHandler.visitInsn(FRETURN);
                                break;
                            case Type.LONG:
                                mvCallHandler.visitInsn(LRETURN);
                                break;
                            case Type.VOID:
                                mvCallHandler.visitInsn(RETURN);
                                break;
                            default:
                                mvCallHandler.visitInsn(ARETURN);
                        }
                    } else {
                        switch (eventClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvCallHandler.visitVarInsn(ILOAD, 1);
                                break;
                            case Type.DOUBLE:
                                mvCallHandler.visitVarInsn(DLOAD, 1);
                                break;
                            case Type.FLOAT:
                                mvCallHandler.visitVarInsn(FLOAD, 1);
                                break;
                            case Type.LONG:
                                mvCallHandler.visitVarInsn(LLOAD, 1);
                                break;
                            default:
                                mvCallHandler.visitVarInsn(ALOAD, 1);
                        }
                        mvCallHandler.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", funcEventResultDesc, false);
                        switch (resultClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvCallHandler.visitInsn(IRETURN);
                                break;
                            case Type.DOUBLE:
                                mvCallHandler.visitInsn(DRETURN);
                                break;
                            case Type.FLOAT:
                                mvCallHandler.visitInsn(FRETURN);
                                break;
                            case Type.LONG:
                                mvCallHandler.visitInsn(LRETURN);
                                break;
                            case Type.VOID:
                                mvCallHandler.visitInsn(RETURN);
                                break;
                            default:
                                mvCallHandler.visitInsn(ARETURN);
                        }
                    }

                    mvCallHandler.visitMaxs(-1, -1);
                    mvCallHandler.visitEnd();
                }
            }
            ArrayDeque<String> methodsToLink = new ArrayDeque<>(Arrays.asList(handlerMHFieldAndMethodNames));
            // non-void 14round + 7, void 5round + 1
            while (methodsToLink.size() > 1) {
                final String name = srh.poolUnusedPlaceholder();
                final MethodVisitor mvHandlerBridge = cwEventBusImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, name, funcEventResultDesc, null, StringThrowableArray);
                mvHandlerBridge.visitCode();

                final int step = resultClassTypeNotVoid ? 14 : 5;
                int i = step;
                String currentMethodToLink;
                Label returnCachedResult = resultClassTypeNotVoid ? new Label() : null;
                while (i + (resultClassTypeNotVoid ? 7 : 1) <= maxMethodBytecodeSize && (currentMethodToLink = methodsToLink.poll()) != null) {
                    i += step;
                    if (resultClassTypeNotVoid) {
                        mvHandlerBridge.visitVarInsn(ALOAD, 0);
                    }
                    mvHandlerBridge.visitVarInsn(ALOAD, 0);
                    switch (eventClassType.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.INT:
                        case Type.SHORT:
                            mvHandlerBridge.visitVarInsn(ILOAD, 1);
                            break;
                        case Type.DOUBLE:
                            mvHandlerBridge.visitVarInsn(DLOAD, 1);
                            break;
                        case Type.FLOAT:
                            mvHandlerBridge.visitVarInsn(FLOAD, 1);
                            break;
                        case Type.LONG:
                            mvHandlerBridge.visitVarInsn(LLOAD, 1);
                            break;
                        default:
                            mvHandlerBridge.visitVarInsn(ALOAD, 1);
                    }
                    mvHandlerBridge.visitMethodInsn(INVOKEVIRTUAL, className, currentMethodToLink, funcEventResultDesc, false);
                    if (resultClassTypeNotVoid) {
                        switch (resultClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvHandlerBridge.visitVarInsn(ISTORE, 3);
                                mvHandlerBridge.visitVarInsn(ILOAD, 3);
                                break;
                            case Type.DOUBLE:
                                mvHandlerBridge.visitVarInsn(DSTORE, 3);
                                mvHandlerBridge.visitVarInsn(DLOAD, 3);
                                break;
                            case Type.FLOAT:
                                mvHandlerBridge.visitVarInsn(FSTORE, 3);
                                mvHandlerBridge.visitVarInsn(FLOAD, 3);
                                break;
                            case Type.LONG:
                                mvHandlerBridge.visitVarInsn(LSTORE, 3);
                                mvHandlerBridge.visitVarInsn(LLOAD, 3);
                                break;
                            default:
                                mvHandlerBridge.visitVarInsn(ASTORE, 3);
                                mvHandlerBridge.visitVarInsn(ALOAD, 3);
                        }
                        mvHandlerBridge.visitMethodInsn(INVOKEVIRTUAL, className, shouldBreakTestMHFieldName, funcResultBooleanDesc, false);
                        mvHandlerBridge.visitJumpInsn(IFNE, returnCachedResult);
                    }
                }
                if (resultClassTypeNotVoid) {
                    mvHandlerBridge.visitVarInsn(ALOAD, 0);
                    mvHandlerBridge.visitMethodInsn(INVOKEVIRTUAL, className, defaultResultSupplierMHFieldName, supplierResultDesc, false);
                    switch (resultClassType.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.INT:
                        case Type.SHORT:
                            mvHandlerBridge.visitInsn(IRETURN);
                            break;
                        case Type.DOUBLE:
                            mvHandlerBridge.visitInsn(DRETURN);
                            break;
                        case Type.FLOAT:
                            mvHandlerBridge.visitInsn(FRETURN);
                            break;
                        case Type.LONG:
                            mvHandlerBridge.visitInsn(LRETURN);
                            break;
                        default:
                            mvHandlerBridge.visitInsn(ARETURN);
                    }
                    mvHandlerBridge.visitLabel(returnCachedResult);
                }
                switch (resultClassType.getSort()) {
                    case Type.BOOLEAN:
                    case Type.BYTE:
                    case Type.CHAR:
                    case Type.INT:
                    case Type.SHORT:
                        mvHandlerBridge.visitVarInsn(ILOAD, 3);
                        mvHandlerBridge.visitInsn(IRETURN);
                        break;
                    case Type.DOUBLE:
                        mvHandlerBridge.visitVarInsn(DLOAD, 3);
                        mvHandlerBridge.visitInsn(DRETURN);
                        break;
                    case Type.FLOAT:
                        mvHandlerBridge.visitVarInsn(FLOAD, 3);
                        mvHandlerBridge.visitInsn(FRETURN);
                        break;
                    case Type.LONG:
                        mvHandlerBridge.visitVarInsn(LLOAD, 3);
                        mvHandlerBridge.visitInsn(LRETURN);
                        break;
                    case Type.VOID:
                        mvHandlerBridge.visitInsn(RETURN);
                        break;
                    default:
                        mvHandlerBridge.visitVarInsn(ALOAD, 3);
                        mvHandlerBridge.visitInsn(ARETURN);
                }

                mvHandlerBridge.visitMaxs(-1, -1);
                mvHandlerBridge.visitEnd();
                methodsToLink.add(name);
            }
            switch (methodsToLink.size()) {
                case 0:
                case 1: {
                    final MethodVisitor mvHandlerFinalBridge = cwEventBusImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, interfaceMethodName, funcEventResultDesc, null, null);
                    mvHandlerFinalBridge.visitCode();

                    final String lastBridgeName = methodsToLink.peek();
                    final Label tryStart = new Label();
                    final Label tryEnd = new Label();
                    final boolean handleExceptionFromHandlers = hasExceptionHandler && !handleExceptionForEveryHandler;
                    final Label handlersExceptionHandler = handleExceptionFromHandlers ? new Label() : null;
                    final Label handlersExceptionHandlerEnd = handleExceptionFromHandlers ? new Label() : null;
                    final Label throwableHandler = new Label();
                    if (handleExceptionFromHandlers) {
                        mvHandlerFinalBridge.visitTryCatchBlock(tryStart, tryEnd, handlersExceptionHandler, exceptionTypeName);
                        mvHandlerFinalBridge.visitTryCatchBlock(tryStart, handlersExceptionHandlerEnd, throwableHandler, "java/lang/Throwable");
                    } else {
                        mvHandlerFinalBridge.visitTryCatchBlock(tryStart, tryEnd, throwableHandler, "java/lang/Throwable");
                    }
                    mvHandlerFinalBridge.visitVarInsn(ALOAD, 0);
                    if (lastBridgeName != null) {
                        switch (eventClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvHandlerFinalBridge.visitVarInsn(ILOAD, 1);
                                break;
                            case Type.DOUBLE:
                                mvHandlerFinalBridge.visitVarInsn(DLOAD, 1);
                                break;
                            case Type.FLOAT:
                                mvHandlerFinalBridge.visitVarInsn(FLOAD, 1);
                                break;
                            case Type.LONG:
                                mvHandlerFinalBridge.visitVarInsn(LLOAD, 1);
                                break;
                            default:
                                mvHandlerFinalBridge.visitVarInsn(ALOAD, 1);
                        }
                        mvHandlerFinalBridge.visitLabel(tryStart);
                        mvHandlerFinalBridge.visitMethodInsn(INVOKEVIRTUAL, className, lastBridgeName, funcEventResultDesc, false);
                    } else {
                        mvHandlerFinalBridge.visitLabel(tryStart);
                        mvHandlerFinalBridge.visitMethodInsn(INVOKEVIRTUAL, className, defaultResultSupplierMHFieldName, supplierResultDesc, false);
                    }
                    mvHandlerFinalBridge.visitLabel(tryEnd);
                    switch (resultClassType.getSort()) {
                        case Type.BOOLEAN:
                        case Type.BYTE:
                        case Type.CHAR:
                        case Type.INT:
                        case Type.SHORT:
                            mvHandlerFinalBridge.visitInsn(IRETURN);
                            break;
                        case Type.DOUBLE:
                            mvHandlerFinalBridge.visitInsn(DRETURN);
                            break;
                        case Type.FLOAT:
                            mvHandlerFinalBridge.visitInsn(FRETURN);
                            break;
                        case Type.LONG:
                            mvHandlerFinalBridge.visitInsn(LRETURN);
                            break;
                        case Type.VOID:
                            mvHandlerFinalBridge.visitInsn(RETURN);
                            break;
                        default:
                            mvHandlerFinalBridge.visitInsn(ARETURN);
                    }
                    if (handleExceptionFromHandlers) {
                        mvHandlerFinalBridge.visitLabel(handlersExceptionHandler);
                        mvHandlerFinalBridge.visitVarInsn(ALOAD, 0);
                        mvHandlerFinalBridge.visitInsn(SWAP);
                        switch (eventClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvHandlerFinalBridge.visitVarInsn(ILOAD, 1);
                                break;
                            case Type.DOUBLE:
                                mvHandlerFinalBridge.visitVarInsn(DLOAD, 1);
                                break;
                            case Type.FLOAT:
                                mvHandlerFinalBridge.visitVarInsn(FLOAD, 1);
                                break;
                            case Type.LONG:
                                mvHandlerFinalBridge.visitVarInsn(LLOAD, 1);
                                break;
                            default:
                                mvHandlerFinalBridge.visitVarInsn(ALOAD, 1);
                        }
                        mvHandlerFinalBridge.visitMethodInsn(INVOKEVIRTUAL, className, exceptionHandlerMHFieldName, funcExceptionEventResultDesc, false);
                        switch (resultClassType.getSort()) {
                            case Type.BOOLEAN:
                            case Type.BYTE:
                            case Type.CHAR:
                            case Type.INT:
                            case Type.SHORT:
                                mvHandlerFinalBridge.visitInsn(IRETURN);
                                break;
                            case Type.DOUBLE:
                                mvHandlerFinalBridge.visitInsn(DRETURN);
                                break;
                            case Type.FLOAT:
                                mvHandlerFinalBridge.visitInsn(FRETURN);
                                break;
                            case Type.LONG:
                                mvHandlerFinalBridge.visitInsn(LRETURN);
                                break;
                            case Type.VOID:
                                mvHandlerFinalBridge.visitInsn(RETURN);
                                break;
                            default:
                                mvHandlerFinalBridge.visitInsn(ARETURN);
                        }
                        mvHandlerFinalBridge.visitLabel(handlersExceptionHandlerEnd);
                    }
                    mvHandlerFinalBridge.visitLabel(throwableHandler);
                    mvHandlerFinalBridge.visitVarInsn(ASTORE, 3);
                    mvHandlerFinalBridge.visitTypeInsn(NEW, "java/lang/RuntimeException");
                    mvHandlerFinalBridge.visitInsn(DUP);
                    mvHandlerFinalBridge.visitVarInsn(ALOAD, 3);
                    mvHandlerFinalBridge.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/Throwable;)V", false);
                    mvHandlerFinalBridge.visitInsn(ATHROW);

                    mvHandlerFinalBridge.visitMaxs(-1, -1);
                    mvHandlerFinalBridge.visitEnd();
                    break;
                }
                default:
                    throw new AssertionError();
            }
            {
                // init(MH[] handlers, ?MH defaultResultSupplier, ?MH shouldBreakTest, ?MH exceptionHandler)
                final MethodVisitor mvInit = cwEventBusImpl.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/invoke/MethodHandle;" + (resultClassTypeNotVoid ? "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodHandle;" : "") + (hasExceptionHandler ? "Ljava/lang/invoke/MethodHandle;" : "") + ")V", null, null);
                mvInit.visitCode();

                mvInit.visitVarInsn(ALOAD, 0);
                mvInit.visitMethodInsn(
                        INVOKESPECIAL,
                        "java/lang/Object",
                        "<init>",
                        srh.addL("()V"),
                        false
                );

                for (int i = 0; i < handlerCount; ++i) {
                    mvInit.visitVarInsn(ALOAD, 0);
                    mvInit.visitVarInsn(ALOAD, 1);
                    switch (i) {
                        case 0:
                            mvInit.visitInsn(ICONST_0);
                            break;
                        case 1:
                            mvInit.visitInsn(ICONST_1);
                            break;
                        case 2:
                            mvInit.visitInsn(ICONST_2);
                            break;
                        case 3:
                            mvInit.visitInsn(ICONST_3);
                            break;
                        case 4:
                            mvInit.visitInsn(ICONST_4);
                            break;
                        case 5:
                            mvInit.visitInsn(ICONST_5);
                            break;
                        default:
                            if (i > Byte.MAX_VALUE) {
                                //noinspection ConstantValue
                                if (i > Short.MAX_VALUE) {
                                    mvInit.visitLdcInsn(i);
                                } else {
                                    mvInit.visitIntInsn(SIPUSH, i);
                                }
                            } else {
                                mvInit.visitIntInsn(BIPUSH, i);
                            }
                            break;
                    }
                    mvInit.visitInsn(AALOAD);
                    mvInit.visitFieldInsn(PUTFIELD, className, handlerMHFieldAndMethodNames[i], "Ljava/lang/invoke/MethodHandle;");
                }
                int index = 2;
                if (resultClassTypeNotVoid) {
                    mvInit.visitVarInsn(ALOAD, 0);
                    mvInit.visitVarInsn(ALOAD, index++);
                    mvInit.visitFieldInsn(PUTFIELD, className, defaultResultSupplierMHFieldName, "Ljava/lang/invoke/MethodHandle;");
                    mvInit.visitVarInsn(ALOAD, 0);
                    mvInit.visitVarInsn(ALOAD, index++);
                    mvInit.visitFieldInsn(PUTFIELD, className, shouldBreakTestMHFieldName, "Ljava/lang/invoke/MethodHandle;");
                }
                if (hasExceptionHandler) {
                    mvInit.visitVarInsn(ALOAD, 0);
                    mvInit.visitVarInsn(ALOAD, index);
                    mvInit.visitFieldInsn(PUTFIELD, className, exceptionHandlerMHFieldName, "Ljava/lang/invoke/MethodHandle;");
                }

                mvInit.visitInsn(RETURN);
                mvInit.visitMaxs(-1, -1);
                mvInit.visitEnd();
            }
            return cwEventBusImpl.toByteArray();
        }
        throw new AssertionError();
    }

    private static String getMergedInitDesc(String[] recordArgMethodReturnTypes) {
        int capacity = 3;
        for (String recordArgMethodReturnType : recordArgMethodReturnTypes)
            capacity += recordArgMethodReturnType.length();
        final StringBuilder mergedInitDescBuilder = new StringBuilder(capacity);
        mergedInitDescBuilder.append("(");
        for (String recordArgMethodReturnType : recordArgMethodReturnTypes)
            mergedInitDescBuilder.append(recordArgMethodReturnType);
        mergedInitDescBuilder.append(")V");
        return mergedInitDescBuilder.toString();
    }

    private static void pushToString(final MethodVisitor mv, final String type) {
        switch (type) {
            case "Z":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "toString", "(Z)Ljava/lang/String;", false);
                break;
            case "C":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "toString", "(C)Ljava/lang/String;", false);
                break;
            case "B":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "toString", "(B)Ljava/lang/String;", false);
                break;
            case "S":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "toString", "(S)Ljava/lang/String;", false);
                break;
            case "I":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "toString", "(I)Ljava/lang/String;", false);
                break;
            case "F":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "toString", "(F)Ljava/lang/String;", false);
                break;
            case "J":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "toString", "(J)Ljava/lang/String;", false);
                break;
            case "D":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "toString", "(D)Ljava/lang/String;", false);
                break;
            default:
                mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "toString", "(Ljava/lang/Object;)Ljava/lang/String;", false);
        }
    }

    private static void pushHashCode(final MethodVisitor mv, final String type) {
        switch (type) {
            case "Z":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "hashCode", "(Z)I", false);
                break;
            case "C":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "hashCode", "(C)I", false);
                break;
            case "B":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "hashCode", "(B)I", false);
                break;
            case "S":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "hashCode", "(S)I", false);
                break;
            case "I":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "hashCode", "(I)I", false);
                break;
            case "F":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "hashCode", "(F)I", false);
                break;
            case "J":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "hashCode", "(J)I", false);
                break;
            case "D":
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "hashCode", "(D)I", false);
                break;
            default:
                mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "hashCode", "(Ljava/lang/Object;)I", false);
        }
    }

    protected static String getPrimitiveTypeName(Class<?> primitiveType) {
        if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException("Not primitive");
        }
        switch (Type.getType(primitiveType).getSort()) {
            case Type.BOOLEAN: {
                return "Boolean";
            }
            case Type.CHAR: {
                return "Char";
            }
            case Type.BYTE: {
                return "Byte";
            }
            case Type.SHORT: {
                return "Short";
            }
            case Type.INT: {
                return "Int";
            }
            case Type.FLOAT: {
                return "Float";
            }
            case Type.LONG: {
                return "Long";
            }
            case Type.DOUBLE: {
                return "Double";
            }
            case Type.VOID: {
                return "Void";
            }
            default: {
                throw new AssertionError();
            }
        }
    }

    protected static Class<? extends AbstractPrimitiveConstantArray<?>> getPrimitiveConstantArrayClass(Class<?> primitiveType) {
        if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException("Not primitive");
        } else if (primitiveType == void.class) {
            throw new IllegalArgumentException("Void");
        }
        switch (Type.getType(primitiveType).getSort()) {
            case Type.BOOLEAN: {
                return BooleanConstantArray.class;
            }
            case Type.CHAR: {
                return CharConstantArray.class;
            }
            case Type.BYTE: {
                return ByteConstantArray.class;
            }
            case Type.SHORT: {
                return ShortConstantArray.class;
            }
            case Type.INT: {
                return IntConstantArray.class;
            }
            case Type.FLOAT: {
                return FloatConstantArray.class;
            }
            case Type.LONG: {
                return LongConstantArray.class;
            }
            case Type.DOUBLE: {
                return DoubleConstantArray.class;
            }
            default: {
                throw new AssertionError();
            }
        }
    }

    protected static void wrapPrimitiveOnStack(Class<?> primitiveType, MethodVisitor mv) {
        if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException("Not primitive");
        } else if (primitiveType == void.class) {
            throw new IllegalArgumentException("Void");
        }
        switch (Type.getType(primitiveType).getSort()) {
            case Type.BOOLEAN: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                return;
            }
            case Type.CHAR: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                return;
            }
            case Type.BYTE: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                return;
            }
            case Type.SHORT: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
                return;
            }
            case Type.INT: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                return;
            }
            case Type.FLOAT: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                return;
            }
            case Type.LONG: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                return;
            }
            case Type.DOUBLE: {
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                return;
            }
            default: {
                throw new AssertionError();
            }
        }
    }

    protected static void pushPrimitiveTypeOnStack(Class<?> primitiveType, MethodVisitor mv) {
        if (!primitiveType.isPrimitive()) {
            throw new IllegalArgumentException("Not primitive");
        }
        switch (Type.getType(primitiveType).getSort()) {
            case Type.BOOLEAN: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.CHAR: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.BYTE: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.SHORT: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.INT: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.FLOAT: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.LONG: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.DOUBLE: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
                return;
            }
            case Type.VOID: {
                mv.visitFieldInsn(GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
                return;
            }
            default: {
                throw new AssertionError();
            }
        }
    }
}
