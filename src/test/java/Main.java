import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import me.z7087.final2constant.util.JavaHelper;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Main {
    private static final boolean TEST_FINAL_SETTER_IN_RECORD = true;
    private static void a() {
        String ignored = Main.class.getName();
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