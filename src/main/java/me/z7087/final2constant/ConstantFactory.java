package me.z7087.final2constant;

import org.objectweb.asm.*;

import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;

public abstract class ConstantFactory {
    public abstract <T> Constant<T> of(T value);

    public abstract <T> DynamicConstant<T> ofMutable(T value);

    public abstract <T> DynamicConstant<T> ofVolatile(T value);

    public abstract <T> Constant<T> ofLazy(Supplier<? extends T> supplier);

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
}
