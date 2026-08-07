package julianh06.wynnextras.compat.wynntils;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class WynntilsCompat {
    public static final String MOD_ID = "wynntils";
    public static final String SUPPORTED_VERSION = "4.2.1";

    private WynntilsCompat() {}

    public static boolean isLoaded() {
        try {
            return FabricLoader.getInstance().isModLoaded(MOD_ID);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String version() {
        try {
            return FabricLoader.getInstance().getModContainer(MOD_ID)
                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                    .orElse("not installed");
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    public static Class<?> requireClass(String name) throws ClassNotFoundException {
        return Class.forName(name, false, WynntilsCompat.class.getClassLoader());
    }

    public static Method requireMethod(Class<?> owner, String name, Class<?> returnType, Class<?>... parameters)
            throws NoSuchMethodException {
        Method method = owner.getDeclaredMethod(name, parameters);
        if (!returnType.isAssignableFrom(method.getReturnType())) {
            throw new NoSuchMethodException(owner.getName() + "." + name + " has return type "
                    + method.getReturnType().getName() + ", expected " + returnType.getName());
        }
        if (!method.trySetAccessible() && !Modifier.isPublic(method.getModifiers())) {
            throw new NoSuchMethodException(owner.getName() + "." + name + " is not accessible");
        }
        return method;
    }

    public static Field requireField(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        if (!type.isAssignableFrom(field.getType())) {
            throw new NoSuchFieldException(owner.getName() + "." + name + " has type "
                    + field.getType().getName() + ", expected " + type.getName());
        }
        if (!field.trySetAccessible() && !Modifier.isPublic(field.getModifiers())) {
            throw new NoSuchFieldException(owner.getName() + "." + name + " is not accessible");
        }
        return field;
    }

    public static void requireParameters(Method method, Class<?>... parameters) throws NoSuchMethodException {
        if (!Arrays.equals(method.getParameterTypes(), parameters)) {
            throw new NoSuchMethodException(method + " has unexpected parameters");
        }
    }
}
