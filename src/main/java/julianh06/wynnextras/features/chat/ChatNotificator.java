package julianh06.wynnextras.features.chat;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.type.Time;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.annotations.WEModule;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.core.command.Command;
import julianh06.wynnextras.event.ChatEvent;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Locale;
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

    private static final String[] BOMB_KEYWORDS = {"bomb", "bombs", "any prof", "dxp"};
    private static final String[] BOMB_EXCLUDE = {
            "shout", "combat level", "storm", "wynnextras",
            // Cosmetic / non-server-bomb references that mention "bomb" but aren't an actual bomb-active announcement.
            "item bomb", "love bomb", "smoke bomb", "party bomb", "confetti", "glitter", "arrow bomb", "dxp weekend", "dxp month"
    };

    public static void notify(Text message) {
        if(message.getString().contains("You feel like thousands of eyes")) RaidChatNotifier.disableChiropUntil = Time.now().timestamp() + 90_000;

        handleBombshareSuggestion(message);

        for(String notificator : WynnExtrasConfig.INSTANCE.notifierWords) {
            if(!notificator.contains("|")) continue;
            String[] parts = notificator.split("\\|");
            if(message.getString().toLowerCase().contains(parts[0].toLowerCase())) {
                displayAndPlaySound(parts[1]);
            }
        }

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
        if (MinecraftClient.getInstance().options.hudHidden) return;
        if (activeText == null) return;
        long now = System.currentTimeMillis();
        if (now >= expireTimeMs) {
            activeText = null;
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (mc.options.hudHidden) return;

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

    private static void handleBombshareSuggestion(Text message) {
        if (!WynnExtrasConfig.INSTANCE.bombShareSuggestion) return;

        String msg = message.getString().toLowerCase();
        int separatorIndex = msg.indexOf(':');
        if (separatorIndex == -1) return;

        String chatContent = msg.substring(separatorIndex + 1);
        if (chatContent.contains(" with ") && chatContent.contains(" remaining")) return;

        boolean excluded = false;
        for (String ex : BOMB_EXCLUDE) {
            if (chatContent.contains(ex)) {
                excluded = true;
                break;
            }
        }

        if(excluded) return;

        for (String keyword : BOMB_KEYWORDS) {
            if (!chatContent.contains(keyword)) continue;

            boolean lootRelated = chatContent.contains("loot");
            boolean combatRelated = chatContent.contains("combat");
            String channel = detectBombshareChannel(message);
            MinecraftClient.getInstance().send(() -> {
                var text = WynnExtras.addWynnExtrasPrefix(Text.literal(""))
                        .append(Text.literal("§e§n[Share all Bombs]").setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent.RunCommand("/we bombshare " + channel))))
                        .append(Text.literal("  "));
                if (lootRelated) {
                    text.append(Text.literal("§a§n[Loot only]").setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent.RunCommand("/we bombshare " + channel + " loot"))));
                } else if (combatRelated) {
                    text.append(Text.literal("§a§n[Combat only]").setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent.RunCommand("/we bombshare " + channel + " combat"))));
                } else {
                    text.append(Text.literal("§a§n[Prof only]").setStyle(Style.EMPTY
                            .withClickEvent(new ClickEvent.RunCommand("/we bombshare " + channel + " prof"))));
                }
                text.append(Text.literal("  "))
                        .append(Text.literal("§c§n[Disable]").setStyle(Style.EMPTY
                                .withClickEvent(new ClickEvent.RunCommand("/we bombshare toggle"))));
                McUtils.sendMessageToClient(text);
            });
            break;
        }
    }

    private static String detectBombshareChannel(Text message) {
        TextColor firstColor = firstColor(message);
        if (firstColor != null) {
            int rgb = firstColor.getRgb();
            if (rgb == 0x55FFFF) return "guild";
            if (rgb == 0xFFFF55) return "party";
        }

        String prefix = message.getString();
        int separatorIndex = prefix.indexOf(':');
        if (separatorIndex != -1) prefix = prefix.substring(0, separatorIndex);
        prefix = prefix.toLowerCase(Locale.ROOT);

        if (prefix.contains("guild") || prefix.contains("[g]")) return "guild";
        if (prefix.contains("party") || prefix.contains("[p]")) return "party";
        return "all";
    }

    private static TextColor firstColor(Text text) {
        if (text.getStyle().getColor() != null) return text.getStyle().getColor();
        for (Text sibling : text.getSiblings()) {
            TextColor color = firstColor(sibling);
            if (color != null) return color;
        }
        return null;
    }
}
