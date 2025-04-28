package me.z7087.final2constant;

import java.lang.invoke.*;
import java.lang.reflect.Array;
import java.util.function.Supplier;

class LookupHiddenConstantFactory extends ConstantFactory {
    //private static final Object ClassOptionEmptyArray;
    private static final MethodHandle MHDefineHiddenClass;
    static {
        Object ClassOptionEmptyArray;
        {
            Class<?> ClassOptionClass;
            try {
                ClassOptionClass = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption");
            } catch (ClassNotFoundException ignored) {
                ClassOptionClass = null;
            }
            ClassOptionEmptyArray = (ClassOptionClass != null) ? Array.newInstance(ClassOptionClass, 0) : null;
        }
        if (ClassOptionEmptyArray != null) {
            MethodHandle _MHDefineHiddenClass;
            try {
                //noinspection JavaLangInvokeHandleSignature
                _MHDefineHiddenClass = MethodHandles.lookup().findVirtual(MethodHandles.Lookup.class, "defineHiddenClass", MethodType.methodType(MethodHandles.Lookup.class, byte[].class, boolean.class, ClassOptionEmptyArray.getClass()));
                _MHDefineHiddenClass = MethodHandles.insertArguments(_MHDefineHiddenClass, 3, ClassOptionEmptyArray);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                _MHDefineHiddenClass = null;
            }
            MHDefineHiddenClass = _MHDefineHiddenClass;
        } else {
            MHDefineHiddenClass = null;
        }
    }

    private final MethodHandle ConstantImplConstructor;
    private final MethodHandle LazyConstantImplConstructor;
    private final MethodHandle DynamicConstantImplConstructor;

    {
        if (MHDefineHiddenClass == null)
            throw new IllegalStateException("DefineHiddenClass MethodHandle getting has failed, LookupHiddenConstantFactory cannot be created!");
        final String LHCFClassName = LookupHiddenConstantFactory.class.getName().replace('.', '/');
        {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClass.invokeExact(lookup, generateConstantImpl(LHCFClassName + "$ConstantImpl"), true)).lookupClass();
                ConstantImplConstructor = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, Object.class)).asType(MethodType.methodType(Constant.class, Object.class));
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            try {
                Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClass.invokeExact(lookup, generateLazyConstantImpl(LHCFClassName + "$LazyConstantImpl"), true)).lookupClass();
                LazyConstantImplConstructor = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, Supplier.class)).asType(MethodType.methodType(Constant.class, Supplier.class));
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
            try {
                Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClass.invokeExact(lookup, generateDynamicConstantImpl(LHCFClassName + "$DynamicConstantImpl"), true)).lookupClass();
                DynamicConstantImplConstructor = MethodHandles.lookup().findConstructor(clazz, MethodType.methodType(void.class, CallSite.class)).asType(MethodType.methodType(DynamicConstant.class, CallSite.class));
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
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
            return (DynamicConstant<T>) DynamicConstantImplConstructor.invokeExact((CallSite) new MutableCallSite(MethodHandles.constant(Object.class, value)));
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
            return (DynamicConstant<T>) DynamicConstantImplConstructor.invokeExact((CallSite) new VolatileCallSite(MethodHandles.constant(Object.class, value)));
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
