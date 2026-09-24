package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.wynncraft.item.GearType;
import julianh06.wynnextras.wynncraft.item.WynnItemParser;
import julianh06.wynnextras.utils.MinecraftUtils;
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
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TotemTimer {
    private static int lastSelectedSlot = -1;
    private static final Style EFFECT_ICON_STYLE = Style.EMPTY.withFont(
            new StyleSpriteSource.Font(Identifier.of("minecraft", "common")));
    private static final Pattern EFFECT_VALUE = Pattern.compile(
            "([^\\p{L}\\p{N}\\s.+~'\\-])\\ufe0f?\\s*(~?\\d+(?:\\.\\d+)?(?:[kKmMbB]|s)?)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern REGENERATION_VALUE = Pattern.compile("\\+(\\d+)\\u2764/s");

    private enum TotemEffect {
        REGENERATION(WynnExtrasConfig.TOTEM_TIMER_EFFECT_REGENERATION, Set.of("\u2764")),
        ELDRITCH_TRANSFUSION(WynnExtrasConfig.TOTEM_TIMER_EFFECT_ELDRITCH_TRANSFUSION, Set.of("\ue020")),
        TOXOPLASMOSIS(WynnExtrasConfig.TOTEM_TIMER_EFFECT_TOXOPLASMOSIS, Set.of("\ue011")),
        INVIGORATING_WAVE(WynnExtrasConfig.TOTEM_TIMER_EFFECT_INVIGORATING_WAVE, Set.of("\ue013")),
        DURATION(WynnExtrasConfig.TOTEM_TIMER_EFFECT_DURATION, Set.of("\ue01f"));

        private final String configId;
        private final Set<String> icons;

        TotemEffect(String configId, Set<String> icons) {
            this.configId = configId;
            this.icons = icons;
        }

        private static TotemEffect fromIcon(String icon) {
            for (TotemEffect effect : values()) {
                if (effect.icons.contains(icon)) return effect;
            }
            return null;
        }

        private static TotemEffect fromConfigId(String configId) {
            for (TotemEffect effect : values()) {
                if (effect.configId.equals(configId)) return effect;
            }
            return null;
        }

        private String icon() {
            return icons.iterator().next();
        }
    }

    public record TotemInfo(String owner, Map<String, String> effectTexts, boolean estimated) {
        public String timeText() {
            return effectTexts.getOrDefault(WynnExtrasConfig.TOTEM_TIMER_EFFECT_DURATION, "");
        }
    }
    private record TotemLineInfo(Map<String, String> effectTexts) {
        private String timeText() {
            return effectTexts.getOrDefault(WynnExtrasConfig.TOTEM_TIMER_EFFECT_DURATION, "");
        }
    }
    private record EffectValue(String icon, String value, TotemEffect effect) {}

    private static boolean isRelik(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return WynnItemParser.parse(stack).map(item -> item.gearType() == GearType.RELIK).orElse(false);
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

    private static List<EffectValue> parseEffectValues(String line) {
        List<EffectValue> values = new ArrayList<>();
        Matcher matcher = EFFECT_VALUE.matcher(line);
        while (matcher.find()) {
            String icon = matcher.group(1);
            String value = matcher.group(2);
            TotemEffect effect = TotemEffect.fromIcon(icon);
            values.add(new EffectValue(icon, value, effect));
        }
        Matcher regenerationMatcher = REGENERATION_VALUE.matcher(line);
        if (regenerationMatcher.find()) {
            String icon = "\u2764";
            String value = regenerationMatcher.group(1);
            values.add(new EffectValue(icon, value, TotemEffect.REGENERATION));
        }
        return values;
    }

    private static int timeColor(String timeText) {
        float secs = parseSeconds(timeText);
        if (secs < 0) return 0xFFFFFFFF;
        if (secs >= 4.0f) return 0xFF44FF44;
        if (secs >= 3.0f) return 0xFFFFFF00;
        if (secs >= 2.0f) return 0xFFFF8800;
        return 0xFFFF4444;
    }

    private static TotemLineInfo parseTotemLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) return new TotemLineInfo(Map.of());

        Map<String, String> effectTexts = new HashMap<>();
        for (EffectValue effectValue : parseEffectValues(trimmed)) {
            if (effectValue.effect() == null) continue;
            String text = switch (effectValue.effect()) {
                case REGENERATION -> "+" + effectValue.value() + "\u2764/s";
                case DURATION -> effectValue.value();
                default -> effectValue.icon() + " " + effectValue.value();
            };
            effectTexts.put(effectValue.effect().configId, text);
        }
        return new TotemLineInfo(effectTexts);
    }

    private static final List<TotemInfo> totems = new ArrayList<>();
    private static boolean warningActive = false;

    // Out-of-render estimation: owner -> {lastKnownSeconds, lastUpdateTick}
    private static final Map<String, float[]> estimatedTotems = new HashMap<>();
    private static final Map<String, Map<String, String>> estimatedTotemEffects = new HashMap<>();
    private static final List<String> lastFoundKeys = new ArrayList<>();
    private static final Set<UUID> lastVisibleTotems = new HashSet<>();
    private static final Set<UUID> invalidatedTotems = new HashSet<>();
    private static long tickCounter = 0;

    public static List<TotemInfo> getTotems() {
        return totems;
    }

    public static String getEffectDisplay(TotemInfo totem) {
        return getEffectDisplayText(totem).getString();
    }

    public static Text getEffectDisplayText(TotemInfo totem) {
        MutableText result = Text.empty();
        boolean first = true;
        for (String effectId : WynnExtrasConfig.INSTANCE.totemTimerActiveEffects) {
            String value = totem.effectTexts().get(effectId);
            if (value == null || value.isBlank()) continue;

            if (!first) result.append(" ");
            first = false;

            TotemEffect effect = TotemEffect.fromConfigId(effectId);
            if (effect == null || effect == TotemEffect.DURATION || effect == TotemEffect.REGENERATION) {
                result.append(value.trim());
                continue;
            }

            String icon = effect.icon();
            String effectValue = value.trim();
            if (effectValue.startsWith(icon)) effectValue = effectValue.substring(icon.length()).trim();
            result.append(Text.literal(icon).setStyle(EFFECT_ICON_STYLE));
            if (!effectValue.isEmpty()) result.append(" " + effectValue);
        }
        return result;
    }

    public static String getHudLine(TotemInfo totem) {
        return getHudText(totem).getString();
    }

    public static Text getHudText(TotemInfo totem) {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        Text effectDisplay = getEffectDisplayText(totem);
        if (effectDisplay.getString().isEmpty()) return Text.empty();
        if (config.totemTimerOwnOnly && config.totemTimerTimeOnly) return effectDisplay;
        if (config.totemTimerOwnOnly) return Text.literal("Totem: ").append(effectDisplay);
        return Text.literal(totem.owner() + "'s Totem: ").append(effectDisplay);
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
                        estimatedTotemEffects.clear();
                        invalidatedTotems.addAll(lastVisibleTotems);
                    }
                }
                lastSelectedSlot = currentSlot;
            }

            warningActive = false;
            if (!WynnExtrasConfig.INSTANCE.totemTimerEnabled) { totems.clear(); return; }
            if (client.world == null || client.player == null) { totems.clear(); return; }

            tickCounter++;
            WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
            String playerName = MinecraftUtils.playerName();

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
                MinecraftUtils.playSoundAmbient(
                    SoundEvent.of(Identifier.of("block.note_block.pling")),
                    c.totemTimerWarningSoundVolume / 100, 2.0f
                );
            }

            if (tickCounter % 2 != 0) return;

            totems.clear();
            double px = client.player.getX(), py = client.player.getY(), pz = client.player.getZ();
            Box searchBox = new Box(px - 64, py - 32, pz - 64, px + 64, py + 32, pz + 64);

            List<DisplayEntity.TextDisplayEntity> allTdes = new ArrayList<>();
            for (Entity e : client.world.getNonSpectatingEntities(Entity.class, searchBox)) {
                if (e instanceof DisplayEntity.TextDisplayEntity tde) allTdes.add(tde);
            }

            Map<String, Integer> ownerCounts = new HashMap<>();
            Set<UUID> visibleTotems = new HashSet<>();
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

                UUID entityId = tde.getUuid();
                visibleTotems.add(entityId);

                if (c.totemTimerOwnOnly && playerName != null && !owner.equals(playerName)) continue;

                Map<String, String> effectTexts = Map.of();
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
                        TotemLineInfo lineInfo = parseTotemLine(l);
                        effectTexts = lineInfo.effectTexts();
                        break;
                    }
                }
                String timeText = effectTexts.getOrDefault(WynnExtrasConfig.TOTEM_TIMER_EFFECT_DURATION, "");
                if (timeText.isEmpty()) {
                    double tx = tde.getX(), ty = tde.getY(), tz = tde.getZ();
                    double bestDist2 = Double.MAX_VALUE;
                    String closestText = null;
                    for (DisplayEntity.TextDisplayEntity other : allTdes) {
                        if (other == tde) continue;
                        String otherRaw = Formatting.strip(other.getText().getString());
                        if (otherRaw == null || otherRaw.isBlank()) continue;
                        double dx = other.getX() - tx, dy = other.getY() - ty, dz = other.getZ() - tz;
                        double dist2 = dx * dx + dy * dy + dz * dz;
                        if (dist2 <= 4.0 && dist2 < bestDist2) {
                            bestDist2 = dist2;
                            closestText = otherRaw.trim();
                        }
                    }
                    if (closestText != null) {
                        TotemLineInfo lineInfo = parseTotemLine(closestText);
                        effectTexts = lineInfo.effectTexts();
                        timeText = lineInfo.timeText();
                    }
                }

                int num = ownerCounts.getOrDefault(owner, 0);
                ownerCounts.put(owner, num + 1);
                String key = owner + "#" + num;
                lastFoundKeys.add(key);

                float secs = parseSeconds(timeText);
                boolean invalidated = invalidatedTotems.contains(entityId);
                if (secs > 0 && !invalidated) {
                    estimatedTotems.put(key, new float[]{ secs, tickCounter });
                    estimatedTotemEffects.put(key, new HashMap<>(effectTexts));
                }

                totems.add(new TotemInfo(owner, Map.copyOf(effectTexts), false));
            }

            invalidatedTotems.retainAll(visibleTotems);
            lastVisibleTotems.clear();
            lastVisibleTotems.addAll(visibleTotems);

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

                    Map<String, String> effectTexts = new HashMap<>(estimatedTotemEffects.getOrDefault(key, Map.of()));
                    effectTexts.put(WynnExtrasConfig.TOTEM_TIMER_EFFECT_DURATION,
                            String.format("~%.0fs", estimatedSecs));

                    String invigoratingWave = effectTexts.get(WynnExtrasConfig.TOTEM_TIMER_EFFECT_INVIGORATING_WAVE);
                    if (invigoratingWave != null) {
                        float effectSeconds = parseSeconds(invigoratingWave) - (ticksElapsed / 20.0f);
                        if (effectSeconds <= 0) {
                            effectTexts.remove(WynnExtrasConfig.TOTEM_TIMER_EFFECT_INVIGORATING_WAVE);
                        } else {
                            effectTexts.put(WynnExtrasConfig.TOTEM_TIMER_EFFECT_INVIGORATING_WAVE,
                                    "\ue013 ~" + (int) Math.ceil(effectSeconds) + "s");
                        }
                    }

                    totems.add(new TotemInfo(owner, Map.copyOf(effectTexts), true));
                }
                for (String key : toRemove) {
                    estimatedTotems.remove(key);
                    estimatedTotemEffects.remove(key);
                }
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
                Text line = getHudText(t);
                if (line.getString().isEmpty()) continue;

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
