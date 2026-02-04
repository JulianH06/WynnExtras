package julianh06.wynnextras.features.lootruns;

import com.wynntils.core.components.Models;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class LootrunHudOverlay {

    private static int xPos = 5;
    private static int yPos = 100;
    private static final int WIDTH = 150;
    private static final int LINE_HEIGHT = 10;
    private static final float TEXT_SCALE = 1.0f;

    private static boolean isDragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static boolean configLoaded = false;

    // Colors
    private static final CustomColor TITLE_COLOR = CustomColor.fromHexString("AA00FF");
    private static final CustomColor HEADER_COLOR = CustomColor.fromHexString("FFFFFF");
    private static final CustomColor VALUE_COLOR = CustomColor.fromHexString("AAAAAA");
    private static final CustomColor MYTHIC_COLOR = CustomColor.fromHexString("AA00AA");
    private static final CustomColor FABLED_COLOR = CustomColor.fromHexString("FF5555");
    private static final CustomColor LEGENDARY_COLOR = CustomColor.fromHexString("55FFFF");
    private static final CustomColor SESSION_COLOR = CustomColor.fromHexString("55FF55");

    public static void register() {
        HudRenderCallback.EVENT.register(LootrunHudOverlay::render);
    }

    private static void loadConfig() {
        if (configLoaded) return;
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        xPos = config.lootrunHudX;
        yPos = config.lootrunHudY;
        configLoaded = true;
    }

    private static void saveConfig() {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        config.lootrunHudX = xPos;
        config.lootrunHudY = yPos;
        WynnExtrasConfig.save();
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        if (!WynnExtrasConfig.INSTANCE.lootrunHudEnabled) return;
        if (!Models.WorldState.onWorld()) return;

        // Only show if we have session activity
        LootrunStatistics stats = LootrunStatistics.INSTANCE;
        if (stats.sessionPulls == 0) return;

        loadConfig();

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        // Don't render during certain screens
        if (mc.currentScreen != null && !(mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen)) {
            return;
        }

        int y = yPos;
        int x = xPos;

        // Background
        if (WynnExtrasConfig.INSTANCE.lootrunHudBackground) {
            int height = calculateHeight();
            context.fill(x - 2, y - 2, x + WIDTH + 2, y + height + 2, 0xAA000000);
        }

        // Title
        renderText(context, "§5§lLOOTRUN STATS", x, y, TITLE_COLOR);
        y += LINE_HEIGHT + 2;

        // Session duration
        renderText(context, "§7Session: §a" + stats.getSessionDuration(), x, y, SESSION_COLOR);
        y += LINE_HEIGHT;

        // Pulls
        renderText(context, "§fPulls: §7" + stats.sessionPulls, x, y, HEADER_COLOR);
        y += LINE_HEIGHT;

        // Mythics
        renderText(context, "§5Mythics: §d" + stats.sessionMythics, x, y, MYTHIC_COLOR);
        y += LINE_HEIGHT;

        // Fabled
        renderText(context, "§cFabled: §c" + stats.sessionFabled, x, y, FABLED_COLOR);
        y += LINE_HEIGHT;

        // Legendary
        renderText(context, "§bLegendary: §b" + stats.sessionLegendary, x, y, LEGENDARY_COLOR);
        y += LINE_HEIGHT;

        // Shiny
        if (stats.sessionShiny > 0) {
            renderText(context, "§eShiny: §e" + stats.sessionShiny, x, y, CustomColor.fromHexString("FFFF55"));
            y += LINE_HEIGHT;
        }

        // Separator
        y += 4;

        // Pulls per mythic
        String ppm = stats.sessionMythics > 0 ?
                String.format("%.1f", stats.getSessionPullsPerMythic()) : "N/A";
        renderText(context, "§7Pulls/Mythic: §f" + ppm, x, y, VALUE_COLOR);
        y += LINE_HEIGHT;

        // Current dry streak
        renderText(context, "§7Dry streak: §f" + stats.sessionDryStreak, x, y, VALUE_COLOR);
        y += LINE_HEIGHT;

        // Time per pull
        if (stats.sessionPulls > 0) {
            String tpp = String.format("%.1fs", stats.getSessionTimePerPull());
            renderText(context, "§7Time/Pull: §f" + tpp, x, y, VALUE_COLOR);
        }
    }

    private static int calculateHeight() {
        int lines = 8; // Base lines
        if (LootrunStatistics.INSTANCE.sessionShiny > 0) lines++;
        return (lines * LINE_HEIGHT) + 10;
    }

    private static void renderText(DrawContext context, String text, int x, int y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context,
                StyledText.fromString(text),
                x, y,
                color,
                HorizontalAlignment.LEFT,
                VerticalAlignment.TOP,
                TextShadow.NORMAL,
                TEXT_SCALE
        );
    }

    public static void handleMouseClick(double mouseX, double mouseY, int button) {
        if (!WynnExtrasConfig.INSTANCE.lootrunHudEnabled) return;
        if (LootrunStatistics.INSTANCE.sessionPulls == 0) return;

        loadConfig();

        int height = calculateHeight();

        // Check if click is within overlay bounds
        if (mouseX >= xPos - 2 && mouseX <= xPos + WIDTH + 2 &&
                mouseY >= yPos - 2 && mouseY <= yPos + height + 2) {

            if (button == 0) { // Left click - start dragging
                isDragging = true;
                dragOffsetX = (int) mouseX - xPos;
                dragOffsetY = (int) mouseY - yPos;
            }
        }
    }

    public static void handleMouseDrag(double mouseX, double mouseY) {
        if (isDragging) {
            xPos = (int) mouseX - dragOffsetX;
            yPos = (int) mouseY - dragOffsetY;

            // Clamp to screen bounds
            MinecraftClient mc = MinecraftClient.getInstance();
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            xPos = Math.max(0, Math.min(xPos, screenWidth - WIDTH));
            yPos = Math.max(0, Math.min(yPos, screenHeight - 100));
        }
    }

    public static void handleMouseRelease() {
        if (isDragging) {
            isDragging = false;
            saveConfig();
        }
    }
}
