package julianh06.wynnextras.features.misc;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.core.components.Models;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.LocalAspectStorage;
import julianh06.wynnextras.features.inventory.BankOverlay;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
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

    private static int getAcolyteBonus() {
        String classId = BankOverlay.currentCharacterID;
        if (classId == null || classId.isEmpty()) return 0;

        Map<String, String> active = LocalAspectStorage.loadActiveAspects(classId);

        int result = 0;
        System.out.println(active);

        for (Map.Entry<String, String> e : active.entrySet()) {
            if (!e.getKey().contains("Acolyte")) continue;

            String tierLine = e.getValue();
            if (tierLine.contains("Tier III")) result = 500;
            else if (tierLine.contains("Tier II")) result = 250;
            else if (tierLine.contains("Tier I")) result = 250;
        }
        return result;
    }

    private static boolean hasResonance() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        ItemStack held = mc.player.getMainHandStack();
        if (held == null || held.isEmpty()) return false;
        String name = held.getName().getString().replaceAll("§.", "").trim();
        return name.equals("Resonance");
    }

    private static void onSound(String path) {
        if (!WynnExtrasConfig.INSTANCE.bloodSorrowTimerEnabled) return;
        if (Models.Character.getClassType() != ClassType.SHAMAN) return;
        if (!path.contains("wither_skeleton.hurt")) return;
        long now = System.currentTimeMillis();
        long duration = (hasResonance() ? 1250 : 5000) + getAcolyteBonus() * (hasResonance() ? 1L : 4L);
        if (now - lastStartMs <= duration + 250) return;
        lastStartMs = now;
        timerEndMs = now + duration;
    }

    public static boolean isActive() {
        return System.currentTimeMillis() < timerEndMs;
    }

    public static float getRemaining() {
        return Math.max(0, (timerEndMs - System.currentTimeMillis()) / 1000f);
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

        HudRenderCallback.EVENT.register(BloodSorrowTimer::renderHud);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.bloodSorrowTimerEnabled) return;
        long now = System.currentTimeMillis();
        if (now >= timerEndMs) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        float remaining = (timerEndMs - now) / 1000f;
        int color = remaining > 1.0f ? 0xFF44FF44 : remaining > 0.5f ? 0xFFFFFF00 : 0xFFFF4444;
        String text = String.format("Blood Sorrow: %.1fs", remaining);

        float bs = WynnExtrasConfig.INSTANCE.bloodSorrowTimerScale;
        int x = WynnExtrasConfig.INSTANCE.bloodSorrowTimerX;
        int y = WynnExtrasConfig.INSTANCE.bloodSorrowTimerY;

        int tw = mc.textRenderer.getWidth(text);
        int th = mc.textRenderer.fontHeight;
        int boxW = (int) ((tw + 6) * bs);
        int boxH = (int) (14 * bs);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x + boxW / 2f, y + boxH / 2f);
        ctx.getMatrices().scale(bs, bs);
        ctx.drawText(mc.textRenderer, text, -tw / 2, -th / 2, color, true);
        ctx.getMatrices().popMatrix();
    }
}
