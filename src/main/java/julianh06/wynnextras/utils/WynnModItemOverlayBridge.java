package julianh06.wynnextras.utils;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

public final class WynnModItemOverlayBridge {
    private static boolean initialized = false;
    private static boolean available = false;
    private static Method renderItemPreMethod;
    private static Method renderItemPostMethod;
    private static Object itemOverlayFeature;

    private WynnModItemOverlayBridge() {}

    public static void renderPre(DrawContext context, ItemStack stack, int x, int y) {
        render(context, stack, x, y, renderItemPreMethod);
    }

    public static void renderPost(DrawContext context, ItemStack stack, int x, int y) {
        render(context, stack, x, y, renderItemPostMethod);
    }

    private static void render(DrawContext context, ItemStack stack, int x, int y, Method method) {
        if (context == null || stack == null || stack.isEmpty()) return;
        if (!ensureInitialized() || method == null || itemOverlayFeature == null) return;

        try {
            method.invoke(itemOverlayFeature, context, stack, x, y, false);
        } catch (ReflectiveOperationException ignored) {
            available = false;
        }
    }

    private static boolean ensureInitialized() {
        if (initialized) return available;
        initialized = true;

        if (!FabricLoader.getInstance().isModLoaded("wynnmod")) {
            return false;
        }

        try {
            Class<?> featureClass = Class.forName("com.wynnmod.feature.Feature");
            Class<?> itemOverlayFeatureClass = Class.forName("com.wynnmod.feature.item.ItemOverlayFeature");

            Method getFeatureInstanceMethod = featureClass.getMethod("getInstance", Class.class);
            renderItemPreMethod = itemOverlayFeatureClass.getDeclaredMethod("onRenderItemPre", DrawContext.class, ItemStack.class, int.class, int.class, boolean.class);
            renderItemPostMethod = itemOverlayFeatureClass.getDeclaredMethod("onRenderItemPost", DrawContext.class, ItemStack.class, int.class, int.class, boolean.class);
            renderItemPreMethod.setAccessible(true);
            renderItemPostMethod.setAccessible(true);
            itemOverlayFeature = getFeatureInstanceMethod.invoke(null, itemOverlayFeatureClass);
            available = true;
        } catch (ReflectiveOperationException | ClassCastException ignored) {
            available = false;
        }

        return available;
    }
}