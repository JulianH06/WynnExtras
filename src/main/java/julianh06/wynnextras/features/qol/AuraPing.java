package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import net.minecraft.text.Text;

import java.lang.reflect.Field;

public class AuraPing {
    private static final int AURA_PROC_TIME_MS = 3200;
    private static long firstAura = 0;
    private static long lastAura = 0;
    private static Field subtitleField = null;

    public static void register() {
        HudRenderCallback.EVENT.register(AuraPing::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.auraPingEnabled) return;

        checkSubtitle();
        long now = System.currentTimeMillis();
        if (now - firstAura >= AURA_PROC_TIME_MS) return;

        long remaining = AURA_PROC_TIME_MS - (now - firstAura);
        String remText = String.format("%.1f", remaining / 1000.0);

        MinecraftClient mc = MinecraftClient.getInstance();
        Window window = mc.getWindow();
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale(6.0f, 6.0f);
        int tx = window.getScaledWidth() / 12 - mc.textRenderer.getWidth(remText) / 3;
        int ty = window.getScaledHeight() / 12 - 10;
        ctx.drawTextWithShadow(mc.textRenderer, Text.of(remText), tx, ty, 0xFF00FFFF);
        ctx.getMatrices().popMatrix();

        if (now - firstAura < 400) {
            int color = parseColor(WynnExtrasConfig.INSTANCE.auraPingColor);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int flashColor = (50 << 24) | (r << 16) | (g << 8) | b;
            ctx.fill(0, 0, window.getScaledWidth(), window.getScaledHeight(), flashColor);
        }
    }

    private static int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFF6F00;
        }
    }

    private static void checkSubtitle() {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (subtitleField == null) {
                for (Field f : mc.inGameHud.getClass().getDeclaredFields()) {
                    if (f.getName().equals("subtitle") || f.getName().equals("field_2039")) {
                        f.setAccessible(true);
                        subtitleField = f;
                        break;
                    }
                }
            }
            if (subtitleField == null) return;
            Text subtitle = (Text) subtitleField.get(mc.inGameHud);
            if (subtitle == null) return;
            if (subtitle.getString().contains("Aura")) {
                auraPinged();
            }
        } catch (Exception ignored) {}
    }

    private static void auraPinged() {
        long now = System.currentTimeMillis();
        if (now - lastAura > AURA_PROC_TIME_MS) {
            firstAura = now;
        }
        lastAura = now;
    }

    public static void reset() {
        firstAura = 0;
    }
}
