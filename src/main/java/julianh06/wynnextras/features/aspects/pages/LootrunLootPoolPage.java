package julianh06.wynnextras.features.aspects.pages;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.aspects.LootrunLootPoolData;
import julianh06.wynnextras.features.aspects.LootrunScanning;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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

    public enum Camp { SI, SE, CORK, COTL, MH }

    private static String[] campNames = {
        "Sky Islands",
        "Silent Expanse",
        "Corkus Traversal",
        "Canyon of the Lost",
        "Molten Heights"
    };

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

            System.out.println("starting fetch for " + camp);
            lastCrowdsourceFetch.put(camp, now);
            WynncraftApiHandler.fetchCrowdsourcedLootrunLootPool(camp.name()).thenAccept(result -> {
                fetchRunning.put(camp, false);

                if (result == null || result.isEmpty()) return;

                List<LootrunLootPoolData.LootrunItem> oldItems = crowdsourcedLootPools.get(camp.name());

                lastCrowdsourceFetch.put(camp, now);
                if (isSamePool(oldItems, result)) {
                    System.out.println("still old pool, retry in 30s");
                    hasOldLootpool.put(camp, true);
                    return;
                }

                System.out.println("NEW POOL for " + camp);

                crowdsourcedLootPools.put(camp.name(), result);
                hasOldLootpool.put(camp, false);

                LootrunLootPoolData.INSTANCE.saveLootPool(camp.name(), result);
            });
        }

        ui.drawCenteredText("§6§lWeekly Lootrun Lootpools", centerX, 60, CustomColor.fromInt(0xFFFFFF), 3f);

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

        int spacing = 20;
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
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(), mouseX - 5, mouseY + 20);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if(lootPoolWidget.mouseScrolled(mx, my, delta)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if(lootPoolWidget.mouseClicked(mx, my, button)) return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            lootPoolWidget.mouseReleased(mx, my, button);
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
            int topHeight = 94;

            if(WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode) {
                ui.drawNineSlice((int) (x),
                        (int) (y), width,
                        (int) (topHeight + 1), 33, ltopd, rtopd, ttopd, btopd, tltopd, trtopd, bltopd, brtopd, CustomColor.fromHexString("2c2d2f"));

                ui.drawNineSlice((int) (x),
                        (int) (y + topHeight), width,
                        (int) (height - topHeight), 33, ld, rd, td, bd, tld, trd, bld, brd, CustomColor.fromHexString("444448"));
            } else {
                ui.drawNineSlice((int) (x),
                        (int) (y), width,
                        (int) (topHeight + 1), 33, ltop, rtop, ttop, btop, tltop, trtop, bltop, brtop, CustomColor.fromHexString("81644b"));

                ui.drawNineSlice((int) (x),
                        (int) (y + topHeight), width,
                        (int) (height - topHeight), 33, l, r, t, b, tl, tr, bl, br, CustomColor.fromHexString("cca76f"));
            }

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
                ui.drawCenteredText("§7No data", x + width / 2f, contentStartY + 40, CustomColor.fromInt(0xFFFFFF), 3f);
                ui.drawCenteredText("§7Open lootrun", x + width / 2f, contentStartY + 80, CustomColor.fromInt(0xFFFFFF), 2.5f);
                ui.drawCenteredText("§7chest to scan", x + width / 2f, contentStartY + 110, CustomColor.fromInt(0xFFFFFF), 2.5f);
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
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("5d4736"));
                textY = drawMythicItems(ctx, textX, textY, items, width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("5d4736"));
                textY = drawTomeItems(ctx, textX, textY, items, width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("5d4736"));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Fabled", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("5d4736"));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Legendary", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("5d4736"));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Rare", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("5d4736"));
                textY = drawItemsByRarity(ctx, textX, textY, items, "Set", width - 15, mouseX, mouseY, contentStartY, contentHeight, actualOffset);
                ui.drawLine(x + 20, textY - 15, x + width - 20, textY - 15, 3, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromHexString("1b1b1c") : CustomColor.fromHexString("5d4736"));
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
                String displayName = truncate(item.name, width / 2 - 30).replace("Unidentified ", "");

                if (item.type.equals("shiny")) {
                    ui.drawText(displayName.replace("⬡ ", ""), x + 20, textY, WynnExtrasConfig.INSTANCE.removeChroma ? CustomColor.fromHexString("FFFFFF") : CommonColors.RAINBOW, 3f);
                } else {
                    ui.drawText(rarityColor + displayName, x + 20, textY, CustomColor.fromInt(0xFFFFFF), 2.8f);
                }
                boolean isShiny = item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty();
                if (isShiny) {
                    ui.drawText(item.shinyStat.replace(": §f0", ""), x + 20, textY + 35, CustomColor.fromInt(0xFFFFFF), 2.2f);
                }

                if (hovering && WynncraftApiHandler.cachedItemDatabase != null && mouseY * ui.getScaleFactorF() > y + 80) {
                    JsonObject jsonItem = WynncraftApiHandler.cachedItemDatabase.get(item.name.replace("Unidentified ", "").replace("⬡ ", "").replace("Shiny ", ""));
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(Text.of(rarityColor + item.name.replace("Unidentified ", "")));
                    if(jsonItem != null && item.name.contains("Tome")) tooltip.addAll(buildTooltipFromApi(jsonItem));
                    hoveredTooltip = tooltip;
                }
            }
            int extraSpacing = (item.type.equals("shiny") && item.shinyStat != null && !item.shinyStat.isEmpty()) ? 35 : 0;
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
                    ui.drawRect(x, y, width, height, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? CustomColor.fromInt(0xFF707070) : CustomColor.fromInt(0xFF674439));
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

    private static boolean shouldFetchLootPool(Camp camp) {
        ZonedDateTime currentReset = LootrunScanning.getCurrentLootrunReset();
        ZonedDateTime lastFetch = lastCrowdsourceFetch.get(camp);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));

        if(hasOldLootpool.get(camp) != null && hasOldLootpool.get(camp)) return lastFetch.plusSeconds(30).isBefore(now);

        if(lastFetch != null && lastFetch.plusSeconds(30).isAfter(now)) return false;

        return lastFetch == null || currentReset.isAfter(lastFetch);
    }
}