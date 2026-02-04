package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.aspects.LootrunLootPoolData;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class LootrunLootPoolPage extends PageWidget {
    private static Map<String, List<LootrunLootPoolData.LootrunItem>> crowdsourcedLootPools = new HashMap<>();
    private boolean fetchedCrowdsourcedLootPools = false;

    private enum Camp { SI, SE, CORKUS, COTL, MH }

    private static String[] campNames = {
            "Sky Islands",
            "Silent Expanse",
            "Corkus Traversal",
            "Canyon of the Lost",
            "Molten Heights"
    };

    private int[] campScrollOffsets = new int[5];
    private static int[] campContentHeights = new int[5];
    private int[] campColumnX = new int[5];
    private int[] campColumnWidth = new int[5];

    static List<LootPoolWidget> lootPoolWidgets = new ArrayList<>();

    private static List<Text> hoveredTooltip = new ArrayList<>();

    private static LootrunLootPoolData.LootrunItem hoveredItem = null;

    public LootrunLootPoolPage(AspectScreen parent) {
        super(parent);

        for(Camp camp : Camp.values()) {
            lootPoolWidgets.add(new LootPoolWidget(camp));
        }
    }

    @Override
    protected void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        hoveredItem = null;

        float scaleFactor = ui.getScaleFactorF();
        int logicalW = (int) (width * scaleFactor);
        int logicalH = (int) (height * scaleFactor);
        int centerX = logicalW / 2;

        int logicalMouseX = (int) (mouseX * scaleFactor);
        int logicalMouseY = (int) (mouseY * scaleFactor);

        if (!fetchedCrowdsourcedLootPools) {
            fetchedCrowdsourcedLootPools = true;
            for (String camp : LootrunLootPoolData.CAMP_CODES) {
                WynncraftApiHandler.fetchCrowdsourcedLootrunLootPool(camp).thenAccept(result -> {
                    if (result != null && !result.isEmpty()) {
                        crowdsourcedLootPools.put(camp, result);
                        LootrunLootPoolData.INSTANCE.saveLootPool(camp, result);
                    }
                });
            }
        }

        ui.drawCenteredText("§6§lWeekly Lootrun Lootpools", centerX, 60, CustomColor.fromInt(0xFFFFFF), 3f);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        ZonedDateTime nextReset = now.with(java.time.DayOfWeek.FRIDAY).withHour(20).withMinute(0).withSecond(0).withNano(0);
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

        int spacing = 40;
        int widgetX = spacing;
        int widgetY = 175;
        int widgets = lootPoolWidgets.size();
        int totalSpacing = spacing * (widgets + 1);
        float scaledWidth = width * ui.getScaleFactorF();
        int widgetWidth = (int) ((scaledWidth - totalSpacing) / widgets);

        int widgetHeight = (int) (height * ui.getScaleFactorF() * 0.9f - widgetY);

        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            lootPoolWidget.setBounds(widgetX, widgetY, widgetWidth, widgetHeight);
            lootPoolWidget.draw(context, mouseX, mouseY, tickDelta, ui);
            widgetX += widgetWidth + spacing;
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(hoveredTooltip.isEmpty()) return;
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(), mouseX - 5, mouseY + 20);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        float scaleFactor = ui.getScaleFactorF();
        int logicalMouseX = (int) (mx * scaleFactor);

        for (int i = 0; i < 5; i++) {
            if (campColumnX[i] > 0 && campColumnWidth[i] > 0) {
                if (logicalMouseX >= campColumnX[i] && logicalMouseX <= campColumnX[i] + campColumnWidth[i]) {
                    int scrollAmount = (int) (-delta * 40);
                    campScrollOffsets[i] += scrollAmount;

                    if (campScrollOffsets[i] < 0) campScrollOffsets[i] = 0;
                    int contentHeight = (int) (height * scaleFactor) - 150 - 150 - 110 - 12;
                    int maxScroll = 10000; // Math.max(0, campContentHeights[i] - contentHeight);
                    if (campScrollOffsets[i] > maxScroll) campScrollOffsets[i] = maxScroll;
                    return true;
                }
            }
        }
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
            int topHeight = height - scrollBarWidget.getHeight() + 14;

            if(WynnExtrasConfig.INSTANCE.darkmodeToggle) {
                ui.drawNineSlice((int) (x),
                        (int) (y), width,
                        (int) (topHeight), 33, ltopd, rtopd, ttopd, btopd, tltopd, trtopd, bltopd, brtopd, CustomColor.fromHexString("2c2d2f"));

                ui.drawNineSlice((int) (x),
                        (int) (y + topHeight), width,
                        (int) (height - topHeight), 33, ld, rd, td, bd, tld, trd, bld, brd, CustomColor.fromHexString("444448"));
            } else {
                ui.drawNineSlice((int) (x),
                        (int) (y), width,
                        (int) (topHeight), 33, ltop, rtop, ttop, btop, tltop, trtop, bltop, brtop, CustomColor.fromHexString("81644b"));

                ui.drawNineSlice((int) (x),
                        (int) (y + topHeight), width,
                        (int) (height - topHeight), 33, l, r, t, b, tl, tr, bl, br, CustomColor.fromHexString("cca76f"));
            }

            ui.drawCenteredText(campNames[camp.ordinal()], x + width / 2f, y + 45, CustomColor.fromHexString("FFFFFF"));


            List<LootrunLootPoolData.LootrunItem> items = getLootPoolForCamp(camp.name());

            ctx.enableScissor(
                    (int) (x / ui.getScaleFactor()),
                    (int) ((y + 195) / ui.getScaleFactor()),
                    (int) ((x + width - 7) / ui.getScaleFactor()),
                    (int) ((y + height - 20) / ui.getScaleFactor()));

            int contentStartY = y + 110;
            int contentHeight = height - 110 - 12;
            float scrollOffset = actualOffset;

            if (items.isEmpty()) {
                ui.drawCenteredText("§7No data", x + width / 2f, contentStartY + 40, CustomColor.fromInt(0xFFFFFF), 3f);
                ui.drawCenteredText("§7Open lootrun", x + width / 2f, contentStartY + 80, CustomColor.fromInt(0xFFFFFF), 2.5f);
                ui.drawCenteredText("§7chest to scan", x + width / 2f, contentStartY + 110, CustomColor.fromInt(0xFFFFFF), 2.5f);
            } else {
                int itemSpacing = 32;
                int totalContentHeight = items.size() * itemSpacing + 20;

                ctx.enableScissor(
                        (int) ui.sx(x + 6),
                        (int) ui.sy(contentStartY),
                        (int) ui.sx(x + width - 6),
                        (int) ui.sy(contentStartY + contentHeight)
                );

                int textY = (int) (contentStartY + 10 - scrollOffset);
                textY = drawShinyItems(ctx, x, textY, items, width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                textY = drawMythicItems(ctx, x, textY, items, width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                textY = drawTomeItems(ctx, x, textY, items, width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                textY = drawItemsByRarity(ctx, x, textY, items, "Fabled", width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                textY = drawItemsByRarity(ctx, x, textY, items, "Legendary", width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                textY = drawItemsByRarity(ctx, x, textY, items, "Rare", width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                textY = drawItemsByRarity(ctx, x, textY, items, "Set", width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                textY = drawItemsByRarity(ctx, x, textY, items, "Unique", width, mouseX, mouseY, contentStartY, contentHeight, actualOffset);

                ctx.disableScissor();

                if (totalContentHeight > contentHeight) {
                    int scrollBarHeight = Math.max(20, contentHeight * contentHeight / totalContentHeight);
                    float scrollBarY = contentStartY + (scrollOffset * (contentHeight - scrollBarHeight) / maxOffset);
                    ui.drawRect(x + width - 12, scrollBarY, 6, scrollBarHeight, CustomColor.fromInt(0xFFAAAAAA));
                }
            }

            int listTop = y + 195;
            int listBottom = y + height - 40;
            float visibleHeight = listBottom - listTop;

            maxOffset = Math.max(contentHeight - visibleHeight, 0);

            if(targetOffset > maxOffset) {
                targetOffset = maxOffset;
            }

            ctx.disableScissor();

            scrollBarWidget.setBounds(x + width, y + 80, 25, height - 80);
            scrollBarWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
        }

        private int drawShinyItems(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                                   int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> shinyItems = items.stream()
                    .filter(i -> i.type.equals("shiny"))
                    .toList();

            if (shinyItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : shinyItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 10;
        }

        private int drawMythicItems(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                                    int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> mythicItems = items.stream()
                    .filter(i -> i.rarity.equals("Mythic") && !i.type.equals("shiny"))
                    .toList();

            if (mythicItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : mythicItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 10;
        }

        private int drawTomeItems(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                                  int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> tomeItems = items.stream()
                    .filter(i -> i.type.equals("tome"))
                    .toList();

            if (tomeItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : tomeItems) {
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 10;
        }

        private int drawItemsByRarity(DrawContext context, int x, int textY, List<LootrunLootPoolData.LootrunItem> items,
                                      String rarity, int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, float scrollOffset) {
            int itemSpacing = 32;
            List<LootrunLootPoolData.LootrunItem> filteredItems = items.stream()
                    .filter(i -> i.rarity.equals(rarity) && !i.type.equals("shiny") && !i.type.equals("tome"))
                    .toList();

            if (filteredItems.isEmpty()) return textY;

            for (LootrunLootPoolData.LootrunItem item : filteredItems) {
                if(item.name.contains("Emerald")) continue;
                textY = drawItem(context, x, textY, item, colWidth, mouseX, mouseY, contentStartY, contentHeight, scrollOffset, itemSpacing);
            }
            return textY + 10;
        }

        private int drawItem(DrawContext context, int x, int textY, LootrunLootPoolData.LootrunItem item,
                             int colWidth, int mouseX, int mouseY, int contentStartY, int contentHeight, float scrollOffset, int itemSpacing) {
            if (textY + itemSpacing >= contentStartY && textY <= contentStartY + contentHeight) {
                boolean hovering = mouseX >= x + 12 && mouseX <= x + colWidth - 12 &&
                        mouseY >= textY && mouseY <= textY + itemSpacing - 5;

                String rarityColor = item.type.equals("tome") ? "§d" : getRarityColor(item.rarity);
                String displayName = truncate(item.name, 40);

                if (item.type.equals("shiny")) {
                    ui.drawText("✦ ", x + 20, textY, CommonColors.RAINBOW, 2.2f);
                    ui.drawText(rarityColor + displayName, x + 38, textY, CustomColor.fromInt(0xFFFFFF), 2.2f);
                } else {
                    ui.drawText(rarityColor + displayName, x + 20, textY, CustomColor.fromInt(0xFFFFFF), 2.2f);
                }
                if (item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty()) {
                    ui.drawText("§7[§a" + item.shinyStat + "§7]", x + 25, textY + 18, CustomColor.fromInt(0xFFFFFF), 1.8f);
                }

                if (hovering) {
                    hoveredItem = item;
                }
            }
            int extraSpacing = (item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty()) ? 18 : 0;
            return textY + itemSpacing + extraSpacing;
        }

        private String truncate(String text, int maxLen) {
            if (text.length() <= maxLen) return text;
            return text.substring(0, maxLen - 3) + "...";
        }

        private String capitalize(String text) {
            if (text == null || text.isEmpty()) return text;
            return text.substring(0, 1).toUpperCase() + text.substring(1);
        }

        private List<LootrunLootPoolData.LootrunItem> getLootPoolForCamp(String campCode) {
            if (crowdsourcedLootPools.containsKey(campCode) && crowdsourcedLootPools.get(campCode) != null) {
                List<LootrunLootPoolData.LootrunItem> items = crowdsourcedLootPools.get(campCode);
                if (!items.isEmpty()) {
                    return items;
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
                ui.drawSliderBackground(x, y, width, height, 5, WynnExtrasConfig.INSTANCE.darkmodeToggle);

                int buttonHeight = 75;
                int scrollAreaHeight = height - buttonHeight;

                if (scrollBarButtonWidget.isHold) {
                    setOffset((int) (mouseY * ui.getScaleFactor()), (int) maxOffset, scrollAreaHeight);
                    parent.actualOffset = parent.targetOffset;
                }

                int yPos = maxOffset == 0 ? y : (int) (y + scrollAreaHeight * Math.min((parent.actualOffset / maxOffset), 1));
                scrollBarButtonWidget.setBounds(x, yPos, width, buttonHeight);
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
                    ui.drawButton(x, y, width, height, 5, hovered || isHold, WynnExtrasConfig.INSTANCE.darkmodeToggle);
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
}