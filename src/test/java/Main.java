import me.z7087.final2constant.Constant;
import me.z7087.final2constant.ConstantArray;
import me.z7087.final2constant.DynamicConstant;
import me.z7087.final2constant.primitives.AbstractPrimitiveConstantArray;
import me.z7087.final2constant.primitives.IntConstantArray;
import me.z7087.final2constant.util.JavaHelper;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.BiFunction;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Main {
    private static final boolean TEST_FINAL_SETTER_IN_RECORD = true;
    private static void a() {
        String ignored = Main.class.getName();
    }
    private static void methodHandleRunnerTest() {
        System.out.println("methodHandleRunnerTest() got call");
    }

    private static String methodHandleRunnerTest2(String s1, String s2) {
        System.out.println("methodHandleRunnerTest2() got call, s1 = " + s1 + ", s2 = " + s2);
        return "methodHandleRunnerTest2() result";
    }

    private static void methodHandleRunnerTest3() throws Throwable {
        System.out.println("methodHandleRunnerTest3() got call, throwing Throwable");
        throw new Throwable();
    }

    private static void methodHandleRunnerTest4(String s1, int i2, long l3) {
        System.out.println("methodHandleRunnerTest4() got call, s1 = " + s1 + ", i2 = " + i2 + ", l3 = " + l3);
    }

    private static void methodHandleRunnerTest5(String s1, int i2, long l3) {
        System.out.println("methodHandleRunnerTest5() got call, s1 = " + s1 + ", i2 = " + i2 + ", l3 = " + l3);
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Throwable {
        System.out.println("Start with runtime java version " + JavaHelper.CACHED_JAVA_VERSION + (JavaHelper.CACHED_JAVA_VERSION <= 8 ? " or lower" : ""));
        {
            System.out.println(Constant.factory.of("nice").get());
        }
        {
            DynamicConstant<String> dynamicConstant = Constant.factory.ofMutable("nice1");
            System.out.println(dynamicConstant.get());
            dynamicConstant.set("nice2");
            System.out.println(dynamicConstant.get());
        }
        {
            DynamicConstant<String> dynamicConstant = Constant.factory.ofVolatile("nice3");
            System.out.println(dynamicConstant.get());
            dynamicConstant.set("nice4");
            System.out.println(dynamicConstant.get());
        }
        {
            System.out.println("lazy start");
            Constant<String> dynamicConstant = Constant.factory.ofLazy(() -> "nice5");
            System.out.println(dynamicConstant.get());
            System.out.println("lazy after");
        }
        {
            System.out.println("record interface start");
            final String[] immutableNames, immutableDescriptors, mutableNames, mutableDescriptors;
            try {
                TestRecordInterface triEmptyImpl = Constant.factory.ofEmptyInterfaceImplInstance(
                        MethodHandles.lookup(),
                        TestRecordInterface.class
                );
                final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                        MethodHandles.lookup(),
                        (IntSupplier & Serializable) triEmptyImpl::int32,
                        (LongSupplier & Serializable) triEmptyImpl::int64
                );
                final String[][] mutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                        MethodHandles.lookup(),
                        (Supplier<TestRecordInterface> & Serializable) triEmptyImpl::tri
                );
                immutableNames = immutableNamesAndDescriptors[0];
                immutableDescriptors = immutableNamesAndDescriptors[1];
                mutableNames = mutableNamesAndDescriptors[0];
                mutableDescriptors = mutableNamesAndDescriptors[1];
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            MethodHandle triConstructor = Constant.factory.ofRecordConstructor(
                    MethodHandles.lookup(),
                    TestRecordInterface.class,
                    true,
                    immutableNames,
                    immutableDescriptors,
                    mutableNames,
                    mutableDescriptors,
                    true,
                    true);
            TestRecordInterface tri = (TestRecordInterface) triConstructor.invokeExact(1, 5L);
            System.out.println(tri.int32());
            System.out.println(tri.int64());
            System.out.println(tri.tri());
            if (TEST_FINAL_SETTER_IN_RECORD) {
                System.out.println("record interface final setter start");
                tri.int32(1999);
                tri.int64(2888L);
                tri.tri((TestRecordInterface) triConstructor.invokeExact(1, 5L));
                System.out.println(tri.int32());
                System.out.println(tri.int64());
                System.out.println(tri.tri());
                System.out.println(tri);
                System.out.println("record interface final setter after");
            }
            System.out.println("record interface after");
        }
        {
            System.out.println("record abstract start");
            final String[] immutableNames, immutableDescriptors, mutableNames, mutableDescriptors;
            try {
                TestRecordAbstract traEmptyImpl = Constant.factory.ofEmptyAbstractImplInstance(
                        TestRecordAbstract.getLookup(), // MUST be the class itself's lookup if the constructor is private
                        // if Main and TRA in the same nest, this is not needed, but the class is compiled with target compatibility java8 that has no nest
                        TestRecordAbstract.class
                );
                final String[][] immutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                        MethodHandles.lookup(), // can be ourselves as we can access getter methods
                        (IntSupplier & Serializable) traEmptyImpl::int32,
                        (LongSupplier & Serializable) traEmptyImpl::int64
                );
                final String[][] mutableNamesAndDescriptors = JavaHelper.getNamesAndDescriptors(
                        MethodHandles.lookup(),
                        (Supplier<TestRecordAbstract> & Serializable) traEmptyImpl::tra
                );
                immutableNames = immutableNamesAndDescriptors[0];
                immutableDescriptors = immutableNamesAndDescriptors[1];
                mutableNames = mutableNamesAndDescriptors[0];
                mutableDescriptors = mutableNamesAndDescriptors[1];
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
            MethodHandle traConstructor = Constant.factory.ofRecordConstructor(
                    TestRecordAbstract.getLookup(),
                    TestRecordAbstract.class,
                    false,
                    immutableNames,
                    immutableDescriptors,
                    mutableNames,
                    mutableDescriptors,
                    true,
                    true);
            TestRecordAbstract tra = (TestRecordAbstract) traConstructor.invokeExact(1, 5L);
            System.out.println(tra.int32());
            System.out.println(tra.int64());
            System.out.println(tra.tra());
            if (TEST_FINAL_SETTER_IN_RECORD) {
                System.out.println("record abstract final setter start");
                tra.int32(1999);
                tra.int64(2888L);
                tra.tra((TestRecordAbstract) traConstructor.invokeExact(1, 5L));
                System.out.println(tra.int32());
                System.out.println(tra.int64());
                System.out.println(tra.tra());
                System.out.println(tra);
                System.out.println("record abstract final setter after");
            }
            System.out.println("record abstract after");
        }
        {
            System.out.println("constant base start");
            DynamicConstant<String> base = Constant.factory.ofBase("owo");
            System.out.println(base.orElseThrow());
            base = Constant.factory.ofBase("awa");
            System.out.println(base.orElseThrow());
            base.set("qwq");
            System.out.println(base.orElseThrow());
            System.out.println("constant base after");
        }
        {
            System.out.println("method handle base start");
            Constant.factory.<Runnable>makeBase(
                    MethodHandles.lookup(),
                    Runnable.class.getMethod("run", (Class<?>[]) null),
                    MethodHandles.lookup().findStatic(Main.class, "methodHandleRunnerTest", MethodType.methodType(void.class)),
                    true
            ).run();
            Constant.factory.<BiFunction<Object, Object, Object>>makeBase(
                    MethodHandles.lookup(),
                    BiFunction.class.getMethod("apply", Object.class, Object.class),
                    MethodHandles.lookup().findStatic(Main.class, "methodHandleRunnerTest2", MethodType.methodType(String.class, String.class, String.class))
                            .asType(MethodType.methodType(Object.class, Object.class, Object.class)),
                    true
            ).apply("string 1", "string 5");
            try {
                Constant.factory.<Runnable>makeBase(
                        MethodHandles.lookup(),
                        Runnable.class.getMethod("run", (Class<?>[]) null),
                        MethodHandles.lookup().findStatic(Main.class, "methodHandleRunnerTest3", MethodType.methodType(void.class)),
                        true
                ).run();
            } catch (Throwable t) {
                //noinspection ConstantValue
                if (t.getClass() == Throwable.class) {
                    System.out.println("got Throwable from methodHandleRunnerTest3");
                    t.printStackTrace(System.out);
                } else {
                    throw t;
                }
            }
            MethodHandleBaseTest m = Constant.factory.makeBase(
                    MethodHandles.lookup(),
                    MethodHandleBaseTest.class.getMethod("invoke", Object[].class),
                    MethodHandles.lookup().findStatic(Main.class, "methodHandleRunnerTest4", MethodType.methodType(void.class, String.class, int.class, long.class))
                            .asType(MethodType.methodType(Object.class, String.class, int.class, long.class))
                            .asSpreader(Object[].class, 3),
                    "setTarget",
                    true
            );
            System.out.println("method handle base test 4 result: " +
                    m.invoke("string 1", 25, 311L)
            );
            m.setTarget(
                    MethodHandles.lookup().findStatic(Main.class, "methodHandleRunnerTest5", MethodType.methodType(void.class, String.class, int.class, long.class))
                            .asType(MethodType.methodType(Object.class, String.class, int.class, long.class))
                            .asSpreader(Object[].class, 3)
            );
            System.out.println("method handle base test 5 result: " +
                    m.invoke("string 5", 33, 555L)
            );
            System.out.println("method handle base after");
        }
        {
            System.out.println("array start");
            @SuppressWarnings("unchecked")
            ConstantArray<String> array = (ConstantArray<String>) Constant.factory.ofArrayConstructor(10).invokeExact();
            array.set(0, "hi");
            array.set(9, "ih");
            System.out.println("array[0] = " + array.get(0) + ", array[1] = " + array.get(1) + ", array[9] = " + array.get(9));
            System.out.println("array after");
        }
        {
            System.out.println("primitive array start");
            @SuppressWarnings("unchecked")
            IntConstantArray array = (IntConstantArray) ((AbstractPrimitiveConstantArray<Integer>) Constant.factory.ofPrimitiveArrayConstructor(10, int.class).invokeExact());
            array.setInt(0, 123);
            array.setInt(9, 457);
            System.out.println("array[0] = " + array.getInt(0) + ", array[1] = " + array.getInt(1) + ", array[9] = " + array.getInt(9));
            System.out.println("primitive array after");
        }
        {
            System.out.println("array base start");
            ConstantArray<String> array = Constant.factory.ofArrayBase(10);
            array.set(0, "owo");
            array.set(9, "awa");
            System.out.println("array[0] = " + array.get(0) + ", array[1] = " + array.get(1) + ", array[9] = " + array.get(9));
            System.out.println("array base after");
        }
        {
            System.out.println("primitive array base start");
            IntConstantArray array = (IntConstantArray) (Constant.factory.<Integer>ofPrimitiveArrayBase(10, int.class));
            array.setInt(0, 123);
            array.setInt(9, 457);
            System.out.println("array[0] = " + array.getInt(0) + ", array[1] = " + array.getInt(1) + ", array[9] = " + array.getInt(9));
            System.out.println("primitive array base after");
        }
        {
            System.out.println("eventbus start");
            final int handlerCount = 1000;
            MethodHandle[] handlers = new MethodHandle[handlerCount];
            {
                for (int i = 0; i < handlerCount; ++i) {
                    handlers[i] = EventHandler.onEventMH.bindTo(new EventHandler());
                }
            }
            IEventHandler eventBusImpl = Constant.factory.ofEventBus(
                    MethodHandles.lookup(),
                    IEventHandler.class,
                    IEventHandler.class.getMethods()[0].getName(),
                    null,
                    Event.class,
                    Result.class,
                    handlers,
                    Result.defaultResultSupplierMH,
                    Result.shouldBreakTestMH,
                    null,
                    null,
                    false,
                    325
            );
            EventBusTester.EventBusDC.set(eventBusImpl);
            EventBusTester.EventBusDC.sync();
            for (int i = 0; i < 10; ++i) {
                EventBusTester.test10000();
            }
            System.out.println("eventbus after");
        }
        System.out.println("End");
    }

    static class EventBusTester {
        public static final DynamicConstant<IEventHandler> EventBusDC = Constant.factory.ofMutable(null);

        public static Result test(Event event) {
            return EventBusDC.orElseThrow().onEvent(event);
        }
        public static void test10000(Event event) {
            for (int i = 0; i < 10000; ++i) {
                test(event);
            }
        }
        public static void test1000000(Event event) {
            for (int i = 0; i < 1000000; ++i) {
                test(event);
            }
        }
        public static long test10000() {
            long t = System.nanoTime();
            test10000(Event.INSTANCE);
            return System.nanoTime() - t;
        }
        public static long test1000000() {
            long t = System.nanoTime();
            test1000000(Event.INSTANCE);
            return System.nanoTime() - t;
        }
    }

    public interface TestRecordInterface {
        int int32();

        void int32(int value);

        long int64();

        void int64(long value);

        TestRecordInterface tri();

        void tri(TestRecordInterface value);
    }

    public static abstract class TestRecordAbstract {
        private static MethodHandles.Lookup getLookup() {
            return MethodHandles.lookup();
        }

        // must be 0 args constructor
        private TestRecordAbstract() {} // private constructor allowed but not recommend

        abstract int int32(); // default access is the min access for these methods

        abstract void int32(int value);

        abstract long int64();

        abstract void int64(long value);

        abstract TestRecordAbstract tra();

        abstract void tra(TestRecordAbstract value);
    }

    public interface MethodHandleBaseTest {
        Object invoke(Object... args);

        void setTarget(MethodHandle target);
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static final class Event {
        public static final Event INSTANCE = new Event();
    }
    @SuppressWarnings("InstantiationOfUtilityClass")
    public static final class Result {
        public static final Result SUCCESS = new Result();
        public static final Result FAIL = new Result();
        public static final Result PASS = new Result();

        public static final MethodHandle shouldBreakTestMH;
        public static final MethodHandle defaultResultSupplierMH;
        static {
            try {
                shouldBreakTestMH = MethodHandles.lookup().findStatic(Result.class, "shouldBreak", MethodType.methodType(boolean.class, Result.class));
                defaultResultSupplierMH = MethodHandles.lookup().findStatic(Result.class, "defaultResult", MethodType.methodType(Result.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        public static boolean shouldBreak(Result result) {
            return result != PASS;
        }
        public static Result defaultResult() {
            return PASS;
        }
    }
    public interface IEventHandler {
        Result onEvent(Event event);
    }
    public static final class EventHandler implements IEventHandler {
        public static final MethodHandle onEventMH;
        static {
            try {
                onEventMH = MethodHandles.lookup().findVirtual(EventHandler.class, "onEvent", MethodType.methodType(Result.class, Event.class));
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public Result onEvent(Event event) {
            return Result.PASS;
        }
    }
}