package me.z7087.final2constant;

import org.objectweb.asm.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;

public abstract class ConstantFactory {
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

        return this.ofRecordConstructor(hostClass, recordInterfaceClass, recordImmutableArgMethodNames, recordImmutableArgMethodTypes, null, null, generateToStringHashCodeEquals, generateSetterForFinalFields);
    }

    public abstract <T> MethodHandle ofRecordConstructor(MethodHandles.Lookup hostClass,
                                                         Class<T> recordInterfaceClass,
                                                         String[] recordImmutableArgMethodNames,
                                                         String[] recordImmutableArgMethodTypes,
                                                         String[] recordMutableArgMethodNames,
                                                         String[] recordMutableArgMethodTypes,
                                                         boolean generateToStringHashCodeEquals,
                                                         boolean generateSetterForFinalFields
    );

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

    protected static byte[] generateDynamicConstantImpl(String className) {
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
        {
            final MethodVisitor mvSync = cwDynamicConstantImpl.visitMethod(ACC_PUBLIC | ACC_FINAL, "sync", "()V", null, null);
            mvSync.visitCode();
            mvSync.visitVarInsn(ALOAD, 0);
            mvSync.visitFieldInsn(GETFIELD, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvSync.visitTypeInsn(INSTANCEOF, "java/lang/invoke/MutableCallSite");
            Label End = new Label();
            mvSync.visitJumpInsn(IFEQ, End);

            mvSync.visitInsn(ICONST_1);
            mvSync.visitTypeInsn(ANEWARRAY, "java/lang/invoke/MutableCallSite");
            mvSync.visitInsn(DUP);
            mvSync.visitInsn(ICONST_0);
            mvSync.visitVarInsn(ALOAD, 0);
            mvSync.visitFieldInsn(GETFIELD, className, "callSite", "Ljava/lang/invoke/CallSite;");
            mvSync.visitTypeInsn(CHECKCAST, "java/lang/invoke/MutableCallSite");
            mvSync.visitInsn(AASTORE);
            mvSync.visitMethodInsn(INVOKESTATIC, "java/lang/invoke/MutableCallSite", "syncAll", "([Ljava/lang/invoke/MutableCallSite;)V", false);

            mvSync.visitLabel(End);
            mvSync.visitInsn(RETURN);
            mvSync.visitMaxs(3, 2);
            mvSync.visitEnd();
        }
        cwDynamicConstantImpl.visitEnd();
        return cwDynamicConstantImpl.toByteArray();
    }
    
    protected static <T> byte[] generateRecordImpl(String className,
                                                   String simpleClassName,
                                                   Class<T> recordInterfaceClass,
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
        final String recordInterfaceClassName = recordInterfaceClass.getName().replace('.', '/');
        cwRecordImpl.visit(V1_8,
                ACC_PUBLIC | ACC_FINAL,
                className,
                null,
                "java/lang/Object",
                new String[] {
                        recordInterfaceClassName
                });
        generateRecordImpl_visitImmutableFields(cwRecordImpl, recordImmutableArgCount, recordImmutableArgMethodNames, recordImmutableArgMethodReturnTypes);
        generateRecordImpl_visitMutableFields(cwRecordImpl, recordMutableArgCount, recordMutableArgMethodNames, recordMutableArgMethodReturnTypes);
        // no need to init mutable fields in the constructor
        generateRecordImpl_visitInitAndGetConstructorMH(
                cwRecordImpl,
                className,
                recordInterfaceClass,
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
                                                                            Class<T> recordInterfaceClass,
                                                                            int recordImmutableArgCount,
                                                                            String[] recordImmutableArgMethodNames,
                                                                            String[] recordImmutableArgMethodReturnTypes
    ) {
        final String mergedInitDesc = getMergedInitDesc(recordImmutableArgMethodReturnTypes);
        {
            final MethodVisitor mvInit = cwRecordImpl.visitMethod(ACC_PUBLIC, "<init>", mergedInitDesc, null, null);
            mvInit.visitCode();

            mvInit.visitVarInsn(ALOAD, 0);
            mvInit.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

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
                        if (recordArgMethodReturnType.charAt(0) != 'L')
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
            mvConstructorMHGetter.visitLdcInsn(Type.getType("L" + recordInterfaceClass.getName().replace('.', '/') + ";"));
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
}
