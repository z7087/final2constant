import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class GetInline {
    private static final Constant<Integer> CONSTANT = Constant.factory.of(1);
    private static final DynamicConstant<GetInline> MUTABLE = Constant.factory.ofMutable(null);
    private static final DynamicConstant<Integer> VOLATILE = Constant.factory.ofVolatile(3);
    private static final Constant<Integer> LAZY = Constant.factory.ofLazy(() -> 4);
    private static final Constant<Integer> CONSTANT2 = Constant.factory.of(5);
    private static final DynamicConstant<GetInline> MUTABLE2 = Constant.factory.ofMutable(null);
    private static final DynamicConstant<Integer> VOLATILE2 = Constant.factory.ofVolatile(7);
    private static final Constant<Integer> LAZY2 = Constant.factory.ofLazy(() -> 8);
    interface FinalModifyTestI {
        int field();
        void field(int value);
    }
    private static final MethodHandle FMTI_CONSTRUCTOR = Constant.factory.ofRecordConstructor(
            MethodHandles.lookup(),
            FinalModifyTestI.class,
            true,
            new String[] {
                    "field"
            },
            new String[] {
                    "I"
            },
            null,
            null,
            true,
            false);
    private static final FinalModifyTestI FMTI;

    static {
        try {
            FMTI = (FinalModifyTestI) FMTI_CONSTRUCTOR.invokeExact(1);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public GetInline getter() {
        //return CONSTANT.get();
        return MUTABLE.get();
        //return VOLATILE.get();
        //return LAZY.get();
    }
    public GetInline getter2() {
        //return CONSTANT.get();
        return MUTABLE2.get();
        //return VOLATILE.get();
        //return LAZY.get();
    }
    public int getterFinalModify() {
        return FMTI.field();
    }
    public void modifyFinal(int value) {
        FMTI.field(value);
    }

//    public static void main(String[] args) throws InterruptedException {
//        System.out.println("start");
//        Thread.sleep(1000);
//        //MUTABLE.set(10086);
//        //MUTABLE.sync();
//        GetInline instance = new GetInline();
//        MUTABLE.set(instance);
//        MUTABLE.sync();
//        for (int i = 0; i < 50000; ++i) {
//            instance.getter();
//        }
//        //MUTABLE.set(10085);
//        //MUTABLE.sync();
//        for (int i = 0; i < 50000; ++i) {
//            instance.getter2();
//        }
//        for (int i = 0; i < 50000; ++i) {
//            instance.getter();
//        }
//        for (int i = 0; i < 50000; ++i) {
//            instance.getter2();
//        }
//        System.out.println("end");
//        Thread.sleep(5000);
//    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("start");
        Thread.sleep(1000);
        GetInline instance = new GetInline();
        for (int i = 0; i < 1000000; ++i) {
            instance.getterFinalModify();
        }
        Thread.sleep(1000);
        instance.modifyFinal(5);
        for (int i = 0; i < 1000000; ++i) {
            instance.getterFinalModify();
        }
        Thread.sleep(1000);
        System.out.println("end " + instance.getterFinalModify());
        Thread.sleep(5000);
    }
}
