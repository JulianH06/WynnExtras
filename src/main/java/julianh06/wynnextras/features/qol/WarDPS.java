package julianh06.wynnextras.features.qol;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.mixin.Accessor.BossBarHudAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class WarDPS {
    private static long lastTimeInWar = 0;
    private static long warStartTime = -1;
    private static long firstDamageTime = -1;
    private static long previousTime = 0;
    private static double previousEhp = 0;
    private static double dps = 0;
    private static double dpsFiveSec = 0;
    private static double maxEhp = 0;
    private static double dpsSinceStart = 0;
    private static double timeRemaining = 0;
    private static final List<Double> previousFiveEhp = new ArrayList<>();

    private static double ehpDisplay = 0;
    private static double lowerDpsDisplay = 0;
    private static double higherDpsDisplay = 0;
    private static long timeDisplay = 0;
    private static String previousTerritoryName = "";

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.warDpsEnabled) return;
            if (client.player == null) return;
            for (ClientBossBar bar : ((BossBarHudAccessor) client.inGameHud.getBossBarHud()).getBossBars().values()) {
                String text = bar.getName().getString();
                if (text != null && text.contains("Tower")) {
                    processBossBar(text);
                    break;
                }
            }
        });
        HudRenderCallback.EVENT.register(WarDPS::render);
    }

    private static void processBossBar(String text) {
        try {
            String[] words = text.split(" ");
            if (words.length < 6) return;

            if (System.currentTimeMillis() - lastTimeInWar > 119_000) previousTerritoryName = "";
            lastTimeInWar = System.currentTimeMillis();

            int startIndex1 = Arrays.asList(words).indexOf("-");
            int startIndex2 = Arrays.asList(words).lastIndexOf("-");
            if (startIndex1 < 2 || startIndex2 < 0) return;

            StringBuilder territoryName = new StringBuilder();
            for (int i = 1; i < startIndex1 - 1; i++) {
                territoryName.append(stripFormat(words[i])).append(" ");
            }

            if (!territoryName.toString().equals(previousTerritoryName)) {
                resetWar();
                previousTerritoryName = territoryName.toString();
                warStartTime = System.currentTimeMillis();
            }

            String health = Objects.requireNonNull(stripFormat(words[startIndex1 + 2]));
            String defense = Objects.requireNonNull(stripFormat(words[startIndex1 + 3]))
                    .replace("(", "").split("\\)")[0].replace("%", "");
            String damage = Objects.requireNonNull(stripFormat(words[startIndex2 + 2]));
            String attacks = Objects.requireNonNull(stripFormat(words[startIndex2 + 3]))
                    .replace("(", "").split("\\)")[0].replace("x", "");

            ehpDisplay = Math.round(Double.parseDouble(health) / (1.0 - (Double.parseDouble(defense) / 100.0)));
            lowerDpsDisplay = Double.parseDouble(damage.split("-")[0]) * Double.parseDouble(attacks);
            higherDpsDisplay = Double.parseDouble(damage.split("-")[1]) * Double.parseDouble(attacks);

            if (maxEhp == 0) {
                maxEhp = ehpDisplay;
                previousEhp = ehpDisplay;
                previousFiveEhp.add(ehpDisplay);
            }

            timeDisplay = (System.currentTimeMillis() - warStartTime) / 1000;
            if (timeDisplay != previousTime) {
                dps = previousEhp - ehpDisplay;
                previousEhp = ehpDisplay;
                if (firstDamageTime == -1 && dps > 0) firstDamageTime = System.currentTimeMillis();
                if (previousFiveEhp.size() == 5) previousFiveEhp.remove(0);
                previousFiveEhp.add(ehpDisplay);
                if (!previousFiveEhp.isEmpty()) dpsFiveSec = Math.floor((previousFiveEhp.get(0) - ehpDisplay) / 5);
                if (firstDamageTime != -1 && System.currentTimeMillis() - firstDamageTime > 0) {
                    dpsSinceStart = (maxEhp - previousEhp) / ((System.currentTimeMillis() - firstDamageTime) / 1000.0);
                    timeRemaining = Math.floor(previousEhp / dpsSinceStart);
                }
            }
            previousTime = timeDisplay;
        } catch (Exception ignored) {}
    }

    private static String stripFormat(String s) {
        return s == null ? null : s.replaceAll("§[0-9a-fk-or]", "");
    }

    private static void resetWar() {
        warStartTime = -1;
        firstDamageTime = -1;
        previousTime = 0;
        previousEhp = 0;
        previousFiveEhp.clear();
        dps = 0;
        dpsFiveSec = 0;
        dpsSinceStart = 0;
        maxEhp = 0;
        timeRemaining = 0;
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.warDpsEnabled) return;
        if (System.currentTimeMillis() - lastTimeInWar > 2000) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        String[] stats = {
                "§bWar Info",
                String.format("%ds", timeDisplay),
                String.format("Tower EHP: §b%s", readable(ehpDisplay)),
                String.format("Tower DPS: §b%s-%s", readable(lowerDpsDisplay), readable(higherDpsDisplay)),
                String.format("Team DPS/1s: §c%s", readable(dps)),
                String.format("Team DPS/5s: §c%s", readable(dpsFiveSec)),
                String.format("Team DPS total: §e%s", readable(dpsSinceStart)),
                dpsSinceStart == 0 ? "ETA: §7Unknown" : String.format("ETA: §a%ds", (int) timeRemaining)
        };

        int x = WynnExtrasConfig.INSTANCE.warDpsX;
        int y = WynnExtrasConfig.INSTANCE.warDpsY;
        int maxW = 0;
        for (String s : stats) maxW = Math.max(maxW, mc.textRenderer.getWidth(s));
        ctx.fill(x - 2, y - 2, x + maxW + 4, y + stats.length * 10 + 2, 0x66000000);
        for (int i = 0; i < stats.length; i++) {
            ctx.drawTextWithShadow(mc.textRenderer, stats[i], x, y + i * 10, 0xFFFFFFFF);
        }
    }

    private static String readable(double v) {
        if (v >= 1_000_000) return String.format("%.2fM", v / 1_000_000);
        if (v >= 1_000) return String.format("%.1fK", v / 1_000);
        return String.format("%.0f", v);
    }
}
