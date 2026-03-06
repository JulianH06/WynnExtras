package julianh06.wynnextras.features.misc;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.core.components.Models;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TotemTimer {
    private static int lastSelectedSlot = -1;
    private static int skipEstimatesTicks = 0;
    private static final int SKIP_TICKS_AFTER_SLOT_CHANGE = 10; // ~0.5s

    public record TotemInfo(String owner, String timeText, boolean estimated) {}

    private static float parseSeconds(String timeText) {
        String digits = timeText.replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) return -1;
        try {
            return Float.parseFloat(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int timeColor(String timeText) {
        float secs = parseSeconds(timeText);
        if (secs < 0) return 0xFFFFFFFF;
        if (secs >= 4.0f) return 0xFF44FF44;
        if (secs >= 3.0f) return 0xFFFFFF00;
        if (secs >= 2.0f) return 0xFFFF8800;
        return 0xFFFF4444;
    }

    private static final List<TotemInfo> totems = new ArrayList<>();
    private static boolean warningActive = false;

    // Out-of-render estimation: owner -> {lastKnownSeconds, lastUpdateTick}
    private static final Map<String, float[]> estimatedTotems = new HashMap<>();
    private static long tickCounter = 0;

    public static List<TotemInfo> getTotems() {
        return totems;
    }

    public static boolean isWarningActive() {
        return warningActive;
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.player == null) return;


            if(client.player.getInventory() != null) {
                int currentSlot = client.player.getInventory().getSelectedSlot();
                if (lastSelectedSlot != -1 && currentSlot != lastSelectedSlot) {
                    estimatedTotems.clear();
                    skipEstimatesTicks = SKIP_TICKS_AFTER_SLOT_CHANGE;
                } else if (skipEstimatesTicks > 0) {
                    estimatedTotems.clear();
                    skipEstimatesTicks--;
                }
                lastSelectedSlot = currentSlot;

            }

            boolean skipEstimatesThisTick = skipEstimatesTicks > 0;

            totems.clear();
            warningActive = false;
            if (!WynnExtrasConfig.INSTANCE.totemTimerEnabled) return;
            if (client.world == null || client.player == null) return;
            if (Models.Character.getClassType() != ClassType.SHAMAN) return;

            tickCounter++;
            WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
            float threshold = c.totemTimerWarningThreshold;
            String playerName = McUtils.playerName();

            double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
            Box searchBox = new Box(px - 64, py - 32, pz - 64, px + 64, py + 32, pz + 64);

            List<DisplayEntity.TextDisplayEntity> allTdes = new ArrayList<>();
            for (Entity e : client.world.getNonSpectatingEntities(Entity.class, searchBox)) {
                if (e instanceof DisplayEntity.TextDisplayEntity tde) allTdes.add(tde);
            }

            // Count per-owner to distinguish multiple totems (owner#0, owner#1, etc.)
            Map<String, Integer> ownerCounts = new HashMap<>();
            List<String> foundKeys = new ArrayList<>();

            for (DisplayEntity.TextDisplayEntity tde : allTdes) {
                String raw = tde.getText().getString();
                String text = Formatting.strip(raw);
                if (text == null || !text.contains("'s Totem")) continue;

                int idx = text.indexOf("'s Totem");
                String owner = idx > 0 ? text.substring(0, idx).trim() : "?";

                // Own-only filter
                if (c.totemTimerOwnOnly && playerName != null && !owner.equals(playerName)) continue;

                String timeText = "";
                String[] lines = text.split("\n");
                for (String line : lines) {
                    String l = line.trim();
                    if (!l.isEmpty() && !l.contains("'s Totem")) {
                        timeText = l;
                        break;
                    }
                }
                if (timeText.isEmpty()) {
                    double tx = tde.getX(), ty = tde.getY(), tz = tde.getZ();
                    double bestDist2 = Double.MAX_VALUE;
                    for (DisplayEntity.TextDisplayEntity other : allTdes) {
                        if (other == tde) continue;
                        String otherRaw = Formatting.strip(other.getText().getString());
                        if (otherRaw == null || otherRaw.isBlank()) continue;
                        double dx = other.getX() - tx, dy = other.getY() - ty, dz = other.getZ() - tz;
                        double dist2 = dx * dx + dy * dy + dz * dz;
                        if (dist2 <= 4.0 && dist2 < bestDist2) {
                            bestDist2 = dist2;
                            timeText = otherRaw.trim();
                        }
                    }
                }

                int num = ownerCounts.getOrDefault(owner, 0);
                ownerCounts.put(owner, num + 1);
                String key = owner + "#" + num;
                foundKeys.add(key);

                float secs = parseSeconds(timeText);
                if (secs > 0 && !skipEstimatesThisTick) {
                    estimatedTotems.put(key, new float[]{ secs, tickCounter });
                }

                totems.add(new TotemInfo(owner, timeText, false));
            }

            // Out-of-render estimation
            if (c.totemTimerEstimate && !skipEstimatesThisTick) {
                List<String> toRemove = new ArrayList<>();
                for (Map.Entry<String, float[]> entry : estimatedTotems.entrySet()) {
                    String key = entry.getKey();
                    if (foundKeys.contains(key)) continue;
                    String owner = key.contains("#") ? key.substring(0, key.lastIndexOf('#')) : key;
                    if (c.totemTimerOwnOnly && playerName != null && !owner.equals(playerName)) continue;

                    float[] data = entry.getValue();
                    float lastSecs = data[0];
                    long lastTick = (long) data[1];
                    long ticksElapsed = tickCounter - lastTick;
                    float estimatedSecs = lastSecs - (ticksElapsed / 20.0f);

                    if (estimatedSecs <= 0) {
                        toRemove.add(key);
                        continue;
                    }

                    String estTimeText = String.format("~%.0fs", estimatedSecs);
                    totems.add(new TotemInfo(owner, estTimeText, true));
                }
                toRemove.forEach(estimatedTotems::remove);
            }

            // Check warning condition
            if (c.totemTimerWarningText || c.totemTimerWarningSound) {
                for (TotemInfo t : totems) {
                    float secs = parseSeconds(t.timeText());
                    if (secs > 0 && secs <= threshold) {
                        warningActive = true;
                        break;
                    }
                }
            }

            // Play warning sound
            if (warningActive && c.totemTimerWarningSound) {
                McUtils.playSoundAmbient(
                        SoundEvent.of(Identifier.of("block.note_block.pling")),
                        1.0f, 2.0f
                );
            }
        });

        HudRenderCallback.EVENT.register(TotemTimer::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.totemTimerEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;

        if (!totems.isEmpty()) {
            float ts = c.totemTimerScale;
            int lineH = (int) (10 * ts);
            int baseX = c.totemTimerX;
            int baseY = c.totemTimerY;
            int i = 0;
            for (TotemInfo t : totems) {
                String timeDisplay = t.timeText().replaceAll("[^0-9s.~]", "").trim();
                if (timeDisplay.isEmpty()) timeDisplay = "?";
                String line = t.owner() + "'s Totem: " + timeDisplay;
                int color = t.estimated() ? 0xFFAAAAAA : timeColor(t.timeText());

                int tw = mc.textRenderer.getWidth(line);
                int th = mc.textRenderer.fontHeight;
                int boxW = (int) ((tw + 6) * ts);
                int boxH = (int) (14 * ts);

                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(baseX + boxW / 2f, baseY + i * lineH + boxH / 2f);
                ctx.getMatrices().scale(ts, ts);
                ctx.drawText(mc.textRenderer, line, -tw / 2, -th / 2, color, true);
                ctx.getMatrices().popMatrix();
                i++;
            }
        }

        if (warningActive && c.totemTimerWarningText) {
            String alarmText = "RECAST TOTEM!";
            float as = c.totemWarningScale;
            int wx = c.totemWarningX;
            int wy = c.totemWarningY;

            int tw = mc.textRenderer.getWidth(alarmText);
            int th = mc.textRenderer.fontHeight;
            int boxW = (int) ((tw + 6) * as);
            int boxH = (int) (14 * as);

            if (wx == -1) {
                wx = (mc.getWindow().getScaledWidth() - boxW) / 2;
            }

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(wx + boxW / 2f, wy + boxH / 2f);
            ctx.getMatrices().scale(as, as);
            ctx.drawText(mc.textRenderer, alarmText, -tw / 2, -th / 2, 0xFFFF4444, true);
            ctx.getMatrices().popMatrix();
        }
    }
}
