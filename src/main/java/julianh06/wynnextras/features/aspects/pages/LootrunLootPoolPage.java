package julianh06.wynnextras.features.aspects.pages;

import julianh06.wynnextras.core.WynnExtras;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.colors.WynncraftShaderColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.ResetTimeConfig;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.aspects.LootrunLootPoolData;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class LootrunLootPoolPage extends PageWidget {
    private static Map<String, List<LootrunLootPoolData.LootrunItem>> crowdsourcedLootPools = new HashMap<>();
    private final static Map<Camp, ZonedDateTime> lastCrowdsourceFetch = new HashMap<>();
    private static final Map<Camp, Boolean> fetchRunning = new HashMap<>();
    private final static Map<Camp, Boolean> hasOldLootpool = new HashMap<>();

    private final RefreshButton refreshButton;

    public enum Camp { SI, SE, CORK, COTL, MH, WFF, EFF }

    private static String[] campNames = {
        "Sky Islands",
        "Silent Expanse",
        "Corkus Traversal",
        "Canyon of the Lost",
        "Molten Heights",
        "West Fruma Foray",
        "East Fruma Foray"
    };

    static List<LootPoolWidget> lootPoolWidgets = new ArrayList<>();

    private static List<Text> hoveredTooltip = new ArrayList<>();

    private static LootrunLootPoolData.LootrunItem hoveredItem = null;

    private static float hScrollOffset = 0f;
    private static float hScrollTarget = 0f;
    private static float hScrollMax = 0f;
    private static final int FIXED_WIDGET_WIDTH = 550;
    private static final int H_WIDGET_SPACING = 40;
    private static HorizontalScrollBarWidget hScrollBarWidget;

    public LootrunLootPoolPage(AspectScreen parent) {
        super(parent);

        for(Camp camp : Camp.values()) {
            lootPoolWidgets.add(new LootPoolWidget(camp));
        }

        refreshButton = new RefreshButton();

        hScrollBarWidget = new HorizontalScrollBarWidget(
                () -> hScrollTarget,
                v -> hScrollTarget = v,
                () -> hScrollOffset,
                v -> hScrollOffset = v,
                () -> hScrollMax
        );
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        hoveredTooltip = new ArrayList<>();

        float scaleFactor = ui.getScaleFactorF();
        int logicalW = (int) (width * scaleFactor);
        int centerX = logicalW / 2;

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        for (Camp camp : Camp.values()) {
            if (!shouldFetchLootPool(camp)) {
                continue;
            }

            if (fetchRunning.getOrDefault(camp, false)) continue;

            fetchRunning.put(camp, true);

            WynnExtras.LOGGER.info("starting fetch for " + camp);
            lastCrowdsourceFetch.put(camp, now);
            WynncraftApiHandler.fetchCrowdsourcedLootrunLootPool(camp.name()).thenAccept(result -> {
                fetchRunning.put(camp, false);

                if (result == null || result.isEmpty()) return;

                List<LootrunLootPoolData.LootrunItem> oldItems = crowdsourcedLootPools.get(camp.name());

                lastCrowdsourceFetch.put(camp, now);
                if (isSamePool(oldItems, result)) {
                    WynnExtras.LOGGER.info("still old pool, retry in 30s");
                    hasOldLootpool.put(camp, true);
                    return;
                }

                WynnExtras.LOGGER.info("NEW POOL for " + camp);

                crowdsourcedLootPools.put(camp.name(), result);
                hasOldLootpool.put(camp, false);

                LootrunLootPoolData.INSTANCE.saveLootPool(camp.name(), result);
            });
        }

        ui.drawCenteredText("§6§lWeekly Lootrun Lootpools", centerX, 60, CustomColor.fromInt(0xFFFFFF), 3f);

        ZonedDateTime nextReset = ResetTimeConfig.INSTANCE.getNextLootrunReset();
        if (nextReset.isBefore(now) || nextReset.isEqual(now)) {
            nextReset = nextReset.plusWeeks(1);
        }

        // Calculate time difference
        Duration duration = Duration.between(now, nextReset);
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;

        String dayString = days == 1 ? "day" : "days";
        String hourString = hours == 1 ? "hour" : "hours";
        String minuteString = minutes == 1 ? "minute" : "minutes";

        String countdown = "§7Resets in";
        if(days > 0) countdown += " §e" + days + " §7" + dayString;
        if(hours > 0) countdown += " §e" + hours + " §7" + hourString;
        if(minutes > 0) countdown += " §e" + minutes + " §7" + minuteString;

        ui.drawCenteredText(countdown, centerX, 100);

        float scaledWidth = width * ui.getScaleFactorF();
        int totalContentWidth = lootPoolWidgets.size() * (FIXED_WIDGET_WIDTH + H_WIDGET_SPACING) + H_WIDGET_SPACING;
        hScrollMax = Math.max(0, totalContentWidth - scaledWidth);

        if (hScrollTarget > hScrollMax) hScrollTarget = hScrollMax;

        float snapValue = 0.5f;
        float speed = 0.3f;
        float hDiff = hScrollTarget - hScrollOffset;
        if (Math.abs(hDiff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) hScrollOffset = hScrollTarget;
        else hScrollOffset += hDiff * speed * tickDelta;

        int widgetY = 175;
        int scrollBarHeight = 30;
        int widgetHeight = (int) (height * ui.getScaleFactorF() * 0.9f - widgetY - scrollBarHeight - 5);

        context.enableScissor(
                0,
                0,
                (int) (scaledWidth / ui.getScaleFactor()),
                (int) ((widgetY + widgetHeight) / ui.getScaleFactor())
        );

        int widgetX = H_WIDGET_SPACING - (int) hScrollOffset;
        for (LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            lootPoolWidget.setBounds(widgetX, widgetY, FIXED_WIDGET_WIDTH, widgetHeight);
            lootPoolWidget.draw(context, mouseX, mouseY, tickDelta, ui);
            widgetX += FIXED_WIDGET_WIDTH + H_WIDGET_SPACING;
        }
        context.disableScissor();

        int scrollBarY = widgetY + widgetHeight + 5;
        hScrollBarWidget.setBounds(40, scrollBarY, (int) scaledWidth - 80, scrollBarHeight);
        hScrollBarWidget.draw(context, mouseX, mouseY, tickDelta, ui);

        refreshButton.setBounds(0, 0, 300, 60);
        refreshButton.draw(context, mouseX, mouseY, tickDelta, ui);
    }

    private static boolean isSamePool(List<LootrunLootPoolData.LootrunItem> oldItems, List<LootrunLootPoolData.LootrunItem> newItems) {
        if (oldItems == null || newItems == null) return false;
        if (oldItems.size() != newItems.size()) return false;

        Set<String> oldNames = oldItems.stream().map(i -> i.name).collect(Collectors.toSet());

        for (LootrunLootPoolData.LootrunItem item : newItems) {
            if (!oldNames.contains(item.name)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(hoveredTooltip.isEmpty()) return;
        int absX = (int)(mouseX * parent.getMatrixScale());
        int absY = (int)(mouseY * parent.getMatrixScale());
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(), absX, absY + 20);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        long window = MinecraftClient.getInstance().getWindow().getHandle();
        boolean shiftHeld = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (shiftHeld) {
            if (delta > 0) hScrollTarget -= 60f;
            else hScrollTarget += 60f;
            if (hScrollTarget < 0) hScrollTarget = 0;
            if (hScrollTarget > hScrollMax) hScrollTarget = hScrollMax;
            return true;
        }

        for (LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if (lootPoolWidget.mouseScrolled(mx, my, delta)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if(lootPoolWidget.mouseClicked(mx, my, button)) return true;
        }

        if(refreshButton.isHovered()) {
            refreshButton.onClick(button);
            return true;
        }

        if (hScrollBarWidget.isHovered()) {
            hScrollBarWidget.onClick(button);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            lootPoolWidget.mouseReleased(mx, my, button);
        }

        hScrollBarWidget.scrollBarButtonWidget.isHold = false;
        return false;
    }

    private static class LootPoolWidget extends Widget {
        Identifier ltop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/ltop.png");
        Identifier rtop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/rtop.png");
        Identifier ttop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/ttop.png");
        Identifier btop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/btop.png");
        Identifier tltop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/tltop.png");
        Identifier trtop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/trtop.png");
        Identifier bltop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/bltop.png");
        Identifier brtop = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/brtop.png");

        Identifier l = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/l.png");
        Identifier r = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/r.png");
        Identifier t = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/t.png");
        Identifier b = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/b.png");
        Identifier tl = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/tl.png");
        Identifier tr = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/tr.png");
        Identifier bl = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/bl.png");
        Identifier br = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/light/br.png");

        Identifier ltopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/ltop.png");
        Identifier rtopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/rtop.png");
        Identifier ttopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/ttop.png");
        Identifier btopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/btop.png");
        Identifier tltopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/tltop.png");
        Identifier trtopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/trtop.png");
        Identifier bltopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/bltop.png");
        Identifier brtopd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/brtop.png");

        Identifier ld = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/l.png");
        Identifier rd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/r.png");
        Identifier td = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/t.png");
        Identifier bd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/b.png");
        Identifier tld = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/tl.png");
        Identifier trd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/tr.png");
        Identifier bld = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/bl.png");
        Identifier brd = Identifier.of("wynnextras", "textures/gui/lootpoolscreen/dark/br.png");

        LootPoolWidget.ScrollBarWidget scrollBarWidget;

        final Camp camp;
        float targetOffset = 0;
        float actualOffset = 0;
        float maxOffset = 999;
        int textureWidth = 150;

        public LootPoolWidget(Camp camp) {
            super(0, 0, 0, 0);
            scrollBarWidget = new LootPoolWidget.ScrollBarWidget(this);
            this.camp = camp;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int topHeight = 94;

            ui.drawVanillaPanel(x, y, width, height, 12, 17, 17, 80, 21);

            ui.drawCenteredText(campNames[camp.ordinal()], x + width / 2f, y + 45, CustomColor.fromHexString("FFFFFF"));

            List<LootrunLootPoolData.LootrunItem> items = getLootPoolForCamp(camp.name());

            ctx.enableScissor(
                    (int) (x / ui.getScaleFactor()),
                    (int) ((y + 85) / ui.getScaleFactor()),
                    (int) ((x + width - 7) / ui.getScaleFactor()),
                    (int) ((y + height - 20) / ui.getScaleFactor()));

            int contentStartY = y + 20;
            int contentHeight = height - 40;
            int totalContentHeight = 0;

            if (items.isEmpty()) {
                ui.drawCenteredText("§4No data", x + width / 2f, contentStartY + 90, CustomColor.fromInt(0xFFFFFF), 3f);
                ui.drawCenteredText("§7Open lootrun", x + width / 2f, contentStartY + 120, CustomColor.fromInt(0xFFFFFF), 2.5f);
                ui.drawCenteredText("§7chest to scan", x + width / 2f, contentStartY + 150, CustomColor.fromInt(0xFFFFFF), 2.5f);
            } else {
                int itemSpacing = 32;

                ctx.enableScissor(
                        (int) ui.sx(x + 6),
                        (int) ui.sy(contentStartY),
                        (int) ui.sx(x + width - 6),
                        (int) ui.sy(contentStartY + contentHeight)
                );

                float snapValue = 0.5f;
                float speed = 0.3f;
                float diff = (targetOffset - actualOffset);
                if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
                else actualOffset += diff * speed * tickDelta;

                float contentTopPadding = 80f;
                float contentStartTextY = contentStartY + contentTopPadding;

                float textY = contentStartTextY - actualOffset;
                float textX = x + 15;
                textY = drawShinyItems(ctx, textX, textY, items, width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawMythicItems(ctx, textX, textY, items, width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawTomeItems(ctx, textX, textY, items, width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawWardItems(ctx, textX, textY, items, width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Fabled", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Legendary", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Rare", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Set", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Unique", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);

                float contentEndY = textY + actualOffset;
                totalContentHeight = (int)(contentEndY - contentStartTextY);

                ctx.disableScissor();
            }

            maxOffset = Math.max(totalContentHeight - contentHeight + 80, 0);

            if(targetOffset > maxOffset) {
                targetOffset = maxOffset;
            }

            ctx.disableScissor();

            scrollBarWidget.setBounds(x + width - 20, y + 85, 15, height - 105);
            scrollBarWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }

        private float drawShinyItems(DrawContext context, float x, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                   float colWidth, float mouseX, float mouseY, float contentStartY, float contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> shinyItems = items.stream()
                    .filter(i -> i.type.equals("shiny"))
                    .toList();

            if (shinyItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : shinyItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 20;
        }

        private float drawMythicItems(DrawContext context, float x, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                      float colWidth, float mouseX, float mouseY, float contentStartY, float contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> mythicItems = items.stream()
                    .filter(i -> i.rarity.equals("Mythic") && !i.type.equals("shiny"))
                    .toList();

            if (mythicItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : mythicItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 20;
        }

        private float drawWardItems(DrawContext context, float x, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                      float colWidth, float mouseX, float mouseY, float contentStartY, float contentHeight, float scrollOffset) {
            int itemSpacing = 32;

            java.util.Set<String> seenWards = new java.util.HashSet<>();
            List<LootrunLootPoolData.LootrunItem> wardItems = items.stream()
                    .filter(i -> i.name.contains("Ward"))
                    .filter(i -> seenWards.add(i.name)) //to prevent two of the same wards from being rendered
                    .toList();

            if (wardItems.isEmpty()) {
                ui.drawText("No Ward", x + 20, textY, CustomColor.fromHexString("f9508e"), 2.8f);
                return textY + itemSpacing + 20;
            }

            for (LootrunLootPoolData.LootrunItem item : wardItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 20;
        }

        private float drawTomeItems(DrawContext context, float x, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                    float colWidth, float mouseX, float mouseY, float contentStartY, float contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> tomeItems = items.stream()
                    .filter(i -> i.type.equals("tome"))
                    .toList();

            if (tomeItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : tomeItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 20;
        }

        private float drawItemsByRarity(DrawContext context, float x, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                      String rarity, float colWidth, float mouseX, float mouseY, float contentStartY, float contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> filteredItems = items.stream()
                    .filter(i -> i.rarity.equals(rarity) && !i.type.equals("shiny") && !i.type.equals("tome"))
                    .toList();

            if (filteredItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : filteredItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 20;
        }

        private float drawItem(DrawContext context, float x, float textY, LootrunLootPoolData.LootrunItem item,
                               float colWidth, float mouseX, float mouseY, float contentStartY, float contentHeight, float scrollOffset, float itemSpacing) {
            if (textY + itemSpacing >= contentStartY && textY <= contentStartY + contentHeight) {
                boolean hovering = mouseX * ui.getScaleFactorF() >= x + 12 && mouseX * ui.getScaleFactorF() <= x + width - 12 &&
                        mouseY * ui.getScaleFactorF() >= textY && mouseY * ui.getScaleFactorF() <= textY + itemSpacing - 5;

                String rarityColor = item.type.equals("tome") ? "§d" : getRarityColor(item.rarity);
                if(item.name.contains("Ward")) rarityColor = "§#f9508eff";
                String displayName = truncate(item.name, width / 2 - 30).replace("Unidentified ", "");

                if (item.type.equals("shiny")) {
                    ui.drawText(displayName.replace("⬡ ", ""), x + 20, textY, WynnExtrasConfig.INSTANCE.removeChroma ? CustomColor.fromHexString("FFFFFF") : WynncraftShaderColor.RAINBOW.color, 4f);
                } else {
                    ui.drawText(rarityColor + displayName, x + 20, textY, CustomColor.fromInt(0xFFFFFF), 2.8f);
                }
                boolean isShiny = item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty();
                if (isShiny) {
                    ui.drawText("§7" + item.shinyStat.replace(": §f0", ""), x + 20, textY + 45, CustomColor.fromInt(0xFFFFFF), 2.2f);
                }

                if (hovering && WynncraftApiHandler.getCachedItemDatabase() != null && mouseY * ui.getScaleFactorF() > y + 80) {
                    JsonObject jsonItem = WynncraftApiHandler.getCachedItemDatabase().get(item.name.replace("Unidentified ", "").replace("⬡ ", "").replace("Shiny ", ""));
                    List<Text> tooltip = new ArrayList<>();
                    if(rarityColor.startsWith("§#")) {
                        String hex = rarityColor.substring(2); // "12345678"
                        int r = Integer.parseInt(hex.substring(0, 2), 16);
                        int g = Integer.parseInt(hex.substring(2, 4), 16);
                        int b = Integer.parseInt(hex.substring(4, 6), 16);

                        tooltip.add(Text.literal(displayName)
                                .styled(style -> style.withColor(net.minecraft.util.math.ColorHelper.getArgb(255, r, g, b))));
                    } else {
                        tooltip.add(Text.of(rarityColor + item.name.replace("Unidentified ", "")));
                    }

                    if(jsonItem != null && item.name.contains("Tome")) tooltip.addAll(buildTooltipFromApi(jsonItem));
                    hoveredTooltip = tooltip;
                }
            }
            int extraSpacing = (item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty()) ? 40 : 0;
            return textY + itemSpacing + extraSpacing;
        }

        private static List<Text> buildTooltipFromApi(JsonObject item) {
            List<Text> tooltip = new ArrayList<>();

            JsonObject ids = item.getAsJsonObject("identifications");
            if (ids == null) return tooltip;

            for (Map.Entry<String, JsonElement> entry : ids.entrySet()) {
                tooltip.add(Text.literal("§7" + formatLine(entry.getKey())));
            }

            return tooltip;
        }

        private static String formatLine(String key) {
            Map<String, String> special = Map.of(
                    "healthRegenRaw", "Health Regen",
                    "healthRegen", "Health Regen",
                    "manaRegen", "Mana Regen",
                    "manaSteal", "Mana Steal",
                    "lifeSteal", "Life Steal",
                    "rawAttackSpeed", "Attack Speed",
                    "raw1stSpellCost", "1st Spell Cost",
                    "raw2ndSpellCost", "2nd Spell Cost",
                    "raw3rdSpellCost", "3rd Spell Cost",
                    "raw4thSpellCost", "4th Spell Cost"
            );

            String name;
            boolean isPercent = true;

            if (special.containsKey(key)) {
                name = special.get(key);
                isPercent = !key.startsWith("raw") || key.contains("Regen");
            } else {
                name = key.replaceAll("([a-z])([A-Z])", "$1 $2");

                if (name.startsWith("raw ")) {
                    name = name.substring(4);
                    isPercent = false;
                }

                name = String.valueOf(name.charAt(0)).toUpperCase() + name.substring(1);

                if (key.contains("AttackSpeed")) isPercent = false;
                if (key.contains("Cost")) isPercent = false;
                if (key.contains("Steal")) isPercent = false;
                if (key.contains("poison")) isPercent = false;
                if (key.contains("jump")) isPercent = false;
            }

            String percent = isPercent ? " %" : "";

            return name + percent;
        }

        private String truncate(String text, int maxLen) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;

            if (tr.getWidth(text) > maxLen) {
                text = tr.trimToWidth(text, maxLen - tr.getWidth("...")) + "...";
            }
            return text;
        }

        private String capitalize(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }

        private List<LootrunLootPoolData.LootrunItem> getLootPoolForCamp(String campCode) {
            if (crowdsourcedLootPools.containsKey(campCode) && crowdsourcedLootPools.get(campCode) != null) {
                List<LootrunLootPoolData.LootrunItem> items = crowdsourcedLootPools.get(campCode);
                if (!items.isEmpty()) {
                    return items.stream().filter(x -> !x.name.contains("Emerald")).toList();
                }
            }
            return LootrunLootPoolData.INSTANCE.getLootPool(campCode);
        }

        private String getRarityColor(String rarity) {
            return switch (rarity) {
                case "Mythic" -> "§5";
                case "Fabled" -> "§c";
                case "Legendary" -> "§b";
                case "Rare" -> "§d";
                case "Set" -> "§a";
                case "Unique" -> "§e";
                default -> "§f";
            };
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if(scrollBarWidget.isHovered()) {
                scrollBarWidget.onClick(button);
                return true;
            }
            return super.mouseClicked(mx, my, button);
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            scrollBarWidget.scrollBarButtonWidget.isHold = false;
            return super.mouseReleased(mx, my, button);
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double delta) {
            if(!hovered) return false;
            if(delta > 0) targetOffset -= 33f;
            else targetOffset += 33f;
            if(targetOffset < 0) targetOffset = 0;
            if(targetOffset > maxOffset) targetOffset = maxOffset;
            return true;
        }

        private class ScrollBarWidget extends Widget {
            LootPoolWidget.ScrollBarWidget.ScrollBarButtonWidget scrollBarButtonWidget;
            int currentMouseY = 0;
            LootPoolWidget parent;

            public ScrollBarWidget(LootPoolWidget parent) {
                super(0, 0, 0, 0);
                this.scrollBarButtonWidget = new LootPoolWidget.ScrollBarWidget.ScrollBarButtonWidget();
                this.parent = parent;
                addChild(scrollBarButtonWidget);
            }

            private void setOffset(int mouseY, int maxOffset, int scrollAreaHeight) {
                float relativeY = mouseY - y - scrollBarButtonWidget.getHeight() / 2f;
                relativeY = Math.max(0, Math.min(relativeY, scrollAreaHeight));

                float scrollPercent = relativeY / scrollAreaHeight;

                parent.targetOffset = scrollPercent * maxOffset;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                currentMouseY = mouseY;

                int scrollAreaHeight = height;

                int buttonHeight;
                if (maxOffset == 0) {
                    buttonHeight = scrollAreaHeight;
                } else {
                    float ratio = scrollAreaHeight / (float) (scrollAreaHeight + maxOffset);
                    buttonHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
                }

                if (scrollBarButtonWidget.isHold) {
                    setOffset((int) (mouseY * ui.getScaleFactor()), (int) maxOffset, scrollAreaHeight - buttonHeight);
                    parent.actualOffset = parent.targetOffset;
                }

                int yPos = maxOffset == 0 ? y : y + (int) ((scrollAreaHeight - buttonHeight) * (parent.actualOffset / (float) maxOffset));

                scrollBarButtonWidget.setBounds((int) (x + width / 2f - 2), yPos, 8, buttonHeight);
            }

            @Override
            protected boolean onClick(int button) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                int buttonHeight = 30;
                int scrollAreaHeight = height - buttonHeight;

                if(scrollBarButtonWidget.isHovered()) scrollBarButtonWidget.isHold = true;
                setOffset((int) ((currentMouseY) * ui.getScaleFactor() + buttonHeight / 2f), (int) maxOffset, scrollAreaHeight);

                return false;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                scrollBarButtonWidget.mouseReleased(mx, my, button);
                return true;
            }

            private static class ScrollBarButtonWidget extends Widget {
                public boolean isHold;

                public ScrollBarButtonWidget() {
                    super(0, 0, 0, 0);
                    isHold = false;
                }

                @Override
                protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                    ui.drawRect(x, y, width, height, UIUtils.getVanillaSeparatorColor(hovered || isHold));
                }

                @Override
                protected boolean onClick(int button) {
                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                    isHold = true;
                    return true;
                }

                @Override
                public boolean mouseReleased(double mx, double my, int button) {
                    isHold = false;
                    return true;
                }
            }
        }
    }

    private static class RefreshButton extends Widget {
        public RefreshButton() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText("Reload lootpools", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            lootPoolWidgets.clear();

            for(Camp camp : Camp.values()) {
                lootPoolWidgets.add(new LootPoolWidget(camp));
            }

            crowdsourcedLootPools.clear();
            lastCrowdsourceFetch.clear();
            fetchRunning.clear();
            hasOldLootpool.clear();

            ResetTimeConfig.INSTANCE.refetch();

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }



    private static class HorizontalScrollBarWidget extends Widget {
        private HorizontalScrollBarWidget.HorizontalScrollBarButtonWidget scrollBarButtonWidget;
        int currentMouseX = 0;

        private final java.util.function.Supplier<Float> getTarget;
        private final java.util.function.Consumer<Float> setTarget;
        private final java.util.function.Supplier<Float> getActual;
        private final java.util.function.Consumer<Float> setActual;
        private final java.util.function.Supplier<Float> getMax;

        public HorizontalScrollBarWidget(
                java.util.function.Supplier<Float> getTarget,
                java.util.function.Consumer<Float> setTarget,
                java.util.function.Supplier<Float> getActual,
                java.util.function.Consumer<Float> setActual,
                java.util.function.Supplier<Float> getMax) {
            super(0, 0, 0, 0);
            this.getTarget = getTarget;
            this.setTarget = setTarget;
            this.getActual = getActual;
            this.setActual = setActual;
            this.getMax = getMax;
            this.scrollBarButtonWidget = new HorizontalScrollBarWidget.HorizontalScrollBarButtonWidget();
            addChild(scrollBarButtonWidget);
        }

        private void setOffset(int mouseX, float maxOffset, int scrollAreaWidth) {
            float relativeX = mouseX - x - scrollBarButtonWidget.getWidth() / 2f;
            relativeX = Math.max(0, Math.min(relativeX, scrollAreaWidth));

            float scrollPercent = relativeX / scrollAreaWidth;
            setTarget.accept(scrollPercent * maxOffset);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            currentMouseX = mouseX;
            ui.drawSliderBackground(x, y, width, height);

            float maxOffset = getMax.get();
            int buttonWidth = maxOffset == 0 ? width : 750;
            int scrollAreaWidth = width - buttonWidth;

            if (scrollBarButtonWidget.isHold) {
                setOffset((int) (mouseX * ui.getScaleFactor()), maxOffset, scrollAreaWidth);
                setActual.accept(getTarget.get());
            }

            int xPos = maxOffset == 0 ? x : (int) (x + scrollAreaWidth * Math.min((getActual.get() / maxOffset), 1));
            scrollBarButtonWidget.setBounds(xPos, y, buttonWidth, height);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            float maxOffset = getMax.get();
            int buttonWidth = Math.max(40, (int) (width * (width / (width + maxOffset))));
            int scrollAreaWidth = width - buttonWidth;

            if (scrollBarButtonWidget.isHovered()) scrollBarButtonWidget.isHold = true;
            setOffset((int) (currentMouseX * ui.getScaleFactor()), maxOffset, scrollAreaWidth);
            return false;
        }

        @Override
        public boolean mouseReleased(double mx, double my, int button) {
            scrollBarButtonWidget.mouseReleased(mx, my, button);
            return true;
        }

        private static class HorizontalScrollBarButtonWidget extends Widget {
            public boolean isHold;

            public HorizontalScrollBarButtonWidget() {
                super(0, 0, 0, 0);
                isHold = false;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                ui.drawButton(x, y, width, height, hovered || isHold);
            }

            @Override
            protected boolean onClick(int button) {
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                isHold = true;
                return true;
            }

            @Override
            public boolean mouseReleased(double mx, double my, int button) {
                isHold = false;
                return true;
            }
        }
    }

    private static boolean shouldFetchLootPool(Camp camp) {
        ZonedDateTime currentReset = ResetTimeConfig.INSTANCE.getCurrentLootrunReset();
        ZonedDateTime lastFetch = lastCrowdsourceFetch.get(camp);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        if(hasOldLootpool.get(camp) != null && hasOldLootpool.get(camp)) return lastFetch.plusSeconds(30).isBefore(now);

        if(lastFetch != null && lastFetch.plusSeconds(30).isAfter(now)) return false;

        return lastFetch == null || currentReset.isAfter(lastFetch);
    }
}
