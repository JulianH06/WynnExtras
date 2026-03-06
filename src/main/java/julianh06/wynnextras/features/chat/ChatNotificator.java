package julianh06.wynnextras.features.chat;

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
    private static int activeColor = 0xFFFFFFFF;

    private static Command testCmd = new Command(
            "notifiertest",
            "",
            context -> {
                displayAndPlaySound("test");
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

        WynnExtrasConfig.INSTANCE.syncPremades();

        for(Map.Entry<String, Boolean> entry : WynnExtrasConfig.INSTANCE.premades.entrySet()) {
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
        float scale = c.notifierScale;
        int textW = (int) (mc.textRenderer.getWidth(activeText) * scale);
        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();
        int x = c.notifierX == -1 ? (screenW - textW) / 2 : c.notifierX;
        int y = c.notifierY == -1 ? (int) (screenH * 0.3f) : c.notifierY;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(mc.textRenderer, activeText, 0, 0, activeColor, true);
        ctx.getMatrices().popMatrix();
    }
}
