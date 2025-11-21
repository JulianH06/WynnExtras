package julianh06.wynnextras.config.simpleconfig;

import java.lang.reflect.*;

@SuppressWarnings("unchecked")
public class Utils {
    public static <V> V getUnsafely(Field field, Object obj) {
        if (obj == null) {
            return null;
        } else {
            try {
                field.setAccessible(true);
                return (V) field.get(obj);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <V> V getUnsafely(Field field, Object obj, V defaultValue) {
        V ret = getUnsafely(field, obj);
        if (ret == null) {
            ret = defaultValue;
        }

        return ret;
    }

    public static void setUnsafely(Field field, Object obj, Object newValue) {
        if (obj != null) {
            try {
                field.setAccessible(true);
                field.set(obj, newValue);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static <V> V constructUnsafely(Class<V> cls) {
        try {
            Constructor<V> constructor = cls.getDeclaredConstructor();
            constructor.setAccessible(true);
            return (V)constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getClassNameOrNull(Class<?> fieldTypeParam, Object elem) {
        try {
            Method getNameMethod = fieldTypeParam.getMethod("getName");
            if (Modifier.isPublic(getNameMethod.getModifiers())
                    && !Modifier.isStatic(getNameMethod.getModifiers())
                    && getNameMethod.getReturnType() == String.class) {
                return (String) getNameMethod.invoke(elem);
            }
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException ignored) {
        }

        return null;
    }
}
