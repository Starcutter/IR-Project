import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) throws
            InvocationTargetException, IllegalAccessException,
            NoSuchMethodException {
        String className = args[0];
        try {
            Method mainMethod = Class.forName(className)
                    .getMethod("main", String[].class);
            mainMethod.invoke(null,
                    new Object[]{Arrays.copyOfRange(args, 1, args.length)});
        } catch (ClassNotFoundException e) {
            System.out.println("Wrong class name.");
            System.exit(-1);
        }
    }
}
