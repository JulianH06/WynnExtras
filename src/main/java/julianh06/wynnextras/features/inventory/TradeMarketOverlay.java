package julianh06.wynnextras.features.inventory;

import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.core.text.StyledText;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.TextShadow;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.WynnExtras;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trade Market Overlay - Shows "Value if all sold" on Your Trades screen
 */
public class TradeMarketOverlay {

    // Your Trades screen title (unicode escape sequence)
    private static final String YOUR_TRADES_TITLE = "\uDAFF\uDFE8\uE013";

    // Patterns for parsing
    private static final Pattern STATUS_PATTERN = Pattern.compile("(Pending|Fulfilled) - (Sold|Bought) (\\d+)/(\\d+) items?");
    // Parse stx from bracket notation like (5stx ...)
    private static final Pattern STX_PATTERN = Pattern.compile("\\((\\d+)stx");
    // Parse le from bracket notation like (... 9.60¼²) - number before ¼
    private static final Pattern LE_PATTERN = Pattern.compile("([\\d.]+)¼");
    // Parse eb from bracket notation like (27²½ ...) - number before ²½
    private static final Pattern EB_PATTERN = Pattern.compile("([\\d.]+)²½");
    // Parse raw emeralds like (... 32²) - number before ² but NOT before ²½
    private static final Pattern E_PATTERN = Pattern.compile("([\\d.]+)²(?!½)");

    // Colors
    private static final CustomColor BRAND_COLOR = CustomColor.fromHexString("7DCEA0");
    private static final CustomColor TITLE_COLOR = CustomColor.fromHexString("FFAA00");
    private static final CustomColor LABEL_COLOR = CustomColor.fromHexString("FFFFFF");
    private static final CustomColor PENDING_COLOR = CustomColor.fromHexString("FFFF55");
    private static final CustomColor SOLD_COLOR = CustomColor.fromHexString("55FF55");
    private static final CustomColor COUNT_COLOR = CustomColor.fromHexString("AAAAAA");

    // Position - will be saved to config
    private static int xPos = 10;
    private static int yPos = 10;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 55;
    private static final float TEXT_SCALE = 1.0f;
    private static final int LINE_HEIGHT = 11;

    // Dragging state
    private static boolean isDragging = false;
    private static int dragOffsetX = 0;
    private static int dragOffsetY = 0;
    private static boolean configLoaded = false;

    public static void register() {
        HudRenderCallback.EVENT.register(TradeMarketOverlay::render);
    }

    private static void loadConfig() {
        if (configLoaded) return;
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        xPos = config.tradeMarketOverlayX;
        yPos = config.tradeMarketOverlayY;
        configLoaded = true;
    }

    private static void saveConfig() {
        WynnExtrasConfig config = WynnExtrasConfig.INSTANCE;
        config.tradeMarketOverlayX = xPos;
        config.tradeMarketOverlayY = yPos;
        WynnExtrasConfig.save();
    }

    private static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Don't render via HUD callback when screen is open (renderOnScreen handles that)
        if (mc.currentScreen != null) return;
    }

    public static void renderOnScreen(DrawContext context) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen == null) return;

        // Only show on Your Trades screen
        String screenTitle = mc.currentScreen.getTitle().getString();
        if (!screenTitle.equals(YOUR_TRADES_TITLE)) return;

        ScreenHandler handler = McUtils.containerMenu();
        if (handler == null) return;

        loadConfig();

        // Calculate values
        TradeValues values = calculateTradeValues(handler);

        // Render overlay
        renderOverlay(context, mc, values);
    }

    private static TradeValues calculateTradeValues(ScreenHandler handler) {
        long ifAllSoldValue = 0;
        long alreadySoldValue = 0;
        int pendingCount = 0;
        int soldCount = 0;

        for (int i = 0; i < Math.min(54, handler.slots.size()); i++) {
            Slot slot = handler.getSlot(i);
            if (slot == null) continue;

            ItemStack stack = slot.getStack();
            if (stack == null || stack.isEmpty()) continue;

            String itemName = stack.getName().getString();
            if (itemName.equals("Back") || itemName.equals("Sell an Item") ||
                    itemName.contains("Page") || itemName.contains("Filter") ||
                    itemName.equals("Available Slot")) {
                continue;
            }

            if (stack.getComponents() == null) continue;
            LoreComponent loreComponent = stack.getComponents().get(DataComponentTypes.LORE);
            if (loreComponent == null) continue;

            List<Text> loreLines = loreComponent.lines();
            if (loreLines.size() < 4) continue;

            // Parse status from lore[0]
            String statusLine = loreLines.get(0).getString();
            Matcher statusMatcher = STATUS_PATTERN.matcher(statusLine);
            if (!statusMatcher.find()) continue;

            String status = statusMatcher.group(1); // Pending or Fulfilled
            String action = statusMatcher.group(2); // Sold or Bought
            int soldAmount = Integer.parseInt(statusMatcher.group(3));
            int totalAmount = Integer.parseInt(statusMatcher.group(4));

            // Only count sell orders, not buy orders
            if (!action.equals("Sold")) continue;

            // Search all lore lines for price in bracket notation like (5stx 9.60¼²) or (27²½ 32²)
            // Parse stx, le, eb, and raw emeralds separately
            long pricePerItem = 0;
            for (int j = 0; j < loreLines.size(); j++) {
                String loreLine = loreLines.get(j).getString();

                // Look for bracket notation with price
                if (!loreLine.contains("(") || !loreLine.contains("each")) continue;

                long stxValue = 0;
                long leValue = 0;
                long ebValue = 0;
                long eValue = 0;

                // Extract stx (1 stx = 262144 emeralds)
                Matcher stxMatcher = STX_PATTERN.matcher(loreLine);
                if (stxMatcher.find()) {
                    stxValue = Long.parseLong(stxMatcher.group(1)) * 262144;
                }

                // Extract le (1 le = 4096 emeralds)
                Matcher leMatcher = LE_PATTERN.matcher(loreLine);
                if (leMatcher.find()) {
                    double leAmount = Double.parseDouble(leMatcher.group(1));
                    leValue = (long) (leAmount * 4096);
                }

                // Extract eb (1 eb = 64 emeralds)
                Matcher ebMatcher = EB_PATTERN.matcher(loreLine);
                if (ebMatcher.find()) {
                    double ebAmount = Double.parseDouble(ebMatcher.group(1));
                    ebValue = (long) (ebAmount * 64);
                }

                // Extract raw emeralds
                Matcher eMatcher = E_PATTERN.matcher(loreLine);
                if (eMatcher.find()) {
                    eValue = (long) Double.parseDouble(eMatcher.group(1));
                }

                pricePerItem = stxValue + leValue + ebValue + eValue;
                if (pricePerItem > 0) {
                    break; // Found the price line
                }
            }
            if (pricePerItem == 0) continue;

            // If All Sold = total items * price (for both Pending and Fulfilled)
            ifAllSoldValue += (long) totalAmount * pricePerItem;

            // Already Sold = items that have been sold * price
            alreadySoldValue += (long) soldAmount * pricePerItem;

            // Count items from the X/Y format
            // soldAmount = items already sold, (totalAmount - soldAmount) = items still pending
            soldCount += soldAmount;
            pendingCount += (totalAmount - soldAmount);
        }

        return new TradeValues(ifAllSoldValue, alreadySoldValue, pendingCount, soldCount);
    }

    private static void renderOverlay(DrawContext context, MinecraftClient mc, TradeValues values) {
        context.getMatrices().push();
        context.getMatrices().translate(0, 0, 500); // Render in foreground

        // Draw background
        if (WynnExtrasConfig.INSTANCE.tradeMarketOverlayBackground) {
            // Content: title (LINE_HEIGHT+2) + 3 data lines (LINE_HEIGHT each)
            int contentHeight = (LINE_HEIGHT + 2) + (LINE_HEIGHT * 3);
            int padX = 4;
            int padY = 3;
            int bgX = xPos - padX;
            int bgY = yPos - padY;
            int bgWidth = WIDTH + padX * 2;
            int bgHeight = contentHeight + padY * 2;
            int bgColor = 0xCC1a1a1a;
            drawBackground(context, bgX, bgY, bgX + bgWidth, bgY + bgHeight, bgColor);
        }

        int y = yPos;

        // Brand pill + Title
        Text pillWithTitle = WynnExtras.addWynnExtrasPrefix(Text.literal("Trade Value").styled(s -> s.withColor(TITLE_COLOR.asInt())));
        context.drawText(mc.textRenderer, pillWithTitle, xPos, y, 0xFFFFFF, true);
        y += LINE_HEIGHT + 2;

        // If all sold
        drawText(context, "If all sold:", xPos, y, LABEL_COLOR);
        String pendingValueStr = formatEmeralds(values.ifAllSoldValue);
        drawTextRight(context, pendingValueStr, xPos + WIDTH, y, PENDING_COLOR);
        y += LINE_HEIGHT;

        // Already sold
        drawText(context, "Already sold:", xPos, y, LABEL_COLOR);
        String soldValueStr = formatEmeralds(values.alreadySoldValue);
        drawTextRight(context, soldValueStr, xPos + WIDTH, y, SOLD_COLOR);
        y += LINE_HEIGHT;

        // Counts
        String countStr = "(" + values.pendingCount + " pending, " + values.soldCount + " sold)";
        drawText(context, countStr, xPos, y, COUNT_COLOR);

        context.getMatrices().pop();
    }

    /**
     * Format emeralds: E / 64 = EB / 64 = LE / 64 = stx
     */
    private static String formatEmeralds(long emeralds) {
        // 1 EB = 64 E
        // 1 LE = 64 EB = 4096 E
        // 1 stx = 64 LE = 262144 E

        long stx = emeralds / 262144;
        long remainingAfterStx = emeralds % 262144;
        long le = remainingAfterStx / 4096;
        long remainingAfterLe = remainingAfterStx % 4096;
        long eb = remainingAfterLe / 64;

        StringBuilder sb = new StringBuilder();
        if (stx > 0) {
            sb.append(stx).append("stx ");
        }
        if (le > 0) {
            sb.append(le).append("le ");
        }
        sb.append(eb).append("eb");

        return sb.toString().trim();
    }

    private static void drawText(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context.getMatrices(), StyledText.fromString(text),
                x, y, color, HorizontalAlignment.LEFT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static void drawTextRight(DrawContext context, String text, float x, float y, CustomColor color) {
        FontRenderer.getInstance().renderText(
                context.getMatrices(), StyledText.fromString(text),
                x, y, color, HorizontalAlignment.RIGHT, VerticalAlignment.TOP,
                TextShadow.OUTLINE, TEXT_SCALE);
    }

    private static float getTextWidth(String text) {
        return FontRenderer.getInstance().getFont().getWidth(text) * TEXT_SCALE;
    }

    private static void drawBackground(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        int r = 3; // corner radius
        // Main center rectangle
        context.fill(x1 + r, y1, x2 - r, y2, color);
        // Left and right strips
        context.fill(x1, y1 + r, x1 + r, y2 - r, color);
        context.fill(x2 - r, y1 + r, x2, y2 - r, color);
        // Corner fills (excluding the actual corner pixel for rounded effect)
        // Top-left
        context.fill(x1 + 1, y1 + 1, x1 + r, y1 + r, color);
        // Top-right
        context.fill(x2 - r, y1 + 1, x2 - 1, y1 + r, color);
        // Bottom-left
        context.fill(x1 + 1, y2 - r, x1 + r, y2 - 1, color);
        // Bottom-right
        context.fill(x2 - r, y2 - r, x2 - 1, y2 - 1, color);
    }

    public static boolean handleClick(double mouseX, double mouseY, int button, int action) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) return false;

        String screenTitle = mc.currentScreen.getTitle().getString();
        if (!screenTitle.equals(YOUR_TRADES_TITLE)) return false;

        loadConfig();

        boolean inBounds = mouseX >= xPos && mouseX <= xPos + WIDTH &&
                mouseY >= yPos && mouseY <= yPos + HEIGHT;

        // Release drag
        if (action == 0 && button == 1 && isDragging) {
            isDragging = false;
            saveConfig();
            return true;
        }

        if (!inBounds) return false;

        // Start drag with right click
        if (action == 1 && button == 1) {
            isDragging = true;
            dragOffsetX = (int) mouseX - xPos;
            dragOffsetY = (int) mouseY - yPos;
            return true;
        }

        return inBounds;
    }

    public static void handleMouseMove(double mouseX, double mouseY) {
        if (!isDragging) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) {
            isDragging = false;
            return;
        }

        xPos = (int) mouseX - dragOffsetX;
        yPos = (int) mouseY - dragOffsetY;

        // Clamp to screen bounds
        if (mc.getWindow() != null) {
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            xPos = Math.max(0, Math.min(xPos, screenWidth - WIDTH));
            yPos = Math.max(0, Math.min(yPos, screenHeight - HEIGHT));
        }
    }

    public static boolean isDragging() {
        return isDragging;
    }

    private static class TradeValues {
        final long ifAllSoldValue;
        final long alreadySoldValue;
        final int pendingCount;
        final int soldCount;

        TradeValues(long ifAllSoldValue, long alreadySoldValue, int pendingCount, int soldCount) {
            this.ifAllSoldValue = ifAllSoldValue;
            this.alreadySoldValue = alreadySoldValue;
            this.pendingCount = pendingCount;
            this.soldCount = soldCount;
        }
    }
}