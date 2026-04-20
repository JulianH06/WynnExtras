package julianh06.wynnextras.features.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class GuildRaidBlockOverlay {
    private static final long DURATION_MS = 2000;
    private static long shownUntil = 0;

    public static void trigger() {
        shownUntil = System.currentTimeMillis() + DURATION_MS;
    }

    public static void render(DrawContext ctx) {
        long now = System.currentTimeMillis();
        if (now >= shownUntil) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return;

        int w = mc.getWindow().getScaledWidth();
        int h = mc.getWindow().getScaledHeight();

        String line1 = "§cBlocked by WynnExtras";
        String line2 = "§7Hold §eSHIFT §7to toggle";

        int w1 = mc.textRenderer.getWidth(line1);
        int w2 = mc.textRenderer.getWidth(line2);
        int maxW = Math.max(w1, w2);
        int boxW = maxW + 16;
        int boxH = 28;
        int bx = w / 2 - boxW / 2;
        int by = h / 8;

        // Fade out in last 400ms
        long remaining = shownUntil - now;
        int alpha = remaining < 400 ? (int) (remaining * 255 / 400) : 255;
        int bgColor = (alpha << 24) | 0x1a1a1a;
        int borderColor = (alpha << 24) | 0xff5555;

        ctx.fill(bx, by, bx + boxW, by + boxH, bgColor);
        ctx.fill(bx, by, bx + boxW, by + 1, borderColor);
        ctx.fill(bx, by + boxH - 1, bx + boxW, by + boxH, borderColor);
        ctx.fill(bx, by, bx + 1, by + boxH, borderColor);
        ctx.fill(bx + boxW - 1, by, bx + boxW, by + boxH, borderColor);

        ctx.drawCenteredTextWithShadow(mc.textRenderer, line1, w / 2, by + 5, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(mc.textRenderer, line2, w / 2, by + 16, 0xFFFFFFFF);
    }
}
