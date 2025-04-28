import me.z7087.final2constant.Constant;
import me.z7087.final2constant.DynamicConstant;

public class Main {
    private static void a() {
        String ignored = Main.class.getName();
    }

    public static void main(String[] args) {
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
            Constant<String> dynamicConstant = Constant.factory.ofLazy(() -> "nice5");
            System.out.println("lazy start");
            System.out.println(dynamicConstant.get());
            System.out.println("lazy after");
        }
        System.out.println("End");
    }
}