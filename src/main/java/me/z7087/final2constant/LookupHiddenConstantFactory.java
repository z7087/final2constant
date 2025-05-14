package me.z7087.final2constant;

import java.lang.invoke.*;
import java.lang.reflect.Array;
import java.util.function.Supplier;

class LookupHiddenConstantFactory extends ConstantFactory {
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private static final MethodHandle MHDefineHiddenClass;
    private static final MethodHandle MHDefineHiddenClassNest;
    static {
        final Object ClassOptionEmptyArray;
        final Object ClassOptionNestArray;
        {
            Class<?> ClassOptionClass;
            try {
                ClassOptionClass = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassOption");
            } catch (ClassNotFoundException ignored) {
                ClassOptionClass = null;
            }
            if (ClassOptionClass != null) {
                ClassOptionEmptyArray = Array.newInstance(ClassOptionClass, 0);
                ClassOptionNestArray = Array.newInstance(ClassOptionClass, 1);
                Array.set(ClassOptionNestArray, 0, ClassOptionClass.getEnumConstants()[0]);
            } else {
                ClassOptionEmptyArray = null;
                ClassOptionNestArray = null;
            }
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
        if (ClassOptionNestArray != null) {
            MethodHandle _MHDefineHiddenClassNest;
            try {
                //noinspection JavaLangInvokeHandleSignature
                _MHDefineHiddenClassNest = MethodHandles.lookup().findVirtual(MethodHandles.Lookup.class, "defineHiddenClass", MethodType.methodType(MethodHandles.Lookup.class, byte[].class, boolean.class, ClassOptionNestArray.getClass()));
                _MHDefineHiddenClassNest = MethodHandles.insertArguments(_MHDefineHiddenClassNest, 3, ClassOptionNestArray);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                _MHDefineHiddenClassNest = null;
            }
            MHDefineHiddenClassNest = _MHDefineHiddenClassNest;
        } else {
            MHDefineHiddenClassNest = null;
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
                                                Class<T> recordAbstractOrInterfaceClass,
                                                boolean useInterface,
                                                String[] recordImmutableArgMethodNames,
                                                String[] recordImmutableArgMethodTypes,
                                                String[] recordMutableArgMethodNames,
                                                String[] recordMutableArgMethodTypes,
                                                boolean generateToStringHashCodeEquals,
                                                boolean generateSetterForFinalFields
    ) {
        assert MHDefineHiddenClassNest != null;
        if (recordImmutableArgMethodNames == null) recordImmutableArgMethodNames = EMPTY_STRING_ARRAY;
        if (recordImmutableArgMethodTypes == null) recordImmutableArgMethodTypes = EMPTY_STRING_ARRAY;
        if (recordMutableArgMethodNames == null) recordMutableArgMethodNames = EMPTY_STRING_ARRAY;
        if (recordMutableArgMethodTypes == null) recordMutableArgMethodTypes = EMPTY_STRING_ARRAY;
        final String simpleClassName = recordAbstractOrInterfaceClass.getSimpleName() + "$RecordImpl";
        try {
            final Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClassNest.invokeExact(
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

    @Override
    public <T> T ofEmptyInterfaceImplInstance(
            MethodHandles.Lookup hostClass,
            Class<T> interfaceClass
    ) {
        assert MHDefineHiddenClassNest != null;
        try {
            final Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClassNest.invokeExact(
                    hostClass,
                    generateEmptyImpl(
                            hostClass.lookupClass().getName().replace('.', '/') + "$$" + interfaceClass.getSimpleName() + "$EmptyImpl",
                            true,
                            interfaceClass
                    ),
                    true
            )).lookupClass();
            final MethodHandle ConstructorMH = hostClass.findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(Object.class));
            return interfaceClass.cast(ConstructorMH.invokeExact());
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T ofEmptyAbstractImplInstance(
            MethodHandles.Lookup hostClass,
            Class<T> abstractClass
    ) {
        assert MHDefineHiddenClassNest != null;
        try {
            final Class<?> clazz = ((MethodHandles.Lookup) MHDefineHiddenClassNest.invokeExact(
                    hostClass,
                    generateEmptyImpl(
                            hostClass.lookupClass().getName().replace('.', '/') + "$$" + abstractClass.getSimpleName() + "$EmptyImpl",
                            false,
                            abstractClass
                    ),
                    true
            )).lookupClass();
            final MethodHandle ConstructorMH = hostClass.findConstructor(clazz, MethodType.methodType(void.class)).asType(MethodType.methodType(Object.class));
            return abstractClass.cast(ConstructorMH.invokeExact());
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
