package julianh06.wynnextras.features.chat;

import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.MinecraftUtils;
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
        if(message.getString().contains("You feel like thousands of eyes")) RaidChatNotifier.disableChiropUntil = System.currentTimeMillis() + 90_000;

        handleBombshareSuggestion(message);

        for(String notificator : WynnExtrasConfig.INSTANCE.notifierWords) {
            if(!notificator.contains("|")) continue;
            String[] parts = notificator.split("\\|");
            if(message.getString().toLowerCase().contains(parts[0].toLowerCase())) {
                displayAndPlaySound(parts[1]);
            }
        }

        WynnExtrasConfig.INSTANCE.syncPremades();

        boolean isOurMessage = message.getString().contains("\uE016\uE018\uE00D");
        for(Map.Entry<String, Boolean> entry : WynnExtrasConfig.INSTANCE.premades.entrySet()) {
            if(!isOurMessage && message.getString().contains(":")) continue;

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
        long now = System.currentTimeMillis();
        startTimeMs = now;
        expireTimeMs = now + WynnExtrasConfig.INSTANCE.textDurationInMs;
        MinecraftUtils.playSoundAmbient(SoundEvent.of(Identifier.of(WynnExtrasConfig.INSTANCE.notificationSound.getSoundId())), WynnExtrasConfig.INSTANCE.soundVolume / 100, WynnExtrasConfig.INSTANCE.soundPitch / 100);
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
        long entranceDurationMs = c.notifierFadeInMs;
        long exitDurationMs = c.notifierFadeOutMs;
        long elapsed = now - startTimeMs;
        long remaining = expireTimeMs - now;
        float entranceProgress = entranceDurationMs > 0
                ? Math.clamp((float) elapsed / entranceDurationMs, 0f, 1f)
                : 1f;
        float exitProgress = exitDurationMs > 0
                ? Math.clamp(1f - (float) remaining / exitDurationMs, 0f, 1f)
                : 0f;

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

        float alpha = 1f;
        float offsetX = 0f;
        float offsetY = 0f;
        float animationScaleX = 1f;
        float animationScaleY = 1f;
        float rotation = 0f;
        float clipLeft = cx + textOffsetX * scale - 2f;
        float clipRight = cx + (textOffsetX + tw) * scale + 2f;
        float clipTop = cy - th / 2f * scale - 2f;
        float clipBottom = cy + th / 2f * scale + 2f;
        boolean clip = false;
        float easedProgress = easeOutCubic(entranceProgress);
        WynnExtrasConfig.NotifierAnimation animation = c.notifierAnimation == null
                ? WynnExtrasConfig.NotifierAnimation.FADE
                : c.notifierAnimation;
        WynnExtrasConfig.NotifierAnimationDirection entranceDirection = c.notifierEntranceDirection == null
                ? WynnExtrasConfig.NotifierAnimationDirection.BOTTOM
                : c.notifierEntranceDirection;

        switch (animation) {
            case APPEAR -> {
            }
            case FADE -> alpha = entranceProgress;
            case FLY_IN -> {
                switch (entranceDirection) {
                    case LEFT -> offsetX = (-cx - tw * scale) * (1f - easedProgress);
                    case RIGHT -> offsetX = (screenW - cx + tw * scale) * (1f - easedProgress);
                    case TOP -> offsetY = (-cy - th * scale) * (1f - easedProgress);
                    case BOTTOM -> offsetY = (screenH - cy + th * scale) * (1f - easedProgress);
                }
            }
            case PEEK_IN -> {
                switch (entranceDirection) {
                    case LEFT -> offsetX = -(clipRight - clipLeft) * (1f - easedProgress);
                    case RIGHT -> offsetX = (clipRight - clipLeft) * (1f - easedProgress);
                    case TOP -> offsetY = -(clipBottom - clipTop) * (1f - easedProgress);
                    case BOTTOM -> offsetY = (clipBottom - clipTop) * (1f - easedProgress);
                }
                clip = entranceProgress < 1f;
            }
            case WIPE -> {
                switch (entranceDirection) {
                    case LEFT -> clipRight = clipLeft + (clipRight - clipLeft) * entranceProgress;
                    case RIGHT -> clipLeft = clipRight - (clipRight - clipLeft) * entranceProgress;
                    case TOP -> clipBottom = clipTop + (clipBottom - clipTop) * entranceProgress;
                    case BOTTOM -> clipTop = clipBottom - (clipBottom - clipTop) * entranceProgress;
                }
                clip = entranceProgress < 1f;
            }
            case ZOOM -> {
                alpha = entranceProgress;
                animationScaleX = 0.25f + 0.75f * easedProgress;
                animationScaleY = animationScaleX;
            }
            case PINWHEEL -> {
                alpha = entranceProgress;
                animationScaleX = 0.1f + 0.9f * easedProgress;
                animationScaleY = animationScaleX;
                rotation = -(1f - easedProgress) * (float) (Math.PI * 2);
            }
            case BOUNCE -> {
                alpha = Math.clamp(entranceProgress * 3f, 0f, 1f);
                offsetX = -Math.min(80f, screenW * 0.15f) * (1f - easedProgress);
                offsetY = (-cy - th * scale) * (1f - easeOutBounce(entranceProgress));
            }
            case EXPAND -> {
                alpha = entranceProgress;
                animationScaleX = easedProgress;
            }
            case FADED_SWIVEL -> {
                float swivelAngle = (1f - easedProgress) * (float) (Math.PI * 1.5);
                alpha = entranceProgress;
                animationScaleX = Math.abs((float) Math.cos(swivelAngle));
                rotation = (float) Math.sin(swivelAngle) * 0.15f;
            }
        }

        float easedExitProgress = easeInCubic(exitProgress);
        WynnExtrasConfig.NotifierExitAnimation exitAnimation = c.notifierExitAnimation == null
                ? WynnExtrasConfig.NotifierExitAnimation.FADE
                : c.notifierExitAnimation;
        WynnExtrasConfig.NotifierAnimationDirection exitDirection = c.notifierExitDirection == null
                ? WynnExtrasConfig.NotifierAnimationDirection.BOTTOM
                : c.notifierExitDirection;

        switch (exitAnimation) {
            case DISAPPEAR -> {
            }
            case FADE -> alpha = Math.min(alpha, 1f - exitProgress);
            case FLY_OUT -> {
                switch (exitDirection) {
                    case LEFT -> offsetX += (-cx - tw * scale) * easedExitProgress;
                    case RIGHT -> offsetX += (screenW - cx + tw * scale) * easedExitProgress;
                    case TOP -> offsetY += (-cy - th * scale) * easedExitProgress;
                    case BOTTOM -> offsetY += (screenH - cy + th * scale) * easedExitProgress;
                }
            }
            case PEEK_OUT -> {
                switch (exitDirection) {
                    case LEFT -> offsetX -= (clipRight - clipLeft) * easedExitProgress;
                    case RIGHT -> offsetX += (clipRight - clipLeft) * easedExitProgress;
                    case TOP -> offsetY -= (clipBottom - clipTop) * easedExitProgress;
                    case BOTTOM -> offsetY += (clipBottom - clipTop) * easedExitProgress;
                }
                clip = clip || exitProgress > 0f;
            }
            case WIPE -> {
                switch (exitDirection) {
                    case LEFT -> clipRight -= (clipRight - clipLeft) * exitProgress;
                    case RIGHT -> clipLeft += (clipRight - clipLeft) * exitProgress;
                    case TOP -> clipBottom -= (clipBottom - clipTop) * exitProgress;
                    case BOTTOM -> clipTop += (clipBottom - clipTop) * exitProgress;
                }
                clip = clip || exitProgress > 0f;
            }
            case ZOOM -> {
                alpha = Math.min(alpha, 1f - exitProgress);
                animationScaleX *= 1f - 0.75f * easedExitProgress;
                animationScaleY *= 1f - 0.75f * easedExitProgress;
            }
            case PINWHEEL -> {
                alpha = Math.min(alpha, 1f - exitProgress);
                animationScaleX *= 1f - 0.9f * easedExitProgress;
                animationScaleY *= 1f - 0.9f * easedExitProgress;
                rotation += easedExitProgress * (float) (Math.PI * 2);
            }
            case BOUNCE -> {
                offsetX += Math.min(80f, screenW * 0.15f) * easedExitProgress;
                offsetY += (screenH - cy + th * scale) * easeInBounce(exitProgress);
            }
            case CONTRACT -> {
                alpha = Math.min(alpha, 1f - exitProgress);
                animationScaleX *= 1f - easedExitProgress;
            }
            case FADED_SWIVEL -> {
                float swivelAngle = easedExitProgress * (float) (Math.PI * 1.5);
                alpha = Math.min(alpha, 1f - exitProgress);
                animationScaleX *= Math.abs((float) Math.cos(swivelAngle));
                rotation += (float) Math.sin(swivelAngle) * 0.15f;
            }
        }

        if (clip) {
            ctx.enableScissor((int) Math.floor(clipLeft), (int) Math.floor(clipTop),
                    (int) Math.ceil(clipRight), (int) Math.ceil(clipBottom));
        }
        int centeredTextX = -tw / 2;
        float textCenterOffsetX = (textOffsetX - centeredTextX) * scale;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx + textCenterOffsetX + offsetX, cy + offsetY);
        ctx.getMatrices().rotate(rotation);
        ctx.getMatrices().scale(scale * animationScaleX, scale * animationScaleY);
        ctx.drawText(mc.textRenderer, activeText, centeredTextX, -th / 2, CustomColor.fromInt(activeColor).withAlpha(alpha).asInt(), true);
        ctx.getMatrices().popMatrix();
        if (clip) ctx.disableScissor();
    }

    private static float easeOutCubic(float progress) {
        float remaining = 1f - progress;
        return 1f - remaining * remaining * remaining;
    }

    private static float easeInCubic(float progress) {
        return progress * progress * progress;
    }

    private static float easeOutBounce(float progress) {
        if (progress < 1f / 2.75f) {
            return 7.5625f * progress * progress;
        }
        if (progress < 2f / 2.75f) {
            progress -= 1.5f / 2.75f;
            return 7.5625f * progress * progress + 0.75f;
        }
        if (progress < 2.5f / 2.75f) {
            progress -= 2.25f / 2.75f;
            return 7.5625f * progress * progress + 0.9375f;
        }
        progress -= 2.625f / 2.75f;
        return 7.5625f * progress * progress + 0.984375f;
    }

    private static float easeInBounce(float progress) {
        return 1f - easeOutBounce(1f - progress);
    }

    private static void handleBombshareSuggestion(Text message) {
        if (!WynnExtrasConfig.INSTANCE.bombShareSuggestion) return;

        String msg = message.getString().toLowerCase();
        if (msg.contains("wynncraft.com/store") || msg.contains("copied to clipboard")) return;

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
                MinecraftUtils.sendMessageToClient(text);
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
