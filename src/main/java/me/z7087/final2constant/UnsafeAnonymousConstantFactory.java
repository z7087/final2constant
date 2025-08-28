package me.z7087.final2constant;

import sun.misc.Unsafe;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Supplier;

class UnsafeAnonymousConstantFactory extends ConstantFactory {
    private static final Unsafe theUnsafe;
    private static final MethodHandle MHDefineAnonymousClass;
    static {
        Unsafe _theUnsafe;
        try {
            final Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            _theUnsafe = (Unsafe) Objects.requireNonNull(theUnsafeField.get(null));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            _theUnsafe = null;
        }
        theUnsafe = _theUnsafe;
        if (_theUnsafe != null) {
            MethodHandle _MHDefineAnonymousClass;
            try {
                //noinspection JavaLangInvokeHandleSignature
                _MHDefineAnonymousClass = MethodHandles.lookup().findVirtual(Unsafe.class, "defineAnonymousClass", MethodType.methodType(Class.class, Class.class, byte[].class, Object[].class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                _MHDefineAnonymousClass = null;
            }
            MHDefineAnonymousClass = _MHDefineAnonymousClass;
        } else {
            MHDefineAnonymousClass = null;
        }
    }

    private static final MethodHandle ConstantImplConstructor;
    private static final MethodHandle LazyConstantImplConstructor;
    private static final MethodHandle DynamicMutableConstantImplConstructor;
    private static final MethodHandle DynamicVolatileConstantImplConstructor;
    private static final boolean valid;

    static {
        if (theUnsafe != null && MHDefineAnonymousClass != null) {
            final String UACFClassName = UnsafeAnonymousConstantFactory.class.getName().replace('.', '/');
            {
                try {
                    Class<?> clazz = (Class<?>) MHDefineAnonymousClass.invokeExact(theUnsafe, UnsafeAnonymousConstantFactory.class, generateConstantImpl(UACFClassName + "$ConstantImpl"), (Object[]) null);
                    ConstantImplConstructor = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, Object.class)).asType(MethodType.methodType(Constant.class, Object.class));
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                try {
                    Class<?> clazz = (Class<?>) MHDefineAnonymousClass.invokeExact(theUnsafe, UnsafeAnonymousConstantFactory.class, generateLazyConstantImpl(UACFClassName + "$LazyConstantImpl"), (Object[]) null);
                    LazyConstantImplConstructor = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, Supplier.class)).asType(MethodType.methodType(Constant.class, Supplier.class));
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                try {
                    Class<?> clazz = (Class<?>) MHDefineAnonymousClass.invokeExact(theUnsafe, UnsafeAnonymousConstantFactory.class, generateDynamicConstantImpl(UACFClassName + "$DynamicConstantImpl", true), (Object[]) null);
                    DynamicMutableConstantImplConstructor = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, CallSite.class)).asType(MethodType.methodType(DynamicConstant.class, CallSite.class));
                    clazz = (Class<?>) MHDefineAnonymousClass.invokeExact(theUnsafe, UnsafeAnonymousConstantFactory.class, generateDynamicConstantImpl(UACFClassName + "$DynamicConstantImpl", false), (Object[]) null);
                    DynamicVolatileConstantImplConstructor = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, CallSite.class)).asType(MethodType.methodType(DynamicConstant.class, CallSite.class));
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            valid = true;
        } else {
            ConstantImplConstructor = LazyConstantImplConstructor = DynamicMutableConstantImplConstructor = DynamicVolatileConstantImplConstructor = MethodHandles.constant(Object.class, null);
            valid = false;
        }
    }

    public UnsafeAnonymousConstantFactory() {
        if (!valid) {
            if (theUnsafe == null)
                throw new IllegalStateException("Unsafe instance getting has failed, UnsafeAnonymousConstantFactory cannot be created!");
            throw new IllegalStateException("DefineAnonymousClass MethodHandle getting has failed, UnsafeAnonymousConstantFactory cannot be created!");
        }
    }

    @Override
    protected Class<?> defineClassAt(MethodHandles.Lookup hostClass, byte[] classBytes) throws Throwable {
        return defineClassWithPrivilegeAt(hostClass, classBytes);
    }

    @Override
    protected Class<?> defineClassWithPrivilegeAt(MethodHandles.Lookup hostClass, byte[] classBytes) throws Throwable {
        return (Class<?>) Objects.requireNonNull(MHDefineAnonymousClass).invokeExact(theUnsafe, hostClass.lookupClass(), classBytes, (Object[]) null);
    }

    @Override
    public <T> Constant<T> of(T value) {
        try {
            //noinspection unchecked
            return (Constant<T>) ConstantImplConstructor.invokeExact(value);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> DynamicConstant<T> ofMutable(T value) {
        try {
            //noinspection unchecked
            return (DynamicConstant<T>) DynamicMutableConstantImplConstructor.invokeExact((CallSite) new MutableCallSite(MethodHandles.constant(Object.class, value)));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> DynamicConstant<T> ofVolatile(T value) {
        try {
            //noinspection unchecked
            return (DynamicConstant<T>) DynamicVolatileConstantImplConstructor.invokeExact((CallSite) new VolatileCallSite(MethodHandles.constant(Object.class, value)));
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> Constant<T> ofLazy(Supplier<? extends T> supplier) {
        try {
            //noinspection unchecked
            return (Constant<T>) LazyConstantImplConstructor.invokeExact(supplier);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
