package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.BossBarHudAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.gui.hud.ClientBossBar;

public class WeeklyWarCount {
    private static final long WEEK_MS = 604_800_000L;
    private static final long WAR_COOLDOWN_MS = 25_000L;
    private static long lastWarDetected = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.weeklyWarCountEnabled) return;
            if (client.player == null) return;
            pruneOldWars();
            checkBossBars(client);
        });
        HudRenderCallback.EVENT.register(WeeklyWarCount::render);
    }

    private static void pruneOldWars() {
        long cutoff = System.currentTimeMillis() - WEEK_MS;
        WynnExtrasConfig.INSTANCE.weeklyWars.removeIf(t -> t < cutoff);
    }

    private static void checkBossBars(MinecraftClient client) {
        for (ClientBossBar bar : ((BossBarHudAccessor) client.inGameHud.getBossBarHud()).getBossBars().values()) {
            String name = bar.getName().getString();
            if (name == null) continue;
            String clean = name.replaceAll("§[0-9a-fk-or]", "");
            if (clean.contains("Tower")) {
                long now = System.currentTimeMillis();
                if (now - lastWarDetected > WAR_COOLDOWN_MS) {
                    WynnExtrasConfig.INSTANCE.weeklyWars.add(now);
                    WynnExtrasConfig.save();
                }
                lastWarDetected = now;
                return;
            }
        }
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.weeklyWarCountEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        int count = WynnExtrasConfig.INSTANCE.weeklyWars.size();
        String text = count + " war" + (count == 1 ? "" : "s");

        float scale = 1.5f;
        int x = WynnExtrasConfig.INSTANCE.weeklyWarCountX;
        int y = WynnExtrasConfig.INSTANCE.weeklyWarCountY;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);

        Integer override = WynnExtrasConfig.INSTANCE.hudColorOverrides.get("weeklyWars");
        int color = override != null ? (override | 0xFF000000) : 0xFFFF55FF;
        ctx.drawTextWithShadow(mc.textRenderer, text, 0, 0, color);

        ctx.getMatrices().popMatrix();
    }
}
