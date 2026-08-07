package julianh06.wynnextras.features.aspects.pages;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.MinecraftUtils;
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
    private static final Map<String, List<LootrunLootPoolData.LootrunItem>> officialLootPools = new HashMap<>();
    private static boolean officialLootPoolsFetchStarted = false;
    private static boolean officialLootPoolsLoading = false;
    private static final Set<String> CORKIAN_MYTHIC_NAMES = Set.of("CORKIAN SIMULATOR", "CORKIAN INSULATOR");

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

    private static float hScrollOffset = 0f;
    private static float hScrollTarget = 0f;
    private static float hScrollMax = 0f;
    private static final int FIXED_WIDGET_WIDTH = 550;
    private static final int MAX_WIDGET_WIDTH = 650;
    private static final int H_WIDGET_SPACING = 40;
    private static HorizontalScrollBarWidget hScrollBarWidget;

    public LootrunLootPoolPage(AspectScreen parent) {
        super(parent);

        if (lootPoolWidgets.isEmpty()) resetLootPoolWidgetsToDefaults();

        refreshButton = new RefreshButton();

        hScrollBarWidget = new HorizontalScrollBarWidget(
                () -> hScrollTarget,
                v -> hScrollTarget = v,
                () -> hScrollOffset,
                v -> hScrollOffset = v,
                () -> hScrollMax
        );
    }

    private static void resetLootPoolWidgetsToDefaults() {
        lootPoolWidgets.clear();
        for(Camp camp : Camp.values()) {
            lootPoolWidgets.add(new LootPoolWidget(camp));
        }
    }

    private static List<LootPoolWidget> getLootPoolWidgetsSnapshot() {
        return new ArrayList<>(lootPoolWidgets);
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        hoveredTooltip = new ArrayList<>();

        float scaleFactor = ui.getScaleFactorF();
        int logicalW = (int) (width * scaleFactor);
        int centerX = logicalW / 2;

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        fetchOfficialLootPools(false);

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
        int widgetCount = lootPoolWidgets.size();
        int minContentWidth = getTotalContentWidth(FIXED_WIDGET_WIDTH, widgetCount);
        boolean showHorizontalScrollBar = widgetCount > 0 && minContentWidth > scaledWidth;
        int widgetWidth = FIXED_WIDGET_WIDTH;
        if (!showHorizontalScrollBar && widgetCount > 0) {
            widgetWidth = Math.min(MAX_WIDGET_WIDTH, Math.max(FIXED_WIDGET_WIDTH, (int) ((scaledWidth - (widgetCount + 1) * H_WIDGET_SPACING) / widgetCount)));
        }

        hScrollMax = showHorizontalScrollBar ? Math.max(0, minContentWidth - scaledWidth) : 0;

        if (hScrollTarget > hScrollMax) hScrollTarget = hScrollMax;
        if (hScrollMax == 0) {
            hScrollTarget = 0;
            hScrollOffset = 0;
        }

        float snapValue = 0.5f;
        float speed = 0.3f;
        float hDiff = hScrollTarget - hScrollOffset;
        if (Math.abs(hDiff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) hScrollOffset = hScrollTarget;
        else hScrollOffset += hDiff * speed * tickDelta;

        int widgetY = 175;
        int scrollBarHeight = 30;
        int widgetHeight = (int) (height * ui.getScaleFactorF() * 0.9f - widgetY - (showHorizontalScrollBar ? scrollBarHeight + 5 : 0));

        context.enableScissor(
                0,
                0,
                (int) (scaledWidth / ui.getScaleFactor()),
                (int) ((widgetY + widgetHeight) / ui.getScaleFactor())
        );

        int widgetsWidth = widgetCount * widgetWidth + Math.max(0, widgetCount - 1) * H_WIDGET_SPACING;
        int widgetX = showHorizontalScrollBar ? H_WIDGET_SPACING - (int) hScrollOffset : (int) ((scaledWidth - widgetsWidth) / 2f);
        for (LootPoolWidget lootPoolWidget : getLootPoolWidgetsSnapshot()) {
            lootPoolWidget.setBounds(widgetX, widgetY, widgetWidth, widgetHeight);
            lootPoolWidget.draw(context, mouseX, mouseY, tickDelta, ui);
            widgetX += widgetWidth + H_WIDGET_SPACING;
        }
        context.disableScissor();

        if (showHorizontalScrollBar) {
            int scrollBarY = widgetY + widgetHeight + 5;
            hScrollBarWidget.setBounds(40, scrollBarY, (int) scaledWidth - 80, scrollBarHeight);
            hScrollBarWidget.draw(context, mouseX, mouseY, tickDelta, ui);
        } else {
            hScrollBarWidget.setBounds(0, 0, 0, 0);
        }

        refreshButton.setBounds(0, 0, 300, 60);
        refreshButton.draw(context, mouseX, mouseY, tickDelta, ui);
    }

    private static int getTotalContentWidth(int widgetWidth, int widgetCount) {
        if (widgetCount <= 0) return 0;
        return widgetCount * widgetWidth + (widgetCount + 1) * H_WIDGET_SPACING;
    }

    private static void fetchOfficialLootPools(boolean forceRefresh) {
        if (officialLootPoolsFetchStarted && !forceRefresh) return;

        officialLootPoolsFetchStarted = true;
        officialLootPoolsLoading = true;

        WynncraftApiHandler.fetchOfficialLootPools(forceRefresh).thenAccept(result -> {
            Map<String, List<LootrunLootPoolData.LootrunItem>> newOfficialLootPools = new HashMap<>();
            List<LootPoolWidget> newLootPoolWidgets = new ArrayList<>();
            if (result != null) {
                List<WynncraftApiHandler.ApiLootPool> camps = result.stream()
                        .filter(pool -> "CAMP".equalsIgnoreCase(pool.type))
                        .toList();

                if (!camps.isEmpty()) {
                    for (WynncraftApiHandler.ApiLootPool camp : camps) {
                        newOfficialLootPools.put(camp.internalName, toLootrunItems(camp.rewards));
                        newLootPoolWidgets.add(new LootPoolWidget(camp.name, camp.internalName));
                    }
                }
            }

            MinecraftClient.getInstance().execute(() -> {
                officialLootPools.clear();
                officialLootPools.putAll(newOfficialLootPools);

                if (!newLootPoolWidgets.isEmpty()) {
                    lootPoolWidgets.clear();
                    lootPoolWidgets.addAll(newLootPoolWidgets);
                }

                officialLootPoolsLoading = false;
            });
        });
    }

    static List<LootrunLootPoolData.LootrunItem> toLootrunItems(List<WynncraftApiHandler.ApiLootPoolReward> rewards) {
        List<LootrunLootPoolData.LootrunItem> items = new ArrayList<>();
        for (WynncraftApiHandler.ApiLootPoolReward reward : rewards) {
            items.add(toLootrunItem(reward));
        }
        return items;
    }

    static LootrunLootPoolData.LootrunItem toLootrunItem(WynncraftApiHandler.ApiLootPoolReward reward) {
        String name = normalizeRewardName(reward.name);
        String rarity = isCorkianMythic(reward.name) ? "Mythic" : capitalize(reward.tier);
        String type = switch (reward.type == null ? "" : reward.type.toUpperCase(Locale.ROOT)) {
            case "TOME" -> "tome";
            case "WARD" -> "ward";
            case "CURRENCY" -> "currency";
            case "INGREDIENT" -> "ingredient";
            default -> reward.shiny ? "shiny" : "normal";
        };
        return new LootrunLootPoolData.LootrunItem(
                name,
                rarity,
                type,
                reward.tooltip,
                "",
                reward.amount,
                reward.type,
                reward.always,
                reward.shiny
        );
    }

    private static boolean isCorkianMythic(String name) {
        if (name == null) return false;
        return CORKIAN_MYTHIC_NAMES.contains(cleanRewardName(name).toUpperCase(Locale.ROOT));
    }

    private static String normalizeRewardName(String name) {
        if (!isCorkianMythic(name)) return name;
        String cleanName = cleanRewardName(name).toLowerCase(Locale.ROOT);
        return Arrays.stream(cleanName.split(" "))
                .filter(part -> !part.isBlank())
                .map(LootrunLootPoolPage::capitalize)
                .collect(Collectors.joining(" "));
    }

    private static String cleanRewardName(String name) {
        return name == null ? "" : name.replaceAll("§.", "").trim();
    }

    static String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
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

        if (shiftHeld && hScrollMax > 0) {
            if (delta > 0) hScrollTarget -= 60f;
            else hScrollTarget += 60f;
            if (hScrollTarget < 0) hScrollTarget = 0;
            if (hScrollTarget > hScrollMax) hScrollTarget = hScrollMax;
            return true;
        }

        for (LootPoolWidget lootPoolWidget : getLootPoolWidgetsSnapshot()) {
            if (lootPoolWidget.mouseScrolled(mx, my, delta)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for(LootPoolWidget lootPoolWidget : getLootPoolWidgetsSnapshot()) {
            if(lootPoolWidget.mouseClicked(mx, my, button)) return true;
        }

        if(refreshButton.isHovered()) {
            refreshButton.onClick(button);
            return true;
        }

        if (hScrollMax > 0 && hScrollBarWidget.isHovered()) {
            hScrollBarWidget.onClick(button);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for(LootPoolWidget lootPoolWidget : getLootPoolWidgetsSnapshot()) {
            lootPoolWidget.mouseReleased(mx, my, button);
        }

        hScrollBarWidget.scrollBarButtonWidget.isHold = false;
        return false;
    }

    static class LootPoolWidget extends Widget {
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
        final String title;
        final String lootPoolKey;
        float targetOffset = 0;
        float actualOffset = 0;
        float maxOffset = 999;
        int textureWidth = 150;

        public LootPoolWidget(Camp camp) {
            super(0, 0, 0, 0);
            scrollBarWidget = new LootPoolWidget.ScrollBarWidget(this);
            this.camp = camp;
            this.title = campNames[camp.ordinal()];
            this.lootPoolKey = camp.name();
        }

        public LootPoolWidget(String title, String lootPoolKey) {
            super(0, 0, 0, 0);
            scrollBarWidget = new LootPoolWidget.ScrollBarWidget(this);
            this.camp = null;
            this.title = title;
            this.lootPoolKey = lootPoolKey;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int topHeight = 94;

            ui.drawVanillaPanel(x, y, width, height, 12, 17, 17, 80, 21);

            float titleScale = getFittingTextScale(title, width - 45, 3f, 2.2f);
            ui.drawCenteredText(title, x + width / 2f, y + 45, CustomColor.fromHexString("FFFFFF"), titleScale);

            List<LootrunLootPoolData.LootrunItem> items = getLootPool();

            ctx.enableScissor(
                    (int) (x / ui.getScaleFactor()),
                    (int) ((y + 85) / ui.getScaleFactor()),
                    (int) ((x + width - 7) / ui.getScaleFactor()),
                    (int) ((y + height - 20) / ui.getScaleFactor()));

            int contentStartY = y + 20;
            int contentHeight = height - 40;
            int totalContentHeight = 0;

            if (items.isEmpty()) {
                if (officialLootPoolsLoading) {
                    ui.drawCenteredText("§4Loading...", x + width / 2f, contentStartY + 90, CustomColor.fromInt(0xFFFFFF), 3f);
                } else {
                    ui.drawCenteredText("§4No data", x + width / 2f, contentStartY + 90, CustomColor.fromInt(0xFFFFFF), 3f);
                    ui.drawCenteredText("§7Official API unavailable", x + width / 2f, contentStartY + 120, CustomColor.fromInt(0xFFFFFF), 2.5f);
                }
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
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, UIUtils.getVanillaDarkSeparatorColor(false));
                textY = drawOtherItems(ctx, textX, textY, items, width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);

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

        private float drawOtherItems(DrawContext context, float x, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                     float colWidth, float mouseX, float mouseY, float contentStartY, float contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> otherItems = items.stream()
                    .filter(i -> i.rarity == null || i.rarity.isEmpty())
                    .filter(i -> !i.name.contains("Ward"))
                    .filter(i -> !i.type.equals("tome"))
                    .filter(i -> !i.type.equals("shiny"))
                    .toList();

            if (otherItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : otherItems) {
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
                String displayName = truncate(formatDisplayName(item), width / 2 - 30).replace("Unidentified ", "");

                if (item.type.equals("shiny")) {
                    ui.drawText(displayName.replace("⬡ ", ""), x + 20, textY, WynnExtrasConfig.INSTANCE.removeChroma ? CustomColor.fromHexString("FFFFFF") : CustomColor.RAINBOW, 4f);
                } else {
                    ui.drawText(rarityColor + displayName, x + 20, textY, CustomColor.fromInt(0xFFFFFF), 2.8f);
                }
                boolean isShiny = item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty();
                if (isShiny) {
                    ui.drawText("§7" + item.shinyStat.replace(": §f0", ""), x + 20, textY + 45, CustomColor.fromInt(0xFFFFFF), 2.2f);
                }

                if (hovering && mouseY * ui.getScaleFactorF() > y + 80) {
                    JsonObject jsonItem = findApiItem(item.name);
                    hoveredTooltip = item.tooltip != null && !item.tooltip.isEmpty()
                            ? buildFallbackTooltip(item, rarityColor, displayName)
                            : jsonItem == null
                                    ? buildFallbackTooltip(item, rarityColor, displayName)
                                    : buildTooltipFromApi(item, jsonItem, rarityColor, displayName);
                }
            }
            int extraSpacing = (item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty()) ? 40 : 0;
            return textY + itemSpacing + extraSpacing;
        }

        private float getFittingTextScale(String text, float maxWidth, float defaultScale, float minScale) {
            int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
            if (textWidth == 0) return defaultScale;
            return Math.max(minScale, Math.min(defaultScale, maxWidth / textWidth));
        }

        private String formatDisplayName(LootrunLootPoolData.LootrunItem item) {
            String name = item.name;
            if (item.always) name += " §7(always)";
            return name;
        }

        static JsonObject findApiItem(String itemName) {
            Map<String, JsonObject> database = WynncraftApiHandler.getCachedItemDatabase();
            if (database == null) return null;

            String cleanName = cleanItemName(itemName);
            JsonObject direct = database.get(cleanName);
            if (direct != null) return direct;

            for (JsonObject candidate : database.values()) {
                if (jsonString(candidate, "displayName").equalsIgnoreCase(cleanName)
                        || jsonString(candidate, "internalName").equalsIgnoreCase(cleanName)) {
                    return candidate;
                }
            }
            return null;
        }

        private static String cleanItemName(String itemName) {
            return itemName
                    .replace("Unidentified ", "")
                    .replace("⬡ ", "")
                    .replace("Shiny ", "")
                    .trim();
        }

        static List<Text> buildTooltipFromApi(LootrunLootPoolData.LootrunItem lootrunItem, JsonObject apiItem,
                                                      String rarityColor, String displayName) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(coloredName(displayName.replace("⬡ ", ""), rarityColor));

            String tier = capitalize(jsonString(apiItem, "tier"));
            String subType = formatCamelName(jsonString(apiItem, "subType"));
            if (!tier.isEmpty() || !subType.isEmpty()) {
                tooltip.add(Text.of("§7" + (tier + " " + subType).trim()));
            }

            String attackSpeed = formatCamelName(jsonString(apiItem, "attackSpeed"));
            if (!attackSpeed.isEmpty()) {
                tooltip.add(Text.of("§7" + attackSpeed + " Attack Speed"));
            }

            addBaseStats(tooltip, apiItem);
            addRequirements(tooltip, apiItem);
            addIdentifications(tooltip, apiItem);

            if (apiItem.has("powderSlots")) {
                tooltip.add(Text.of("§7Powder Slots: §f" + apiItem.get("powderSlots").getAsInt()));
            }

            if (lootrunItem.shinyStat != null && !lootrunItem.shinyStat.isEmpty()) {
                tooltip.add(Text.of("§7" + lootrunItem.shinyStat.replace(": §f0", "")));
            }

            return tooltip;
        }

        static List<Text> buildFallbackTooltip(LootrunLootPoolData.LootrunItem item, String rarityColor, String displayName) {
            List<Text> tooltip = new ArrayList<>();
            tooltip.add(coloredName(displayName.replace("⬡ ", ""), rarityColor));

            if (item.tooltip != null && !item.tooltip.isEmpty()) {
                for (String line : item.tooltip.split("\\R")) {
                    if (!line.isBlank()) tooltip.add(Text.of("§7" + line));
                }
            }

            if (item.shinyStat != null && !item.shinyStat.isEmpty()) {
                tooltip.add(Text.of("§7" + item.shinyStat.replace(": §f0", "")));
            }

            return tooltip;
        }

        private static Text coloredName(String name, String rarityColor) {
            if(rarityColor.startsWith("§#")) {
                String hex = rarityColor.substring(2);
                int r = Integer.parseInt(hex.substring(0, 2), 16);
                int g = Integer.parseInt(hex.substring(2, 4), 16);
                int b = Integer.parseInt(hex.substring(4, 6), 16);

                return Text.literal(name)
                        .styled(style -> style.withColor(net.minecraft.util.math.ColorHelper.getArgb(255, r, g, b)));
            }
            return Text.of(rarityColor + name);
        }

        private static void addBaseStats(List<Text> tooltip, JsonObject item) {
            JsonObject base = item.getAsJsonObject("base");
            if (base == null || base.isEmpty()) return;

            tooltip.add(Text.empty());

            addBaseLine(tooltip, base, "baseHealth", "§cHealth");
            addBaseLine(tooltip, base, "baseDamage", "§6✣ Neutral Damage");
            addBaseLine(tooltip, base, "baseEarthDamage", "§2✤ Earth Damage");
            addBaseLine(tooltip, base, "baseThunderDamage", "§e✦ Thunder Damage");
            addBaseLine(tooltip, base, "baseWaterDamage", "§b❉ Water Damage");
            addBaseLine(tooltip, base, "baseFireDamage", "§c✹ Fire Damage");
            addBaseLine(tooltip, base, "baseAirDamage", "§f❋ Air Damage");
            addBaseLine(tooltip, base, "baseEarthDefence", "§2✤ Earth Defence");
            addBaseLine(tooltip, base, "baseThunderDefence", "§e✦ Thunder Defence");
            addBaseLine(tooltip, base, "baseWaterDefence", "§b❉ Water Defence");
            addBaseLine(tooltip, base, "baseFireDefence", "§c✹ Fire Defence");
            addBaseLine(tooltip, base, "baseAirDefence", "§f❋ Air Defence");
        }

        private static void addBaseLine(List<Text> tooltip, JsonObject base, String key, String label) {
            if (!base.has(key) || base.get(key).isJsonNull()) return;
            tooltip.add(Text.of(label + ": §f" + formatJsonValue(base.get(key), false)));
        }

        private static void addRequirements(List<Text> tooltip, JsonObject item) {
            JsonObject requirements = item.getAsJsonObject("requirements");
            if (requirements == null || requirements.isEmpty()) return;

            tooltip.add(Text.empty());

            addRequirementLine(tooltip, requirements, "level", "Combat Lv. Min");
            addRequirementLine(tooltip, requirements, "classRequirement", "Class Req");
            addRequirementLine(tooltip, requirements, "strength", "Strength Min");
            addRequirementLine(tooltip, requirements, "dexterity", "Dexterity Min");
            addRequirementLine(tooltip, requirements, "intelligence", "Intelligence Min");
            addRequirementLine(tooltip, requirements, "defence", "Defence Min");
            addRequirementLine(tooltip, requirements, "agility", "Agility Min");
        }

        private static void addRequirementLine(List<Text> tooltip, JsonObject requirements, String key, String label) {
            if (!requirements.has(key) || requirements.get(key).isJsonNull()) return;
            String value = requirements.get(key).getAsString();
            if (key.equals("classRequirement")) value = capitalize(value);
            tooltip.add(Text.of("§7" + label + ": §f" + value));
        }

        private static void addIdentifications(List<Text> tooltip, JsonObject item) {
            JsonObject ids = item.getAsJsonObject("identifications");
            if (ids == null || ids.isEmpty()) return;

            tooltip.add(Text.empty());

            for (Map.Entry<String, JsonElement> entry : ids.entrySet()) {
                String value = formatJsonValue(entry.getValue(), isPercentIdentification(entry.getKey()));
                String color = value.startsWith("-") ? "§c" : "§a";
                tooltip.add(Text.of(color + value + " §7" + formatLine(entry.getKey())));
            }
        }

        private static String formatJsonValue(JsonElement value, boolean percent) {
            String suffix = percent ? "%" : "";
            if (value == null || value.isJsonNull()) return "";

            if (value.isJsonPrimitive()) {
                return formatNumber(value.getAsInt()) + suffix;
            }

            JsonObject range = value.getAsJsonObject();
            if (range.has("min") && range.has("max")) {
                return formatNumber(range.get("min").getAsInt()) + suffix + " to "
                        + formatNumber(range.get("max").getAsInt()) + suffix;
            }

            if (range.has("raw")) {
                return formatNumber(range.get("raw").getAsInt()) + suffix;
            }

            return "";
        }

        private static String formatNumber(int value) {
            return value > 0 ? "+" + value : String.valueOf(value);
        }

        private static boolean isPercentIdentification(String key) {
            return !key.startsWith("raw")
                    && !key.equals("manaSteal")
                    && !key.equals("lifeSteal")
                    && !key.equals("healthRegenRaw")
                    && !key.equals("poison");
        }

        private static String jsonString(JsonObject object, String key) {
            if (object == null || !object.has(key) || object.get(key).isJsonNull()) return "";
            return object.get(key).getAsString();
        }

        private static String formatCamelName(String value) {
            if (value == null || value.isEmpty()) return "";
            String withSpaces = value.replaceAll("([a-z])([A-Z])", "$1 $2")
                    .replace("_", " ")
                    .replace("-", " ");
            return Arrays.stream(withSpaces.split(" "))
                    .filter(part -> !part.isBlank())
                    .map(LootPoolWidget::capitalize)
                    .collect(Collectors.joining(" "));
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

        private static String capitalize(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }

        private List<LootrunLootPoolData.LootrunItem> getLootPool() {
            if (officialLootPools.containsKey(lootPoolKey) && officialLootPools.get(lootPoolKey) != null) {
                List<LootrunLootPoolData.LootrunItem> items = officialLootPools.get(lootPoolKey);
                if (!items.isEmpty()) return items;
            }

            return new ArrayList<>();
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
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                    MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
            resetLootPoolWidgetsToDefaults();

            officialLootPools.clear();
            officialLootPoolsFetchStarted = false;
            officialLootPoolsLoading = false;
            WynncraftApiHandler.clearOfficialLootPoolsCache();

            ResetTimeConfig.INSTANCE.refetch();

            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
            if (maxOffset <= 0 || scrollAreaWidth <= 0) {
                setTarget.accept(0f);
                return;
            }
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
            int buttonWidth = maxOffset == 0 ? width : Math.max(40, (int) (width * (width / (width + maxOffset))));
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
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
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