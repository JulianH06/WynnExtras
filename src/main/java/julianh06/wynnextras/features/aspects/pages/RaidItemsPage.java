package julianh06.wynnextras.features.aspects.pages;

import julianh06.wynnextras.utils.colors.CustomColor;
import julianh06.wynnextras.utils.MinecraftUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.core.ResetTimeConfig;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.aspects.LootrunLootPoolData;
import julianh06.wynnextras.features.crafting.data.WynnDataService;
import julianh06.wynnextras.utils.UI.UIUtils;
import julianh06.wynnextras.utils.UI.Widget;
import julianh06.wynnextras.utils.WynncraftApiHandler;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RaidItemsPage extends PageWidget {
    private enum Raid { NOTG, NOL, TCC, TNA, TWP }

    private static final Map<Raid, String> raidNames = Map.of(
            Raid.NOTG, "Nest of the Grootslangs",
            Raid.NOL, "Orphion's Nexus of Light",
            Raid.TCC, "The Canyon Colossus",
            Raid.TNA, "The Nameless Anomaly",
            Raid.TWP, "The Wartorn Palace"
    );

    private static final Map<Raid, String> raidInternalNames = Map.of(
            Raid.NOTG, "grootslang",
            Raid.NOL, "orphion",
            Raid.TCC, "colossus",
            Raid.TNA, "nameless",
            Raid.TWP, "fruma"
    );

    private static final Map<Raid, Identifier> raidTextures = Map.of(
            Raid.NOTG, Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png"),
            Raid.NOL, Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png"),
            Raid.TCC, Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png"),
            Raid.TNA, Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png"),
            Raid.TWP, Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/twp.png")
    );

    private static final Pattern POWDER_PATTERN = Pattern.compile("^(AIR|EARTH|FIRE|THUNDER|WATER)\\s+([1-6])$");
    private static final Pattern CORKIAN_AMPLIFIER_PATTERN = Pattern.compile("^CORKIAN\\s+AMPLIFIER(?:\\s+TIER)?\\s+(\\d+|IV|III|II|I)$");
    private static final Map<String, String> POWDER_COLORS = Map.of(
            "AIR", "§f",
            "EARTH", "§2",
            "FIRE", "§c",
            "THUNDER", "§e",
            "WATER", "§b"
    );
    private static final String CORKIAN_AMPLIFIER_COLOR = "§b";

    private static final Map<Raid, List<LootrunLootPoolData.LootrunItem>> raidRewards = new java.util.HashMap<>();
    private static boolean fetchStarted = false;
    private static boolean loading = false;
    private static boolean available = false;
    private static final List<RaidItemsWidget> widgets = new ArrayList<>();
    private static List<Text> hoveredTooltip = new ArrayList<>();

    private static float hScrollOffset = 0f;
    private static float hScrollTarget = 0f;
    private static float hScrollMax = 0f;
    private static final int FIXED_WIDGET_WIDTH = 550;
    private static final int MAX_WIDGET_WIDTH = 650;
    private static final int H_WIDGET_SPACING = 40;
    private static HorizontalScrollBarWidget hScrollBarWidget;

    private final RefreshButton refreshButton = new RefreshButton();

    public RaidItemsPage(AspectScreen parent) {
        super(parent);
        ensureWidgets();
        hScrollBarWidget = new HorizontalScrollBarWidget(
                () -> hScrollTarget,
                v -> hScrollTarget = v,
                () -> hScrollOffset,
                v -> hScrollOffset = v,
                () -> hScrollMax
        );
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        hoveredTooltip = new ArrayList<>();
        ensureWidgets();
        fetchOfficialLootPools(false);

        float scaledWidth = width * ui.getScaleFactorF();
        int centerX = (int) (scaledWidth / 2f);
        ui.drawCenteredText("§6§lWeekly Raid Item Lootpools", centerX, 60);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        ZonedDateTime nextReset = ResetTimeConfig.INSTANCE.getNextLootpoolReset();
        if (nextReset.isBefore(now) || nextReset.isEqual(now)) {
            nextReset = nextReset.plusWeeks(1);
        }

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

        if (!available && !loading) {
            ui.drawCenteredText("§eOfficial API data unavailable", centerX, 130, CustomColor.fromHexString("FFFF00"));
        }

        int widgetCount = widgets.size();
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

        float hDiff = hScrollTarget - hScrollOffset;
        if (Math.abs(hDiff) < 0.5f || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) hScrollOffset = hScrollTarget;
        else hScrollOffset += hDiff * 0.3f * tickDelta;

        int widgetY = 175;
        int scrollBarHeight = 30;
        int widgetHeight = (int) (height * ui.getScaleFactorF() * 0.9f - widgetY - (showHorizontalScrollBar ? scrollBarHeight + 5 : 0));

        ctx.enableScissor(
                0,
                0,
                (int) (scaledWidth / ui.getScaleFactor()),
                (int) ((widgetY + widgetHeight) / ui.getScaleFactor())
        );

        int widgetsWidth = widgetCount * widgetWidth + Math.max(0, widgetCount - 1) * H_WIDGET_SPACING;
        int widgetX = showHorizontalScrollBar ? H_WIDGET_SPACING - (int) hScrollOffset : (int) ((scaledWidth - widgetsWidth) / 2f);
        for (RaidItemsWidget widget : getWidgetsSnapshot()) {
            widget.setBounds(widgetX, widgetY, widgetWidth, widgetHeight);
            widget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            widgetX += widgetWidth + H_WIDGET_SPACING;
        }
        ctx.disableScissor();

        if (showHorizontalScrollBar) {
            int scrollBarY = widgetY + widgetHeight + 5;
            hScrollBarWidget.setBounds(40, scrollBarY, (int) scaledWidth - 80, scrollBarHeight);
            hScrollBarWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
        } else {
            hScrollBarWidget.setBounds(0, 0, 0, 0);
        }

        refreshButton.setBounds(0, 0, 300, 60);
        refreshButton.draw(ctx, mouseX, mouseY, tickDelta, ui);
    }

    private static int getTotalContentWidth(int widgetWidth, int widgetCount) {
        if (widgetCount <= 0) return 0;
        return widgetCount * widgetWidth + (widgetCount + 1) * H_WIDGET_SPACING;
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if (hoveredTooltip.isEmpty()) return;
        int absX = (int) (mouseX * parent.getMatrixScale());
        int absY = (int) (mouseY * parent.getMatrixScale());
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(), absX, absY + 20);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (RaidItemsWidget widget : getWidgetsSnapshot()) {
            if (widget.mouseClicked(mx, my, button)) return true;
        }

        if (refreshButton.isHovered()) {
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
        for (RaidItemsWidget widget : getWidgetsSnapshot()) {
            widget.mouseReleased(mx, my, button);
        }

        hScrollBarWidget.scrollBarButtonWidget.isHold = false;
        return false;
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

        for (RaidItemsWidget widget : getWidgetsSnapshot()) {
            if (widget.mouseScrolled(mx, my, delta)) return true;
        }
        return false;
    }

    private static void ensureWidgets() {
        if (!widgets.isEmpty()) return;
        for (Raid raid : Raid.values()) {
            widgets.add(new RaidItemsWidget(raid));
        }
    }

    private static List<RaidItemsWidget> getWidgetsSnapshot() {
        return new ArrayList<>(widgets);
    }

    private static void fetchOfficialLootPools(boolean forceRefresh) {
        if (fetchStarted && !forceRefresh) return;

        fetchStarted = true;
        loading = true;

        WynncraftApiHandler.fetchOfficialLootPools(forceRefresh).thenAccept(result -> {
            Map<Raid, List<LootrunLootPoolData.LootrunItem>> newRaidRewards = new java.util.HashMap<>();

            if (result != null) {
                for (WynncraftApiHandler.ApiLootPool pool : result) {
                    if (!"RAID".equalsIgnoreCase(pool.type)) continue;

                    Raid raid = getRaidByInternalName(pool.internalName);
                    if (raid == null) continue;

                    List<LootrunLootPoolData.LootrunItem> items = pool.rewards.stream()
                            .filter(reward -> !"ASPECT".equalsIgnoreCase(reward.type))
                            .filter(reward -> !isIgnoredRaidReward(reward))
                            .map(LootrunLootPoolPage::toLootrunItem)
                            .toList();
                    newRaidRewards.put(raid, items);
                }
            }

            boolean newAvailable = newRaidRewards.values().stream().anyMatch(items -> !items.isEmpty());
            MinecraftClient.getInstance().execute(() -> {
                raidRewards.clear();
                raidRewards.putAll(newRaidRewards);
                available = newAvailable;
                loading = false;
            });
        });
    }

    private static boolean isIgnoredRaidReward(WynncraftApiHandler.ApiLootPoolReward reward) {
        String cleanName = cleanItemName(reward.name).toLowerCase(Locale.ROOT);
        String compactName = cleanName.replace(" ", "");
        String type = reward.type == null ? "" : reward.type.toUpperCase(Locale.ROOT);

        if (cleanName.equals("ability shard")) return true;
        if (compactName.contains("ingredientbag")) return true;
        if ("INGREDIENT".equals(type)) return true;
        return "CURRENCY".equals(type) && cleanName.contains("emerald");
    }

    private static Raid getRaidByInternalName(String internalName) {
        for (Map.Entry<Raid, String> entry : raidInternalNames.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(internalName)) return entry.getKey();
        }
        return null;
    }

    private static class RaidItemsWidget extends Widget {
        private final Raid raid;
        private final ScrollBarWidget scrollBarWidget;
        private float targetOffset = 0;
        private float actualOffset = 0;
        private float maxOffset = 0;

        public RaidItemsWidget(Raid raid) {
            super(0, 0, 0, 0);
            this.raid = raid;
            this.scrollBarWidget = new ScrollBarWidget(this);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawVanillaPanel(x, y, width, height, 12, 17, 17, 100, 18);

            Identifier texture = raidTextures.get(raid);
            if (texture != null) ui.drawImage(texture, x + width / 2f - 45, y - 20, 90, 90);
            ui.drawCenteredText(raidNames.get(raid), x + width / 2f, y + 83, CustomColor.fromHexString("FFFFFF"), 2.8f);

            List<LootrunLootPoolData.LootrunItem> items = raidRewards.getOrDefault(raid, List.of());
            int contentStartY = y + 20;
            int listTop = y + 105;
            int contentHeight = height - 125;

            if (loading && items.isEmpty()) {
                ui.drawCenteredText("§4Loading...", x + width / 2f, contentStartY + 120, CustomColor.fromInt(0xFFFFFF), 3f);
                return;
            }

            if (items.isEmpty()) {
                ui.drawCenteredText("§4No item rewards", x + width / 2f, contentStartY + 120, CustomColor.fromInt(0xFFFFFF), 3f);
                return;
            }

            ctx.enableScissor(
                    (int) ui.sx(x + 6),
                    (int) ui.sy(listTop),
                    (int) ui.sx(x + width - 6),
                    (int) ui.sy(listTop + contentHeight)
            );

            float diff = targetOffset - actualOffset;
            if (Math.abs(diff) < 0.5f || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
            else actualOffset += diff * 0.3f * tickDelta;

            float contentStartTextY = listTop + 25f;
            float textY = contentStartTextY - actualOffset;
            float textX = x + 15;

            textY = drawShinyItems(ctx, textX, textY, items, mouseX, mouseY, listTop, contentHeight);
            textY = drawItemsByRarity(ctx, textX, textY, items, "Mythic", mouseX, mouseY, listTop, contentHeight);
            textY = drawTypeItems(ctx, textX, textY, items, "TOME", mouseX, mouseY, listTop, contentHeight);
            textY = drawTypeItems(ctx, textX, textY, items, "WARD", mouseX, mouseY, listTop, contentHeight);
            textY = drawCorkianAmplifierItems(ctx, textX, textY, items, mouseX, mouseY, listTop, contentHeight);
            textY = drawItemsByRarity(ctx, textX, textY, items, "Fabled", mouseX, mouseY, listTop, contentHeight);
            textY = drawItemsByRarity(ctx, textX, textY, items, "Legendary", mouseX, mouseY, listTop, contentHeight);
            textY = drawItemsByRarity(ctx, textX, textY, items, "Rare", mouseX, mouseY, listTop, contentHeight);
            textY = drawItemsByRarity(ctx, textX, textY, items, "Set", mouseX, mouseY, listTop, contentHeight);
            textY = drawItemsByRarity(ctx, textX, textY, items, "Unique", mouseX, mouseY, listTop, contentHeight);
            textY = drawOtherItems(ctx, textX, textY, items, mouseX, mouseY, listTop, contentHeight);

            float contentEndY = textY + actualOffset;
            maxOffset = Math.max(contentEndY - contentStartTextY - contentHeight + 40, 0);
            if (targetOffset > maxOffset) targetOffset = maxOffset;

            ctx.disableScissor();

            scrollBarWidget.setBounds(x + width - 20, listTop, 15, contentHeight);
            scrollBarWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }

        private float drawShinyItems(DrawContext ctx, float textX, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                     int mouseX, int mouseY, int listTop, int contentHeight) {
            List<LootrunLootPoolData.LootrunItem> filtered = items.stream()
                    .filter(item -> "shiny".equals(item.type))
                    .toList();
            return drawItems(ctx, textX, textY, filtered, mouseX, mouseY, listTop, contentHeight);
        }

        private float drawItemsByRarity(DrawContext ctx, float textX, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                        String rarity, int mouseX, int mouseY, int listTop, int contentHeight) {
            List<LootrunLootPoolData.LootrunItem> filtered = items.stream()
                    .filter(item -> rarity.equals(item.rarity))
                    .filter(item -> !"shiny".equals(item.type))
                    .filter(item -> !isCorkianAmplifier(item))
                    .filter(item -> !"TOME".equalsIgnoreCase(item.rewardType))
                    .filter(item -> !"WARD".equalsIgnoreCase(item.rewardType))
                    .toList();
            return drawItems(ctx, textX, textY, filtered, mouseX, mouseY, listTop, contentHeight);
        }

        private float drawTypeItems(DrawContext ctx, float textX, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                    String type, int mouseX, int mouseY, int listTop, int contentHeight) {
            List<LootrunLootPoolData.LootrunItem> filtered = items.stream()
                    .filter(item -> type.equalsIgnoreCase(item.rewardType))
                    .toList();
            return drawItems(ctx, textX, textY, filtered, mouseX, mouseY, listTop, contentHeight);
        }

        private float drawCorkianAmplifierItems(DrawContext ctx, float textX, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                                int mouseX, int mouseY, int listTop, int contentHeight) {
            List<LootrunLootPoolData.LootrunItem> filtered = items.stream()
                    .filter(RaidItemsPage::isCorkianAmplifier)
                    .toList();
            return drawItems(ctx, textX, textY, filtered, mouseX, mouseY, listTop, contentHeight);
        }

        private float drawOtherItems(DrawContext ctx, float textX, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                     int mouseX, int mouseY, int listTop, int contentHeight) {
            List<LootrunLootPoolData.LootrunItem> filtered = items.stream()
                    .filter(item -> item.rarity == null || item.rarity.isEmpty())
                    .filter(item -> !"shiny".equals(item.type))
                    .filter(item -> !isCorkianAmplifier(item))
                    .filter(item -> !"TOME".equalsIgnoreCase(item.rewardType))
                    .filter(item -> !"WARD".equalsIgnoreCase(item.rewardType))
                    .toList();
            return drawItems(ctx, textX, textY, filtered, mouseX, mouseY, listTop, contentHeight);
        }

        private float drawItems(DrawContext ctx, float textX, float textY, List<LootrunLootPoolData.LootrunItem> items,
                                int mouseX, int mouseY, int listTop, int contentHeight) {
            if (items.isEmpty()) return textY;

            int itemSpacing = 32;
            for (LootrunLootPoolData.LootrunItem item : items) {
                textY = drawItem(ctx, textX, textY, item, mouseX, mouseY, listTop, contentHeight, itemSpacing);
            }

            ui.drawLine(x + 20, textY + 5, x + width - 20, textY + 5, 3, UIUtils.getVanillaDarkSeparatorColor(false));
            return textY + 25;
        }

        private float drawItem(DrawContext ctx, float textX, float textY, LootrunLootPoolData.LootrunItem item,
                               int mouseX, int mouseY, int listTop, int contentHeight, int itemSpacing) {
            if (textY + itemSpacing >= listTop && textY <= listTop + contentHeight) {
                boolean hovering = mouseX * ui.getScaleFactorF() >= textX + 12 && mouseX * ui.getScaleFactorF() <= x + width - 12
                        && mouseY * ui.getScaleFactorF() >= textY && mouseY * ui.getScaleFactorF() <= textY + itemSpacing - 5;

                String rarityColor = colorFor(item);
                String displayName = formatDisplayName(item).replace("Unidentified ", "");
                float textScale = "shiny".equals(item.type) ? 4f : 2.8f;
                String drawName = truncate(displayName, width - 70, textScale);

                if ("shiny".equals(item.type)) {
                    ui.drawText(drawName.replace("⬡ ", ""), textX + 20, textY,
                            WynnExtrasConfig.INSTANCE.removeChroma ? CustomColor.fromHexString("FFFFFF") : CustomColor.RAINBOW, textScale);
                } else {
                    ui.drawText(rarityColor + drawName, textX + 20, textY, CustomColor.fromInt(0xFFFFFF), textScale);
                }

                if (hovering && mouseY * ui.getScaleFactorF() > listTop) {
                    WynnDataService.ItemData apiItem = LootrunLootPoolPage.LootPoolWidget.findApiItem(item);
                    hoveredTooltip = item.tooltip != null && !item.tooltip.isEmpty()
                            ? LootrunLootPoolPage.LootPoolWidget.buildFallbackTooltip(item, rarityColor, displayName)
                            : apiItem == null
                                    ? LootrunLootPoolPage.LootPoolWidget.buildFallbackTooltip(item, rarityColor, displayName)
                                    : LootrunLootPoolPage.LootPoolWidget.buildTooltipFromApi(item, apiItem, rarityColor, displayName);
                }
            }

            return textY + itemSpacing;
        }

        private String colorFor(LootrunLootPoolData.LootrunItem item) {
            String powderElement = powderElement(item.name);
            if (powderElement != null) return POWDER_COLORS.getOrDefault(powderElement, "§f");
            if (isCorkianAmplifier(item)) return CORKIAN_AMPLIFIER_COLOR;
            if ("TOME".equalsIgnoreCase(item.rewardType)) return "§d";
            if ("WARD".equalsIgnoreCase(item.rewardType)) return "§#f9508eff";
            return switch (item.rarity) {
                case "Mythic" -> "§5";
                case "Fabled" -> "§c";
                case "Legendary" -> "§b";
                case "Rare" -> "§d";
                case "Set" -> "§a";
                case "Unique" -> "§e";
                default -> "§f";
            };
        }

        private String formatDisplayName(LootrunLootPoolData.LootrunItem item) {
            String name = formatRewardName(item.name);
            if (item.always) name += " §7(always)";
            return name;
        }

        private String truncate(String text, int maxWidth, float textScale) {
            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;
            int scaledMaxWidth = Math.max(0, (int) (maxWidth / textScale));
            if (renderer.getWidth(text) <= scaledMaxWidth) return text;
            return renderer.trimToWidth(text, Math.max(0, scaledMaxWidth - renderer.getWidth("..."))) + "...";
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (scrollBarWidget.isHovered()) {
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
            if (!hovered) return false;
            if (delta > 0) targetOffset -= 33f;
            else targetOffset += 33f;
            if (targetOffset < 0) targetOffset = 0;
            if (targetOffset > maxOffset) targetOffset = maxOffset;
            return true;
        }

        private static class ScrollBarWidget extends Widget {
            ScrollBarButtonWidget scrollBarButtonWidget;
            int currentMouseY = 0;
            RaidItemsWidget parent;

            public ScrollBarWidget(RaidItemsWidget parent) {
                super(0, 0, 0, 0);
                this.scrollBarButtonWidget = new ScrollBarButtonWidget();
                this.parent = parent;
                addChild(scrollBarButtonWidget);
            }

            private void setOffset(int mouseY, int maxOffset, int scrollAreaHeight) {
                float relativeY = mouseY - y - scrollBarButtonWidget.getHeight() / 2f;
                relativeY = Math.clamp(relativeY, 0, scrollAreaHeight);
                parent.targetOffset = (relativeY / scrollAreaHeight) * maxOffset;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                currentMouseY = mouseY;
                int scrollAreaHeight = height;
                int buttonHeight;
                if (parent.maxOffset == 0) {
                    buttonHeight = scrollAreaHeight;
                } else {
                    float ratio = scrollAreaHeight / (float) (scrollAreaHeight + parent.maxOffset);
                    buttonHeight = Math.max(20, (int) (scrollAreaHeight * ratio));
                }

                if (scrollBarButtonWidget.isHold) {
                    setOffset((int) (mouseY * ui.getScaleFactor()), (int) parent.maxOffset, scrollAreaHeight - buttonHeight);
                    parent.actualOffset = parent.targetOffset;
                }

                int yPos = parent.maxOffset == 0 ? y : y + (int) ((scrollAreaHeight - buttonHeight) * (parent.actualOffset / parent.maxOffset));
                scrollBarButtonWidget.setBounds((int) (x + width / 2f - 2), yPos, 8, buttonHeight);
            }

            @Override
            protected boolean onClick(int button) {
                MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                int buttonHeight = 30;
                int scrollAreaHeight = height - buttonHeight;

                if (scrollBarButtonWidget.isHovered()) scrollBarButtonWidget.isHold = true;
                setOffset((int) (currentMouseY * ui.getScaleFactor() + buttonHeight / 2f), (int) parent.maxOffset, scrollAreaHeight);
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

    private static String cleanItemName(String itemName) {
        if (itemName == null) return "";
        return itemName
                .replaceAll("§.", "")
                .replace("Unidentified ", "")
                .replace("⬡ ", "")
                .replace("Shiny ", "")
                .trim();
    }

    private static String powderElement(String itemName) {
        Matcher matcher = POWDER_PATTERN.matcher(cleanItemName(itemName).toUpperCase(Locale.ROOT));
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static String formatPowderName(String itemName) {
        String cleanName = cleanItemName(itemName);
        Matcher matcher = POWDER_PATTERN.matcher(cleanName.toUpperCase(Locale.ROOT));
        if (!matcher.matches()) return itemName;

        String element = matcher.group(1).toLowerCase(Locale.ROOT);
        element = element.substring(0, 1).toUpperCase(Locale.ROOT) + element.substring(1);
        return element + " powder " + toRoman(Integer.parseInt(matcher.group(2)));
    }

    private static String formatRewardName(String itemName) {
        if (isCorkianAmplifier(itemName)) return formatCorkianAmplifierName(itemName);
        return formatPowderName(itemName);
    }

    private static boolean isCorkianAmplifier(LootrunLootPoolData.LootrunItem item) {
        return item != null && isCorkianAmplifier(item.name);
    }

    private static boolean isCorkianAmplifier(String itemName) {
        return cleanItemName(itemName).toUpperCase(Locale.ROOT).startsWith("CORKIAN AMPLIFIER");
    }

    private static String formatCorkianAmplifierName(String itemName) {
        String cleanName = cleanItemName(itemName);
        Matcher matcher = CORKIAN_AMPLIFIER_PATTERN.matcher(cleanName.toUpperCase(Locale.ROOT));
        if (!matcher.matches()) return "Corkian Amplifier";

        String tier = matcher.group(1);
        if (tier.matches("\\d+")) tier = toRoman(Integer.parseInt(tier));
        return "Corkian Amplifier " + tier;
    }

    private static String toRoman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            default -> String.valueOf(value);
        };
    }

    private static class RefreshButton extends Widget {
        public RefreshButton() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, hovered);
            ui.drawCenteredText("Reload raid items", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            raidRewards.clear();
            fetchStarted = false;
            loading = false;
            available = false;
            WynncraftApiHandler.clearOfficialLootPoolsCache();
            MinecraftUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }

    private static class HorizontalScrollBarWidget extends Widget {
        private final HorizontalScrollBarButtonWidget scrollBarButtonWidget;
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
            this.scrollBarButtonWidget = new HorizontalScrollBarButtonWidget();
            addChild(scrollBarButtonWidget);
        }

        private void setOffset(int mouseX, float maxOffset, int scrollAreaWidth) {
            if (maxOffset <= 0 || scrollAreaWidth <= 0) {
                setTarget.accept(0f);
                return;
            }
            float relativeX = mouseX - x - scrollBarButtonWidget.getWidth() / 2f;
            relativeX = Math.max(0, Math.min(relativeX, scrollAreaWidth));
            setTarget.accept((relativeX / scrollAreaWidth) * maxOffset);
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