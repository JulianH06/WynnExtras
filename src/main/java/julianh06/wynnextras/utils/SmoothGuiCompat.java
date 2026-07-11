package julianh06.wynnextras.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class SmoothGuiCompat {
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("smoothgui");
    private static boolean initialized = false;
    private static Field appliedField = null;
    private static Method popMethod = null;
    private static Method pushMethod = null;

    private SmoothGuiCompat() {}

    public static boolean popIfApplied(DrawContext context) {
        if (!LOADED || context == null) return false;
        init();
        try {
            if (appliedField == null || popMethod == null) return false;
            if (!appliedField.getBoolean(null)) return false;
            popMethod.invoke(null, context);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static void pushIfNeeded(DrawContext context, boolean shouldPush) {
        if (!shouldPush || context == null) return;
        init();
        try {
            if (pushMethod != null) pushMethod.invoke(null, context);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> smoothGui = Class.forName("com.ezzenix.smoothgui.SmoothGui");
            appliedField = smoothGui.getField("applied");
            popMethod = smoothGui.getMethod("pop", DrawContext.class);
            pushMethod = smoothGui.getMethod("push", DrawContext.class);
        } catch (ReflectiveOperationException ignored) {
            appliedField = null;
            popMethod = null;
            pushMethod = null;
        }
    }
}