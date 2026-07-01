package julianh06.wynnextras.utils;

import julianh06.wynnextras.mixin.Accessor.SlotAccessor;
import net.minecraft.screen.slot.Slot;

import java.lang.reflect.Field;

public final class SlotAccess {
    private static final Field X_FIELD = findField("x", "field_7873", "e");
    private static final Field Y_FIELD = findField("y", "field_7872", "f");

    private SlotAccess() {}

    public static void setPosition(Slot slot, int x, int y) {
        if (slot == null || slot.x == x && slot.y == y) return;

        if (slot instanceof SlotAccessor accessor) {
            accessor.setX(x);
            accessor.setY(y);
            return;
        }

        setInt(X_FIELD, slot, x);
        setInt(Y_FIELD, slot, y);
    }

    private static void setInt(Field field, Slot slot, int value) {
        if (field == null) return;
        try {
            field.setInt(slot, value);
        } catch (IllegalAccessException ignored) {}
    }

    private static Field findField(String... names) {
        for (String name : names) {
            try {
                Field field = Slot.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        return null;
    }
}