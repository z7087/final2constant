import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;
import me.z7087.final2constant.util.JavaHelper;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public class Main {
    private static final boolean TEST_FINAL_SETTER_IN_RECORD = true;
    private static void a() {
        String ignored = Main.class.getName();
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("Start");
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
        System.out.println("End");
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
}