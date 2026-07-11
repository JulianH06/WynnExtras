package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurseTracker {
    private static final char CURSE_SYMBOL = '\u2749';
    private static final Pattern CURSE_PATTERN = Pattern.compile(CURSE_SYMBOL + "\\s*(\\d+(?:\\.\\d+)?)\\s*s");

    private static long tickCounter = 0;
    private static float labelValue = -1f;
    private static long syncAtMs = 0L;
    private static long lastSeenAtMs = -1L;

    public static volatile Set<Integer> cursedEntityIds = Set.of();

    public record CurseState(String displayText, int color, boolean expired) {}

    public static CurseState getState() {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (!c.curseTrackerEnabled) return null;

        if (labelValue > 0) {
            float predicted = labelValue - (System.currentTimeMillis() - syncAtMs) / 1000f;
            if (predicted < 0f) predicted = 0f;
            return new CurseState("Curse: " + String.format("%.1fs", predicted), timeColor(predicted), false);
        }
        if (lastSeenAtMs > 0 && System.currentTimeMillis() - lastSeenAtMs <= WynnExtrasConfig.INSTANCE.curseTimeout * 1000L) {
            return new CurseState("Curse: expired", 0xFFFF4444, true);
        }
        return null;
    }

    private static LivingEntity findClosestMob(Entity center, ClientWorld world, double maxRange) {
        Box box = new Box(
            center.getX() - maxRange, center.getY() - maxRange, center.getZ() - maxRange,
            center.getX() + maxRange, center.getY() + maxRange, center.getZ() + maxRange
        );
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        Entity self = MinecraftClient.getInstance().player;
        for (LivingEntity le : world.getNonSpectatingEntities(LivingEntity.class, box)) {
            if (le == self) continue;
            double dx = le.getX() - center.getX();
            double dy = le.getY() - center.getY();
            double dz = le.getZ() - center.getZ();
            double dSq = dx * dx + dy * dy + dz * dz;
            if (dSq < bestDistSq) {
                bestDistSq = dSq;
                best = le;
            }
        }
        return best;
    }

    private static int timeColor(float secs) {
        if (secs >= 3.0f) return 0xFF44FF44;
        if (secs >= 2.0f) return 0xFFFFFF00;
        if (secs >= 1.0f) return 0xFFFF8800;
        return 0xFFFF4444;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!WynnExtrasConfig.INSTANCE.curseTrackerEnabled) {
                labelValue = -1f;
                lastSeenAtMs = -1L;
                cursedEntityIds = Set.of();
                return;
            }
            if (client.world == null || client.player == null) {
                labelValue = -1f;
                cursedEntityIds = Set.of();
                return;
            }

            tickCounter++;
            if (tickCounter % 10 != 0) return;

            double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
            Box searchBox = new Box(px - 64, py - 32, pz - 64, px + 64, py + 32, pz + 64);

            float bestSeconds = -1f;
            Set<Integer> newCursed = new HashSet<>();
            for (Entity e : client.world.getNonSpectatingEntities(Entity.class, searchBox)) {
                if (!(e instanceof DisplayEntity.TextDisplayEntity tde)) continue;
                String raw = tde.getText().getString();
                if (raw == null || raw.indexOf(CURSE_SYMBOL) < 0) continue;
                String text = Formatting.strip(raw);
                if (text == null) continue;

                Matcher m = CURSE_PATTERN.matcher(text);
                boolean matched = false;
                while (m.find()) {
                    try {
                        float secs = Float.parseFloat(m.group(1));
                        if (secs > bestSeconds) bestSeconds = secs;
                        matched = true;
                    } catch (NumberFormatException ignored) {}
                }
                if (matched) {
                    LivingEntity closest = findClosestMob(tde, client.world, 3.0);
                    if (closest != null) newCursed.add(closest.getId());
                }
            }
            cursedEntityIds = newCursed;

            long now = System.currentTimeMillis();
            if (bestSeconds > 0) {
                lastSeenAtMs = now;
                // Only resync the anchor when the label value actually changes — otherwise
                // the smooth countdown between scans would jitter on every scan tick.
                if (Math.abs(bestSeconds - labelValue) > 0.05f) {
                    labelValue = bestSeconds;
                    syncAtMs = now;
                }
            } else {
                labelValue = -1f;
            }
        });

        HudRenderCallback.EVENT.register(CurseTracker::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        if (!c.curseTrackerEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;

        CurseState state = getState();
        if (state == null) return;

        float ts = c.curseTrackerScale;
        int baseX = c.curseTrackerX == -1 ? mc.getWindow().getScaledWidth() / 2 : c.curseTrackerX;
        int baseY = c.curseTrackerY;

        String line = state.displayText();
        int color = state.color();

        int tw = mc.textRenderer.getWidth(line);
        int th = mc.textRenderer.fontHeight;
        int previewTw = mc.textRenderer.getWidth("Curse: 3.2s");

        WynnExtrasConfig.Align align = c.curseTrackerAlignment;
        int textOffsetX;
        if (align == WynnExtrasConfig.Align.LEFT) {
            textOffsetX = -previewTw / 2;
        } else if (align == WynnExtrasConfig.Align.RIGHT) {
            textOffsetX = previewTw / 2 - tw;
        } else {
            textOffsetX = -tw / 2;
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(baseX, baseY);
        ctx.getMatrices().scale(ts, ts);
        ctx.drawText(mc.textRenderer, line, textOffsetX, -th / 2, color, true);
        ctx.getMatrices().popMatrix();
    }
}
