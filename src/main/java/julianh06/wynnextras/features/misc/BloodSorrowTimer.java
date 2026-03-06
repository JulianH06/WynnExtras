package julianh06.wynnextras.features.misc;

import com.wynntils.models.character.type.ClassType;
import com.wynntils.core.components.Models;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundInstanceListener;
import net.minecraft.client.sound.WeightedSoundSet;

public class BloodSorrowTimer {

    private static long lastStartMs = Long.MIN_VALUE / 2;
    private static long timerEndMs = 0;

    private static void onSound(String path) {
        if (!WynnExtrasConfig.INSTANCE.bloodSorrowTimerEnabled) return;
        if (Models.Character.getClassType() != ClassType.SHAMAN) return;
        if (!path.contains("wither_skeleton.hurt")) return;
        long now = System.currentTimeMillis();
        if (now - lastStartMs <= 2000) return;
        lastStartMs = now;
        timerEndMs = now + 1750;
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

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(bs, bs);
        ctx.drawText(mc.textRenderer, text, 0, 0, color, true);
        ctx.getMatrices().popMatrix();
    }
}
