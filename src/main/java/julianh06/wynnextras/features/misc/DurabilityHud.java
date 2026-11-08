package julianh06.wynnextras.features.misc;

import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.wynncraft.item.WynnItemData;
import julianh06.wynnextras.wynncraft.item.WynnItemParser;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

//shows the durability of whatever you're holding (icon + number) on the hud
//
//when it gets low (at or under the % you pick) the text goes red and you get any mix of
//chat message / popup / anvil sound, once per item. at 0 durability you get the big BROKEN
//message in the middle of the screen plus an anvil sound and a chat message, also once per item.
//there's an option to keep the BROKEN banner up the whole time the item is at 0 until you repair it
//
//the durability comes from the item lore ("Durability 350/350") through the existing
//WynnItemParser, items without durability just show nothing
public class DurabilityHud {
    private static final int POPUP_TICKS = 100;      //5 seconds
    private static final int POPUP_FADE_TICKS = 20;
    private static final int BROKEN_TICKS = 70;      //3.5 seconds, only used when the persistent banner is off
    private static final int BROKEN_FADE_TICKS = 20;

    private static ItemStack currentStack = ItemStack.EMPTY;
    private static WynnItemData.Amount current = null;
    private static String warnedItemName = null;     //items we already warned about so it only happens once per item
    private static String brokenItemName = null;
    private static String popupText = null;
    private static int popupTicksLeft = 0;
    private static String brokenText = null;
    private static int brokenTicksLeft = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(DurabilityHud::tick);
        HudRenderCallback.EVENT.register(DurabilityHud::render);
    }

    private static void tick(MinecraftClient client) {
        WynnExtrasConfig cfg = WynnExtrasConfig.INSTANCE;
        if (popupTicksLeft > 0) popupTicksLeft--;
        if (brokenTicksLeft > 0) brokenTicksLeft--;
        current = null;
        if (client.player == null || !cfg.durabilityHudToggle) return;

        currentStack = client.player.getMainHandStack();
        //parse() caches by item components so doing this every tick is cheap
        current = WynnItemParser.parse(currentStack).flatMap(d -> d.durability()).orElse(null);
        if (current == null) return;

        String name = currentStack.getName().getString();
        boolean broken = current.current() <= 0;
        boolean low = current.percentage() <= cfg.durabilityWarnPercent;

        //if it got repaired, let it warn again next time
        if (!broken && name.equals(brokenItemName)) brokenItemName = null;
        if (!low && name.equals(warnedItemName)) warnedItemName = null;

        if (broken && cfg.durabilityBrokenToggle) {
            if (!name.equals(brokenItemName)) {
                brokenItemName = name;
                warnedItemName = name; //skip the normal low durability warning, the broken one already covers it
                brokenText = name;
                brokenTicksLeft = BROKEN_TICKS;
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                        "§4§l" + name + " §cis BROKEN! §7Repair it before you continue."));
                playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0f);
            }
            return;
        }

        if (low && cfg.durabilityWarnToggle && !name.equals(warnedItemName)) {
            warnedItemName = name;
            String text = name + " durability is low: " + current.current() + "/" + current.maximum()
                    + " (" + current.percentage() + "%)";
            if (cfg.durabilityWarnChat) {
                MinecraftUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§c" + text));
            }
            if (cfg.durabilityWarnPopup) {
                popupText = text;
                popupTicksLeft = POPUP_TICKS;
            }
            if (cfg.durabilityWarnSound) playSound(SoundEvents.BLOCK_ANVIL_USE, 1.0f);
        }
    }

    private static void playSound(SoundEvent sound, float pitch) {
        MinecraftUtils.playSoundAmbient(sound, 1.0f, pitch);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        WynnExtrasConfig cfg = WynnExtrasConfig.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!cfg.durabilityHudToggle || mc.player == null || mc.options.hudHidden) return;

        renderPopup(ctx, mc);
        renderBroken(ctx, mc);
        if (current == null) return;

        String text = switch (cfg.durabilityFormat) {
            case RAW -> current.current() + "/" + current.maximum();
            case PERCENT -> current.percentage() + "%";
            case BOTH -> current.current() + "/" + current.maximum() + " (" + current.percentage() + "%)";
        };
        boolean low = cfg.durabilityWarnToggle && current.percentage() <= cfg.durabilityWarnPercent;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cfg.durabilityHudX, cfg.durabilityHudY);
        ctx.getMatrices().scale(cfg.durabilityHudScale, cfg.durabilityHudScale);
        ctx.drawItem(currentStack, 0, 0);
        ctx.drawText(mc.textRenderer, text, 20, 4, low ? 0xFFFF5555 : 0xFFFFFFFF, true);
        ctx.getMatrices().popMatrix();
    }

    //small red message near the top of the screen that fades out
    private static void renderPopup(DrawContext ctx, MinecraftClient mc) {
        if (popupText == null || popupTicksLeft <= 0) return;
        int alpha = fade(popupTicksLeft, POPUP_FADE_TICKS);
        int w = mc.textRenderer.getWidth(popupText);
        ctx.drawText(mc.textRenderer, popupText, mc.getWindow().getScaledWidth() / 2 - w / 2, 55,
                (alpha << 24) | 0xFF5555, true);
    }

    //the big BROKEN banner across the middle of the screen. normally it fades out after a few
    //seconds, with the persistent option on it stays (with a slight pulse) for as long as the
    //held item is at 0 durability
    private static void renderBroken(DrawContext ctx, MinecraftClient mc) {
        WynnExtrasConfig cfg = WynnExtrasConfig.INSTANCE;
        boolean persistent = cfg.durabilityBrokenToggle && cfg.durabilityBrokenPersistent
                && current != null && current.current() <= 0;

        String itemName;
        int alpha;
        if (persistent) {
            itemName = currentStack.getName().getString();
            alpha = 212 + (int) (43 * Math.sin(System.currentTimeMillis() / 250.0)); //pulses between roughly 170 and 255
        } else if (brokenText != null && brokenTicksLeft > 0) {
            itemName = brokenText;
            alpha = fade(brokenTicksLeft, BROKEN_FADE_TICKS);
        } else {
            return;
        }

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        String title = "BROKEN";
        String sub = itemName + " has 0 durability";
        int fontH = mc.textRenderer.fontHeight;
        int titleW = mc.textRenderer.getWidth(title);
        int subW = mc.textRenderer.getWidth(sub);
        float titleScale = Math.min(12f, screenW * 0.6f / titleW); //scaled so it fills most of the screen width
        float subScale = 2f;

        float titleH = fontH * titleScale;
        float subH = fontH * subScale;
        float gap = 8f;
        float top = screenH / 2f - (titleH + gap + subH) / 2f;

        //dark red band behind the text
        int bandAlpha = Math.max(4, alpha * 0x88 / 255);
        ctx.fill(0, (int) (top - 14), screenW, (int) (top + titleH + gap + subH + 14), (bandAlpha << 24) | 0x300000);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(screenW / 2f, top);
        ctx.getMatrices().scale(titleScale, titleScale);
        ctx.drawText(mc.textRenderer, title, -titleW / 2, 0, (alpha << 24) | 0xFF2222, true);
        ctx.getMatrices().popMatrix();

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(screenW / 2f, top + titleH + gap);
        ctx.getMatrices().scale(subScale, subScale);
        ctx.drawText(mc.textRenderer, sub, -subW / 2, 0, (alpha << 24) | 0xFFFFFF, true);
        ctx.getMatrices().popMatrix();
    }

    //stays at 255 until the last fadeTicks, then fades out. never goes under 8 because text with
    //a really low alpha renders weird
    private static int fade(int ticksLeft, int fadeTicks) {
        return ticksLeft >= fadeTicks ? 255 : Math.max(8, ticksLeft * 255 / fadeTicks);
    }
}
