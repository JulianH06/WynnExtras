package julianh06.wynnextras.features.misc;

import com.wynntils.models.gear.type.GearType;
import com.wynntils.models.items.WynnItem;
import com.wynntils.models.items.items.game.GearItem;
import com.wynntils.core.components.Models;
import com.wynntils.utils.mc.McUtils;
import net.minecraft.item.ItemStack;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.*;

public class TotemTimer {
    private static int lastSelectedSlot = -1;

    public record TotemInfo(String owner, String timeText, boolean estimated) {}

    private static boolean isRelik(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Optional<WynnItem> opt = Models.Item.getWynnItem(stack);
        return opt.isPresent() && opt.get() instanceof GearItem gear && gear.getGearType() == GearType.RELIK;
    }

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
    private static final List<String> lastFoundKeys = new ArrayList<>();
    private static long tickCounter = 0;

    public static List<TotemInfo> getTotems() {
        return totems;
    }

    public static boolean isWarningActive() {
        return warningActive;
    }

    public static void register() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            client.getSoundManager().registerListener(new SoundInstanceListener() {
                @Override
                public void onSoundPlayed(SoundInstance sound, WeightedSoundSet soundSet, float range) {
                    onSound(sound.getId().getPath());
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if(client.player == null) return;

            if (client.player.getInventory() != null) {
                int currentSlot = client.player.getInventory().getSelectedSlot();
                if (lastSelectedSlot != -1 && currentSlot != lastSelectedSlot) {
                    ItemStack prevStack = client.player.getInventory().getStack(lastSelectedSlot);
                    ItemStack newStack = client.player.getInventory().getStack(currentSlot);
                    if (isRelik(prevStack) && isRelik(newStack)) {
                        estimatedTotems.clear();
                    }
                }
                lastSelectedSlot = currentSlot;
            }

            warningActive = false;
            if (!WynnExtrasConfig.INSTANCE.totemTimerEnabled) { totems.clear(); return; }
            if (client.world == null || client.player == null) { totems.clear(); return; }

            tickCounter++;
            WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
            String playerName = McUtils.playerName();

            // Warning check + sound runs every tick
            if (c.totemTimerWarningText || c.totemTimerWarningSound) {
                float threshold = c.totemTimerWarningThreshold;
                for (TotemInfo t : totems) {
                    float secs = parseSeconds(t.timeText());
                    if (secs > 0 && secs <= threshold) {
                        warningActive = true;
                        break;
                    }
                }
            }
            if (warningActive && c.totemTimerWarningSound) {
                McUtils.playSoundAmbient(
                    SoundEvent.of(Identifier.of("block.note_block.pling")),
                    c.totemTimerWarningSoundVolume / 100, 2.0f
                );
            }

            if (tickCounter % 10 != 0) return;

            totems.clear();
            double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
            Box searchBox = new Box(px - 64, py - 32, pz - 64, px + 64, py + 32, pz + 64);

            List<DisplayEntity.TextDisplayEntity> allTdes = new ArrayList<>();
            for (Entity e : client.world.getNonSpectatingEntities(Entity.class, searchBox)) {
                if (e instanceof DisplayEntity.TextDisplayEntity tde) allTdes.add(tde);
            }

            Map<String, Integer> ownerCounts = new HashMap<>();
            lastFoundKeys.clear();

            for (DisplayEntity.TextDisplayEntity tde : allTdes) {
                String raw = tde.getText().getString();
                String text = Formatting.strip(raw);
                if (text == null || (!text.contains("'s Totem") && !text.contains("' Totem"))) continue;
                if (text.contains("Totem of Tales")) continue;

                int idx = text.contains("'s Totem") ? text.indexOf("'s Totem") : text.indexOf("' Totem");
                // Take only the text on the same line as "'s Totem"/"' Totem", Wynntils may prepend
                // a banner on a separate line before the player name.
                int lineStart = text.lastIndexOf('\n', idx > 0 ? idx - 1 : 0);
                String owner = text.substring(lineStart + 1, idx).trim();
                if (owner.isEmpty()) owner = "?";

                if (c.totemTimerOwnOnly && playerName != null && !owner.equals(playerName)) continue;

                String timeText = "";
                String[] lines = text.split("\n");
                // Scan lines starting after the header line so a prepended banner is skipped.
                boolean pastHeader = false;
                for (String line : lines) {
                    String l = line.trim();
                    if (l.contains("'s Totem") || l.contains("' Totem")) {
                        pastHeader = true;
                        continue;
                    }
                    if (pastHeader && !l.isEmpty()) {
                        String[] tokens = l.split("\\s+");
                        timeText = tokens[tokens.length - 1];
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
                lastFoundKeys.add(key);

                float secs = parseSeconds(timeText);
                if (secs > 0) {
                    estimatedTotems.put(key, new float[]{ secs, tickCounter });
                }

                totems.add(new TotemInfo(owner, timeText, false));
            }

            if (c.totemTimerEstimate) {
                List<String> toRemove = new ArrayList<>();
                for (Map.Entry<String, float[]> entry : estimatedTotems.entrySet()) {
                    String key = entry.getKey();
                    if (lastFoundKeys.contains(key)) continue;
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

        });

        HudRenderCallback.EVENT.register(TotemTimer::renderHud);
    }

    private static void onSound(String path) {
        if (!WynnExtrasConfig.INSTANCE.totemTimerEnabled || !WynnExtrasConfig.INSTANCE.totemTimerEstimate) return;
        if (!path.contains("underwater.enter")) return;

        estimatedTotems.forEach((k, v) -> {
            v[0] = 10;
            v[1] = tickCounter;
        });
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.totemTimerEnabled) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;
        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;

        if (!totems.isEmpty()) {
            float ts = c.totemTimerScale;
            int lineH = (int) (10 * ts);
            int baseX = c.totemTimerX == -1 ? mc.getWindow().getScaledWidth() / 2 : c.totemTimerX;
            int baseY = c.totemTimerY;
            int i = 0;
            for (TotemInfo t : totems) {
                String timeDisplay = t.timeText().trim();
                if (timeDisplay.isEmpty()) timeDisplay = "?";

                String line = (c.totemTimerOwnOnly && c.totemTimerTimeOnly) ? timeDisplay
                        : (c.totemTimerOwnOnly ? ("Totem: " + timeDisplay)
                        : (t.owner() + "'s Totem: " + timeDisplay));

                Integer override = WynnExtrasConfig.INSTANCE.hudColorOverrides.get("totem");
                boolean useSolid = c.totemTimerSolidColor && override != null;
                int color = t.estimated() ? 0xFFAAAAAA
                        : (useSolid ? (override | 0xFF000000) : timeColor(t.timeText()));

                int tw = mc.textRenderer.getWidth(line);
                int th = mc.textRenderer.fontHeight;

                WynnExtrasConfig.Align align = c.totemTimerAlignment;

                int previewTw = mc.textRenderer.getWidth("PlayerName's Totem: 38s");

                int textOffsetX;
                if (align == WynnExtrasConfig.Align.LEFT) {
                    textOffsetX = -previewTw / 2;
                } else if (align == WynnExtrasConfig.Align.RIGHT) {
                    textOffsetX = previewTw / 2 - tw;
                } else {
                    textOffsetX = -tw / 2;
                }

                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(baseX, baseY + i * lineH);
                ctx.getMatrices().scale(ts, ts);
                ctx.drawText(mc.textRenderer, line, textOffsetX, -th / 2, color, true);
                ctx.getMatrices().popMatrix();
                i++;
            }
        }

        if (warningActive && c.totemTimerWarningText) {
            String alarmText = "RECAST TOTEM!";
            float as = c.totemWarningScale;
            int wx = c.totemWarningX == -1 ? mc.getWindow().getScaledWidth() / 2 : c.totemWarningX;
            int wy = c.totemWarningY;

            int tw = mc.textRenderer.getWidth(alarmText);
            int th = mc.textRenderer.fontHeight;

            WynnExtrasConfig.Align align = c.totemWarningAlignment;

            int previewTw = mc.textRenderer.getWidth("RECAST TOTEM!");

            int textOffsetX;
            if (align == WynnExtrasConfig.Align.LEFT) {
                textOffsetX = -previewTw / 2;
            } else if (align == WynnExtrasConfig.Align.RIGHT) {
                textOffsetX = previewTw / 2 - tw;
            } else {
                textOffsetX = -tw / 2;
            }

            int color = 0xFF000000 | WynnExtrasConfig.INSTANCE.totemTimerWarningTextColor.getRGB();

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(wx, wy);
            ctx.getMatrices().scale(as, as);
            ctx.drawText(mc.textRenderer, alarmText, textOffsetX, -th / 2, color, true);
            ctx.getMatrices().popMatrix();
        }
    }
}
