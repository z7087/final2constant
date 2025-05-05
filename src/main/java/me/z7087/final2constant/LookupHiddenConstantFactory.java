package me.z7087.final2constant;

import java.lang.invoke.*;
import java.lang.reflect.Array;
import java.util.function.Supplier;

class LookupHiddenConstantFactory extends ConstantFactory {
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

    private static final MethodHandle ConstantImplConstructor;
    private static final MethodHandle LazyConstantImplConstructor;
    private static final MethodHandle DynamicConstantImplConstructor;
    private static final boolean valid;

    static {
        if (MHDefineHiddenClass != null) {
            final String LHCFClassName = LookupHiddenConstantFactory.class.getName().replace('.', '/');
            {
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                try {
                    Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClass.invokeExact(lookup, generateConstantImpl(LHCFClassName + "$ConstantImpl"), true)).lookupClass();
                    ConstantImplConstructor = lookup.findConstructor(clazz, MethodType.methodType(void.class, Object.class)).asType(MethodType.methodType(Constant.class, Object.class));
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                try {
                    Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClass.invokeExact(lookup, generateLazyConstantImpl(LHCFClassName + "$LazyConstantImpl"), true)).lookupClass();
                    LazyConstantImplConstructor = lookup.findConstructor(clazz, MethodType.methodType(void.class, Supplier.class)).asType(MethodType.methodType(Constant.class, Supplier.class));
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
                try {
                    Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClass.invokeExact(lookup, generateDynamicConstantImpl(LHCFClassName + "$DynamicConstantImpl"), true)).lookupClass();
                    DynamicConstantImplConstructor = lookup.findConstructor(clazz, MethodType.methodType(void.class, CallSite.class)).asType(MethodType.methodType(DynamicConstant.class, CallSite.class));
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
            valid = true;
        } else {
            ConstantImplConstructor = LazyConstantImplConstructor = DynamicConstantImplConstructor = MethodHandles.constant(Object.class, null);
            valid = false;
        }
    }

    public LookupHiddenConstantFactory() {
        if (!valid)
            throw new IllegalStateException("DefineHiddenClass MethodHandle getting has failed, LookupHiddenConstantFactory cannot be created!");
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

    @Override
    public <T> MethodHandle ofRecordConstructor(MethodHandles.Lookup hostClass,
                                                Class<T> recordInterfaceClass,
                                                String[] recordArgMethodNames,
                                                String[] recordArgMethodTypes,
                                                boolean generateToStringHashCodeEquals,
                                                boolean generateSetter
    ) {
        assert MHDefineHiddenClass != null;
        final String simpleClassName = recordInterfaceClass.getSimpleName() + "$RecordImpl";
        try {
            final Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClass.invokeExact(
                    hostClass,
                    generateRecordImpl(
                            hostClass.lookupClass().getName().replace('.', '/') + "$$" + simpleClassName,
                            simpleClassName,
                            recordInterfaceClass,
                            recordArgMethodNames,
                            recordArgMethodTypes,
                            generateToStringHashCodeEquals,
                            generateSetter
                    ),
                    true
            )).lookupClass();
            final MethodHandle MHGetConstructorMH = hostClass.findStatic(clazz, "getConstructorMH", MethodType.methodType(MethodHandle.class, int.class));
            return (MethodHandle) MHGetConstructorMH.invokeExact(0);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
