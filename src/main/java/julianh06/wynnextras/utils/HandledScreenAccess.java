package julianh06.wynnextras.utils;

import julianh06.wynnextras.mixin.Accessor.HandledScreenAccessor;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;

import java.lang.reflect.Field;

public final class HandledScreenAccess {
    private HandledScreenAccess() {}

    public static int x(HandledScreen<?> screen) {
        if (screen instanceof HandledScreenAccessor accessor) return accessor.getX();
        return getInt(screen, 0, "x", "field_2776", "z");
    }

    public static int y(HandledScreen<?> screen) {
        if (screen instanceof HandledScreenAccessor accessor) return accessor.getY();
        return getInt(screen, 0, "y", "field_2800", "A");
    }

    public static int backgroundWidth(HandledScreen<?> screen) {
        if (screen instanceof HandledScreenAccessor accessor) return accessor.getBackgroundWidth();
        return getInt(screen, 176, "backgroundWidth", "field_2792", "d");
    }

    public static int backgroundHeight(HandledScreen<?> screen) {
        if (screen instanceof HandledScreenAccessor accessor) return accessor.getBackgroundHeight();
        return getInt(screen, 166, "backgroundHeight", "field_2779", "e");
    }

    public static Slot focusedSlot(HandledScreen<?> screen) {
        if (screen instanceof HandledScreenAccessor accessor) return accessor.getFocusedSlot();
        Object value = get(screen, Slot.class, "focusedSlot", "field_2787", "y");
        return value instanceof Slot slot ? slot : null;
    }

    public static void setFocusedSlot(HandledScreen<?> screen, Slot slot) {
        if (screen instanceof HandledScreenAccessor accessor) {
            accessor.setFocusedSlot(slot);
            return;
        }
        set(screen, Slot.class, slot, "focusedSlot", "field_2787", "y");
    }

    private static int getInt(HandledScreen<?> screen, int fallback, String... names) {
        Object value = get(screen, int.class, names);
        return value instanceof Integer i ? i : fallback;
    }

    private static Object get(HandledScreen<?> screen, Class<?> type, String... names) {
        Field field = findField(screen.getClass(), type, names);
        if (field == null) return null;
        try {
            field.setAccessible(true);
            return field.get(screen);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static void set(HandledScreen<?> screen, Class<?> type, Object value, String... names) {
        Field field = findField(screen.getClass(), type, names);
        if (field == null) return;
        try {
            field.setAccessible(true);
            field.set(screen, value);
        } catch (IllegalAccessException ignored) {}
    }

    private static Field findField(Class<?> start, Class<?> type, String... names) {
        for (Class<?> cls = start; cls != null; cls = cls.getSuperclass()) {
            for (String name : names) {
                try {
                    Field field = cls.getDeclaredField(name);
                    if (field.getType() == type) return field;
                } catch (NoSuchFieldException ignored) {}
            }
        }
        return null;
    }
}