package julianh06.wynnextras.utils;

public final class LunarCompat {
    private static Boolean lunarClient;
    private static Object lastHandledScreenMixinScreen = null;
    private static long lastHandledScreenMixinRenderMs = 0L;

    private LunarCompat() {}

    public static boolean isLunarClient() {
        if (lunarClient != null) return lunarClient;
        lunarClient = hasClass("com.moonsworth.lunar.genesis.Genesis")
                || hasClass("com.moonsworth.lunar.LunarClient");
        return lunarClient;
    }

    public static void recordHandledScreenMixinRender(Object screen) {
        lastHandledScreenMixinScreen = screen;
        lastHandledScreenMixinRenderMs = System.currentTimeMillis();
    }

    public static boolean wasHandledScreenMixinRenderedRecently(Object screen) {
        return lastHandledScreenMixinScreen == screen && System.currentTimeMillis() - lastHandledScreenMixinRenderMs < 30L;
    }

    private static boolean hasClass(String name) {
        try {
            Class.forName(name, false, LunarCompat.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}