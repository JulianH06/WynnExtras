package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.wynncraft.state.AbilityState;
import julianh06.wynnextras.wynncraft.item.GearType;
import julianh06.wynnextras.wynncraft.item.WynnItemParser;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.LocalAspectStorage;
import julianh06.wynnextras.features.inventory.BankOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.item.ItemStack;

import java.util.Map;

public class BloodSorrowTimer {
    private static long lastStartMs = Long.MIN_VALUE / 2;
    private static long timerEndMs = 0;

    private static int lastSelectedSlot = -1;
    private static int skipEstimatesTicks = 0;
    private static final int SKIP_TICKS_AFTER_SLOT_CHANGE = 10; // ~0.5s
    private static int lastBloodPoolValue = -1;
    private static long soundFiredAt = 0;
    private static long bloodPoolDroppedAt = 0;
    private static final long TRIGGER_WINDOW_MS = 500;

    private static int cachedAcolyteBonus = -1;
    private static String cachedAcolyteBonusClassId = null;

    public static void invalidateAcolyteCache() {
        cachedAcolyteBonus = -1;
        cachedAcolyteBonusClassId = null;
    }

    private static int getAcolyteBonus() {
        if(!WynnExtrasConfig.INSTANCE.autoDetectBloodSorrowTime && !WynnExtrasConfig.INSTANCE.autoDetectAcolyteAspectTier) {
            return switch (WynnExtrasConfig.INSTANCE.acolyteAspect) {
                case 1, 2 -> 250;
                case 3 -> 500;
                default -> 0;
            };
        }

        String classId = BankOverlay.currentCharacterID;
        if (classId == null || classId.isEmpty()) return 0;

        if (cachedAcolyteBonus >= 0 && classId.equals(cachedAcolyteBonusClassId)) {
            return cachedAcolyteBonus;
        }

        Map<String, String> active = LocalAspectStorage.loadActiveAspects(classId);

        int result = 0;

        for (Map.Entry<String, String> e : active.entrySet()) {
            if (!e.getKey().contains("Acolyte")) continue;

            String tierLine = e.getValue();
            if (tierLine.contains("Tier III")) result = 500;
            else if (tierLine.contains("Tier II")) result = 250;
            else if (tierLine.contains("Tier I")) result = 250;
        }

        cachedAcolyteBonus = result;
        cachedAcolyteBonusClassId = classId;
        return result;
    }

    private static boolean hasResonance() {
        if(!WynnExtrasConfig.INSTANCE.autoDetectBloodSorrowTime && !WynnExtrasConfig.INSTANCE.autoDetectResonanceInHand) {
            return WynnExtrasConfig.INSTANCE.resoInHand;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        ItemStack held = mc.player.getMainHandStack();
        if (held == null || held.isEmpty()) return false;
        String name = held.getName().getString().replaceAll("§.", "").trim();
        return name.contains("Resonance");
    }

    private static void onSound(String path) {
        if (!WynnExtrasConfig.INSTANCE.bloodSorrowTimerEnabled) return;
        if (!path.contains("wither_skeleton.hurt")) return;
        soundFiredAt = System.currentTimeMillis();
        if (AbilityState.bloodPoolValue().isPresent()) {
            startTimer();
            soundFiredAt = 0;
            bloodPoolDroppedAt = 0;
        } else {
            tryStartTimer();
        }
    }

    public static boolean isActive() {
        return System.currentTimeMillis() < timerEndMs;
    }

    public static float getRemaining() {
        return Math.max(0, (timerEndMs - System.currentTimeMillis()) / 1000f);
    }

    private static void startTimer() {
        long now = System.currentTimeMillis();
        boolean resonance = hasResonance();
        long duration = (resonance ? 1250 : 5000) + getAcolyteBonus() * (resonance ? 1L : 4L);
        if (now - lastStartMs <= duration + 100) return;
        lastStartMs = now;
        timerEndMs = now + duration;
    }

    private static void tryStartTimer() {
        if (soundFiredAt == 0 || bloodPoolDroppedAt == 0) return;
        if (Math.abs(soundFiredAt - bloodPoolDroppedAt) > TRIGGER_WINDOW_MS) return;
        startTimer();
        soundFiredAt = 0;
        bloodPoolDroppedAt = 0;
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
            if (!WynnExtrasConfig.INSTANCE.bloodSorrowTimerEnabled) return;
            if (client.player == null) return;

            int currentValue = AbilityState.bloodPoolValue().orElse(-1);

            if (lastBloodPoolValue >= 0) {
                boolean largeDrop = currentValue >= 0 && lastBloodPoolValue - currentValue > 50;
                boolean barDisappeared = currentValue < 0 && lastBloodPoolValue > 50;
                if (largeDrop || barDisappeared) {
                    bloodPoolDroppedAt = System.currentTimeMillis();
                    tryStartTimer();
                }
            }
            lastBloodPoolValue = currentValue;
        });

        HudRenderCallback.EVENT.register(BloodSorrowTimer::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if(MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getInventory() != null) {
            int currentSlot = MinecraftClient.getInstance().player.getInventory().getSelectedSlot();
            if (lastSelectedSlot != -1 && currentSlot != lastSelectedSlot) {
                lastStartMs = 0;
                timerEndMs = 0;
                skipEstimatesTicks = SKIP_TICKS_AFTER_SLOT_CHANGE;
            }

            lastSelectedSlot = currentSlot;

            if (skipEstimatesTicks > 0) {
                skipEstimatesTicks--;
                return;
            }
        }

        if (!WynnExtrasConfig.INSTANCE.bloodSorrowTimerEnabled) return;
        long now = System.currentTimeMillis();
        if (now >= timerEndMs) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;
        ItemStack held = mc.player.getMainHandStack();
        GearType heldGearType = WynnItemParser.parse(held).map(item -> item.gearType()).orElse(GearType.UNKNOWN);
        if (held == null || held.isEmpty() || heldGearType != GearType.UNKNOWN && heldGearType != GearType.RELIK) {
            lastStartMs = 0;
            timerEndMs = 0;
            return;
        }

        float remaining = (timerEndMs - now) / 1000f;
        int color = remaining > 1.0f ? 0xFF44FF44 : remaining > 0.5f ? 0xFFFFFF00 : 0xFFFF4444;
        String text = String.format("Blood Sorrow: %.1fs", remaining);

        float bs = WynnExtrasConfig.INSTANCE.bloodSorrowTimerScale;
        int x = WynnExtrasConfig.INSTANCE.bloodSorrowTimerX == -1 ? mc.getWindow().getScaledWidth() / 2 : WynnExtrasConfig.INSTANCE.bloodSorrowTimerX;
        int y = WynnExtrasConfig.INSTANCE.bloodSorrowTimerY;

        int tw = mc.textRenderer.getWidth(text);
        int th = mc.textRenderer.fontHeight;

        WynnExtrasConfig.Align align = WynnExtrasConfig.INSTANCE.bloodSorrowAlignment;

        int previewTw = mc.textRenderer.getWidth("Blood Sorrow: 1.7s");

        int textOffsetX;
        if (align == WynnExtrasConfig.Align.LEFT) {
            textOffsetX = -previewTw / 2;
        } else if (align == WynnExtrasConfig.Align.RIGHT) {
            textOffsetX = previewTw / 2 - tw;
        } else {
            textOffsetX = -tw / 2;
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(bs, bs);
        ctx.drawText(mc.textRenderer, text, textOffsetX, -th / 2, color, true);
        ctx.getMatrices().popMatrix();
    }
}
