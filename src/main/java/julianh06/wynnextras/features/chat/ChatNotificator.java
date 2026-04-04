package julianh06.wynnextras.features.chat;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ChatEvent;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Map;


@WEModule
public class ChatNotificator {
    private static String activeText = null;
    private static long expireTimeMs = 0;
    private static long startTimeMs = 0;
    private static int activeColor = 0xFFFFFFFF;

    private static Command testCmd = new Command(
            "notifiertest",
            "",
            context -> {
                displayAndPlaySound("Test");
                return 1;
            },
            null,
            null
    );

    public static void init() {
        HudRenderCallback.EVENT.register(ChatNotificator::renderHud);
    }

    @SubscribeEvent
    void recieveMessageGame(ChatEvent event) {
        notify(event.message);
    }

    private static void notify(Text message) {
        if(message.getString().contains("You feel like thousands of eyes")) RaidChatNotifier.disableChiropUntil = Time.now().timestamp() + 90_000;

        for(String notificator : WynnExtrasConfig.INSTANCE.notifierWords) {
            if(!notificator.contains("|")) return;
            String[] parts = notificator.split("\\|");
            if(message.getString().toLowerCase().contains(parts[0].toLowerCase())) {
                displayAndPlaySound(parts[1]);
            }
        }

        //TODO: add twp to pv loot tracker aasdfhjk

        WynnExtrasConfig.INSTANCE.syncPremades();

        for(Map.Entry<String, Boolean> entry : WynnExtrasConfig.INSTANCE.premades.entrySet()) {
            if(message.getString().contains(":")) continue;

            String[] parts = entry.getKey().split("\\|");
            if(parts.length != 2) continue;
            String trigger = parts[0];
            String display = parts[1];
            boolean enabled = entry.getValue();

            if(!enabled) continue;

            if(message.getString().toLowerCase().contains(trigger.toLowerCase())) {
                displayAndPlaySound(display);
            }
        }
    }

    private static void displayAndPlaySound(String display) {
        activeText = display;
        activeColor = WynnExtrasConfig.INSTANCE.textColor.getRGB() | 0xFF000000;
        startTimeMs = System.currentTimeMillis();
        expireTimeMs = System.currentTimeMillis() + WynnExtrasConfig.INSTANCE.textDurationInMs;
        McUtils.playSoundAmbient(SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.notificationSound.getSoundId())), WynnExtrasConfig.INSTANCE.soundVolume / 100, WynnExtrasConfig.INSTANCE.soundPitch / 100);
    }

    private static void renderHud(DrawContext ctx, RenderTickCounter tickCounter) {
        if (activeText == null) return;
        long now = System.currentTimeMillis();
        if (now >= expireTimeMs) {
            activeText = null;
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        WynnExtrasConfig c = WynnExtrasConfig.INSTANCE;
        long fadeInMs = c.notifierFadeInMs;
        long fadeOutMs = c.notifierFadeOutMs;
        long elapsed = now - startTimeMs;
        long remaining = expireTimeMs - now;

        float alpha;
        if (elapsed < fadeInMs) {
            alpha = fadeInMs > 0 ? (float) elapsed / fadeInMs : 1f;
        } else if (remaining < fadeOutMs) {
            alpha = fadeOutMs > 0 ? (float) remaining / fadeOutMs : 1f;
        } else {
            alpha = 1.0f;
        }
        alpha = Math.max(0f, Math.min(1f, alpha));

        float scale = c.notifierScale;
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int cx = c.notifierX == -1 ? screenW / 2 : c.notifierX;
        int cy = c.notifierY == -1 ? (int) (screenH * 0.3f) : c.notifierY;

        int tw = mc.textRenderer.getWidth(activeText);
        int th = mc.textRenderer.fontHeight;

        WynnExtrasConfig.Align align = c.notifierAlignment;

        int previewTw = mc.textRenderer.getWidth("NOTIFICATION");

        int textOffsetX;
        if (align == WynnExtrasConfig.Align.LEFT) {
            textOffsetX = -previewTw / 2;
        } else if (align == WynnExtrasConfig.Align.RIGHT) {
            textOffsetX = previewTw / 2 - tw;
        } else {
            textOffsetX = -tw / 2;
        }

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(mc.textRenderer, activeText, textOffsetX, -th / 2, CustomColor.fromInt(activeColor).withAlpha(alpha).asInt(), true);
        ctx.getMatrices().popMatrix();
    }
}
