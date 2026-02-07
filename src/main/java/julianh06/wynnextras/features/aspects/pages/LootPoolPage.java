package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.aspects.FavoriteAspectsData;
import julianh06.wynnextras.features.aspects.LootPoolData;
import julianh06.wynnextras.utils.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static julianh06.wynnextras.features.aspects.AspectUtils.*;

public class LootPoolPage extends PageWidget {
    private static java.util.Map<String, List<julianh06.wynnextras.features.aspects.LootPoolData.AspectEntry>> crowdsourcedLootPools = new java.util.HashMap<>();
    private static boolean fetchedCrowdsourcedLootPools = false;

    private static java.util.Map<String, com.mojang.datafixers.util.Pair<Integer, String>> personalAspectProgress = new java.util.HashMap<>();
    private static boolean fetchedPersonalProgress = false;

    private enum Raid { NOTG, NOL, TCC, TNA }

    static List<LootPoolWidget> lootPoolWidgets = new ArrayList<>();

    private static List<Text> hoveredTooltip = new ArrayList<>();

    private static String importFeedback = null;
    private static long importFeedbackTime = 0;

    private ImportFromWynntilsButton importFromWynntilsButton;
    private HideMaxButton hideMaxButton;
    private OnlyFavoritesButton onlyFavoritesButton;
    private RefreshButton refreshButton;

    private static boolean hideMax = false;
    private static boolean onlyFavorites = false;

    private enum corwdSourceStatus { Loading, Found, Null }
    private static List<corwdSourceStatus> hasCrowdSourcedData = new ArrayList<>(List.of(corwdSourceStatus.Loading, corwdSourceStatus.Loading, corwdSourceStatus.Loading, corwdSourceStatus.Loading));

    private static String[] raidNames = {
            "Nest of the Grootslangs",
            "Orphion's Nexus of Light",
            "The Canyon Colossus",
            "The Nameless Anomaly"
    };

    public LootPoolPage(AspectScreen parent) {
        super(parent);

        for(Raid raid : Raid.values()) {
            lootPoolWidgets.add(new LootPoolWidget(raid));
        }

        importFromWynntilsButton = new ImportFromWynntilsButton();
        hideMaxButton = new HideMaxButton();
        onlyFavoritesButton = new OnlyFavoritesButton();
        refreshButton = new RefreshButton();
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        hoveredTooltip.clear();
        if (!fetchedCrowdsourcedLootPools) {
            fetchedCrowdsourcedLootPools = true;
            String[] raids = {"NOTG", "NOL", "TCC", "TNA"};
            int i = 0;
            for (String raidType : raids) {
                int finalI = i;
                WynncraftApiHandler.fetchCrowdsourcedLootPool(raidType).thenAccept(result -> {
                    if(result == null) {
                        hasCrowdSourcedData.set(finalI, corwdSourceStatus.Null);
                    }
                    if (result != null && !result.isEmpty()) {
                        hasCrowdSourcedData.set(finalI, corwdSourceStatus.Found);
                        crowdsourcedLootPools.put(raidType, result);
                        // Save to local data for offline access
                        julianh06.wynnextras.features.aspects.LootPoolData.INSTANCE.saveLootPoolFull(raidType, result);
                    }
                });
                i++;
            }
        }

        if (!fetchedPersonalProgress && McUtils.player() != null) {
            fetchedPersonalProgress = true;
            String playerUUID = McUtils.player().getUuidAsString();
            WynncraftApiHandler.fetchPlayerAspectData(playerUUID, playerUUID).thenAccept(result -> {
                if (result != null && result.status() == WynncraftApiHandler.FetchStatus.OK && result.user() != null) {
                    julianh06.wynnextras.features.profileviewer.data.User userData = result.user();
                    // Convert aspect data to progress map (name -> (amount, rarity))
                    java.util.List<julianh06.wynnextras.features.profileviewer.data.Aspect> aspects = userData.getAspects();
                    if (aspects != null) {
                        for (julianh06.wynnextras.features.profileviewer.data.Aspect aspect : aspects) {
                            personalAspectProgress.put(aspect.getName(),
                                    new com.mojang.datafixers.util.Pair<>(aspect.getAmount(), aspect.getRarity()));
                        }
                    }
                }
            });
        }

        float centerX = (x + width / 2f) * ui.getScaleFactorF();

        ui.drawCenteredText("§6§lWeekly Aspect Lootpools", centerX, 60);

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("CET"));
        ZonedDateTime nextReset = now.with(java.time.DayOfWeek.FRIDAY).withHour(19).withMinute(0).withSecond(0).withNano(0);
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
        int widgets = 4;
        int totalSpacing = spacing * (widgets + 1);
        float scaledWidth = width * ui.getScaleFactorF();
        int widgetWidth = (int) ((scaledWidth - totalSpacing) / widgets);

        int widgetHeight = (int) (height * ui.getScaleFactorF() * 0.9f - widgetY);

        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            lootPoolWidget.setBounds(widgetX, widgetY, widgetWidth, widgetHeight);
            lootPoolWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            widgetX += widgetWidth + spacing;
        }

        importFromWynntilsButton.setBounds(0, 0, 500, 60);
        importFromWynntilsButton.draw(ctx, mouseX, mouseY, tickDelta, ui);

        refreshButton.setBounds(520, 0, 350, 60);
        refreshButton.draw(ctx, mouseX, mouseY, tickDelta, ui);

        hideMaxButton.setBounds((int) (width * ui.getScaleFactorF()) - 300, 0, 300, 60);
        hideMaxButton.draw(ctx, mouseX, mouseY, tickDelta, ui);

        onlyFavoritesButton.setBounds((int) (width * ui.getScaleFactorF()) - 720, 0, 400, 60);
        onlyFavoritesButton.draw(ctx, mouseX, mouseY, tickDelta, ui);

        if (importFeedback != null && System.currentTimeMillis() - importFeedbackTime < 5000) {
            ui.drawCenteredText(importFeedback, 240, 74);
        } else {
            importFeedback = null;
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(hoveredTooltip.isEmpty()) return;
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(), mouseX - 5, mouseY + 20);
    }

    @Override
    protected boolean onClick(int button) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if(lootPoolWidget.onClick(button)) return true;
        }
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if(lootPoolWidget.mouseClicked(mx, my, button)) return true;
        }

        if(importFromWynntilsButton.isHovered()) {
            importFromWynntilsButton.onClick(button);
            return true;
        }

        if(hideMaxButton.isHovered()) {
            hideMaxButton.onClick(button);
            return true;
        }

        if(onlyFavoritesButton.isHovered()) {
            onlyFavoritesButton.onClick(button);
            return true;
        }

        if(refreshButton.isHovered()) {
            refreshButton.onClick(button);
            return true;
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

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if(lootPoolWidget.mouseScrolled(mx, my, delta)) return true;
        }

        return false;
    }

    public void pageOpened() {
        String[] raids = {"NOTG", "NOL", "TCC", "TNA"};
        int i = 0;
        for (String raidType : raids) {
            if(crowdsourcedLootPools.get(raidType) != null) {
                if(!crowdsourcedLootPools.get(raidType).isEmpty()) continue;
            }

            int finalI = i;
            WynncraftApiHandler.fetchCrowdsourcedLootPool(raidType).thenAccept(result -> {
                if(result == null) {
                    hasCrowdSourcedData.set(finalI, corwdSourceStatus.Null);
                }
                if (result != null && !result.isEmpty()) {
                    hasCrowdSourcedData.set(finalI, corwdSourceStatus.Found);
                    crowdsourcedLootPools.put(raidType, result);
                    // Save to local data for offline access
                    julianh06.wynnextras.features.aspects.LootPoolData.INSTANCE.saveLootPoolFull(raidType, result);
                }
            });
            i++;
        }
    }

    private static class LootPoolWidget extends Widget {
        static Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png");
        static Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png");
        static Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png");
        static Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png");

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

        final Raid raid;
        PersonalScoreWidget scoreWidget;
        ScrollBarWidget scrollBarWidget;
        List<AspectWidget> aspectWidgets = new ArrayList<>();

        float targetOffset = 0;
        float actualOffset = 0;
        float maxOffset = 999;
        int textureWidth = 150;

        public LootPoolWidget(Raid raid) {
            super(0, 0, 0, 0);
            this.raid = raid;
            scoreWidget = new PersonalScoreWidget(raid);
            scrollBarWidget = new ScrollBarWidget(this);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int topHeight = 202;

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

            ui.drawImage(getTextureForRaid(raid), x + (width - textureWidth) / 2f, y - textureWidth / 4f, textureWidth, textureWidth);

            // Calculate and show score
            DecimalFormat df = new DecimalFormat("#.00");

            List<LootPoolData.AspectEntry> lootPool = getLootPoolForRaid(raid.name());

            if(!lootPool.isEmpty() && hasCrowdSourcedData.get(raid.ordinal()) == corwdSourceStatus.Null) {
                hasCrowdSourcedData.set(raid.ordinal(), corwdSourceStatus.Found);
            }

            boolean allLoaded = true;
            for(LootPoolData.AspectEntry entry : lootPool) {
                if(entry.tierInfo.isEmpty()) {
                    allLoaded = false;
                    break;
                }
            }

            List<LootPoolData.AspectEntry> mythicAspects = lootPool.stream().filter(a -> a.rarity.equalsIgnoreCase("mythic")).toList();
            List<LootPoolData.AspectEntry> fabledAspects = lootPool.stream().filter(a -> a.rarity.equalsIgnoreCase("fabled")).toList();
            List<LootPoolData.AspectEntry> legendaryAspects = lootPool.stream().filter(a -> a.rarity.equalsIgnoreCase("legendary")).toList();

            //aspectWidgets.clear();
            if(aspectWidgets.isEmpty() && allLoaded) {
                for (LootPoolData.AspectEntry entry : mythicAspects) {
                    if(hideMax && entry.tierInfo.contains("MAX")) continue;
                    if(onlyFavorites && !FavoriteAspectsData.INSTANCE.isFavorite(entry.name)) continue;
                    aspectWidgets.add(new AspectWidget(entry, this));
                }
                for (LootPoolData.AspectEntry entry : fabledAspects) {
                    if(hideMax && entry.tierInfo.contains("MAX")) continue;
                    if(onlyFavorites && !FavoriteAspectsData.INSTANCE.isFavorite(entry.name)) continue;
                    aspectWidgets.add(new AspectWidget(entry, this));
                }
                for (LootPoolData.AspectEntry entry : legendaryAspects) {
                    if(hideMax && entry.tierInfo.contains("MAX")) continue;
                    if(onlyFavorites && !FavoriteAspectsData.INSTANCE.isFavorite(entry.name)) continue;
                    aspectWidgets.add(new AspectWidget(entry, this));
                }
            }

            double score = calculateRaidScore(lootPool);
            String scoreString = "Personal Score: " + df.format(score);

            boolean max = score == 0;
            if(max) {
                scoreString = "MAXED";
            }

            ui.drawCenteredText(raidNames[raid.ordinal()], x + width / 2f, y + textureWidth - 20, max ? CommonColors.RAINBOW : CustomColor.fromHexString("FFFFFF"));

            scoreWidget.scoreString = scoreString;
            int scoreWidth = MinecraftClient.getInstance().textRenderer.getWidth(scoreString);
            if(hasCrowdSourcedData.get(raid.ordinal()) != corwdSourceStatus.Found) {
                scoreWidget.setBounds(0, 0, 0, 0);
                if(hasCrowdSourcedData.get(raid.ordinal()) == corwdSourceStatus.Loading) {
                    ui.drawCenteredText("Loading lootpool data...", x + width / 2f, y + textureWidth + 14, CustomColor.fromHexString("FF0000"));
                } else {
                    ui.drawCenteredText("There is data for this raid yet!", x + width / 2f, y + textureWidth + 14, CustomColor.fromHexString("FF0000"));
                }
            } else {
                scoreWidget.setBounds((int) (x + (width - scoreWidth * 3) / 2f), y + textureWidth, scoreWidth * 3, 30);
                scoreWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            }
            ctx.enableScissor(
                    (int) (x / ui.getScaleFactor()),
                    (int) ((y + 195) / ui.getScaleFactor()),
                    (int) ((x + width - 7) / ui.getScaleFactor()),
                    (int) ((y + height - 20) / ui.getScaleFactor()));

            float snapValue = 0.5f;
            float speed = 0.3f;
            float diff = (targetOffset - actualOffset);
            if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
            else actualOffset += diff * speed * tickDelta;

            int aspectY = y + textureWidth + 60 - (int) actualOffset;
            int aspectHeight = 50;
            int spacing = 5;

            float contentHeight = 0;

            for (int i = 0; i < aspectWidgets.size(); i++) {
                contentHeight += aspectHeight + spacing;

                AspectWidget aspectWidget = aspectWidgets.get(i);

                aspectWidget.setBounds(x, aspectY, width, aspectHeight);
                aspectWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);

                aspectY += aspectHeight + spacing;

                boolean isLastOfRarity =
                        i + 1 < aspectWidgets.size() &&
                                !aspectWidgets.get(i + 1).aspect.rarity
                                        .equalsIgnoreCase(aspectWidget.aspect.rarity);

                if (isLastOfRarity) {
                    contentHeight += spacing * 4;
                    aspectY += spacing * 4;
                    ui.drawLine(
                            x + 20,
                            aspectY - spacing * 2,
                            x + width - 20,
                            aspectY - spacing * 2,
                            3,
                            WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode
                                    ? CustomColor.fromHexString("1b1b1c")
                                    : CustomColor.fromHexString("5d4736")
                    );
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

            scrollBarWidget.setBounds(x + width - 20, y + 195, 15, height - 215);
            scrollBarWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
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
        protected boolean onClick(int button) {
            for(AspectWidget aspectWidget : aspectWidgets) {
                if(aspectWidget.isHovered()) {
                    aspectWidget.onClick(button);
                    return true;
                }
            }

            if(scoreWidget.isHovered()) {
                scoreWidget.onClick(button);
                return true;
            }

            return false;
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

        private static Identifier getTextureForRaid(Raid raid) {
            return switch (raid) {
                case NOTG -> NOTGTexture;
                case NOL -> NOLTexture;
                case TCC -> TCCTexture;
                case TNA -> TNATexture;
                case null, default -> null;
            };
        }

        private double calculateRaidScore(List<LootPoolData.AspectEntry> aspects) {
            if(aspects.isEmpty()) return -1;

            double score = 0.0;

            for (LootPoolData.AspectEntry aspect : aspects) {
                String tierInfo = aspect.tierInfo;

                if (tierInfo == null || tierInfo.isEmpty() || tierInfo.contains("[MAX]")) {
                    continue; // Already maxed or no data, no score contribution
                }

                // Parse tierInfo: "Tier I >>>>>> Tier II [10/14]"
                int remaining = 0;
                String currentTierStr = "";
                String targetTierStr = "";

                // Extract remaining count [X/Y]
                java.util.regex.Pattern progressPattern = java.util.regex.Pattern.compile("\\[(\\d+)/(\\d+)\\]");
                java.util.regex.Matcher progressMatcher = progressPattern.matcher(tierInfo);
                if (!progressMatcher.find()) {
                    continue; // Can't parse progress, skip this aspect
                }

                int current = Integer.parseInt(progressMatcher.group(1));
                int max = Integer.parseInt(progressMatcher.group(2));
                remaining = max - current;

                // Extract tiers (match I, II, III, IV properly)
                java.util.regex.Pattern tierPattern = java.util.regex.Pattern.compile("Tier\\s+(IV|III|II|I)");
                java.util.regex.Matcher tierMatcher = tierPattern.matcher(tierInfo);
                if (tierMatcher.find()) {
                    currentTierStr = tierMatcher.group(1); // First match = current tier
                    if (tierMatcher.find()) {
                        targetTierStr = tierMatcher.group(1); // Second match = target tier
                    } else {
                        // No target tier found - working to max out current tier
                        targetTierStr = currentTierStr;
                    }
                } else {
                    continue; // Can't parse tiers, skip this aspect
                }

                int currentTier = romanToInt(currentTierStr);
                int targetTier = romanToInt(targetTierStr);

                if (currentTier == 0 || targetTier == 0) {
                    continue; // Invalid tier, skip
                }

                // Apply tier-based weights
                double weight = getTierWeight(aspect.rarity, currentTier, targetTier);
                double contribution = remaining * weight;

                // Favorite aspects count 3x more
                if (FavoriteAspectsData.INSTANCE.isFavorite(aspect.name)) {
                    contribution *= 3.0;
                }

                score += contribution;
            }

            return score;
        }

        private List<LootPoolData.AspectEntry> getLootPoolForRaid(String raidCode) {
            // Use crowdsourced data if available
            if (crowdsourcedLootPools.containsKey(raidCode)) {
                List<LootPoolData.AspectEntry> crowdsourced = crowdsourcedLootPools.get(raidCode);
                if (crowdsourced != null && !crowdsourced.isEmpty()) {
                    // Overlay personal progress on crowdsourced data
                    List<LootPoolData.AspectEntry> withProgress = new java.util.ArrayList<>();
                    for (LootPoolData.AspectEntry aspect : crowdsourced) {
                        // Check if we have personal progress for this aspect
                        if (personalAspectProgress.containsKey(aspect.name)) {
                            com.mojang.datafixers.util.Pair<Integer, String> progress = personalAspectProgress.get(aspect.name);
                            int amount = progress.getFirst();
                            String rarity = progress.getSecond();

                            // Convert amount to tier info string
                            String tierInfo = convertAmountToTierInfo(amount, rarity);

                            // Create new entry with personal tier info
                            withProgress.add(new LootPoolData.AspectEntry(aspect.name, rarity, tierInfo, aspect.description));
                        } else {
                            // No personal data, use crowdsourced as-is (will show without tier)
                            withProgress.add(aspect);
                        }
                    }
                    return withProgress;
                }
            }
            // Fall back to local data
            return LootPoolData.INSTANCE.getLootPool(raidCode);
        }


        private class ScrollBarWidget extends Widget {
            ScrollBarButtonWidget scrollBarButtonWidget;
            int currentMouseY = 0;
            LootPoolWidget parent;

            public ScrollBarWidget(LootPoolWidget parent) {
                super(0, 0, 0, 0);
                this.scrollBarButtonWidget = new ScrollBarButtonWidget();
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

        private static class PersonalScoreWidget extends Widget {
            String scoreString = "";
            final Raid raid;

            public PersonalScoreWidget(Raid raid) {
                super(0, 0, 0, 0);
                this.raid = raid;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                if(scoreString.isEmpty()) return;

                if(scoreString.equals("MAXED")) ui.drawText(scoreString, x, y, CommonColors.RAINBOW);
                else ui.drawText((hovered ? "§n" : "") + scoreString, x, y, CustomColor.fromHexString("c0c0c0"));

                if(hovered) {
                    List<Text> tooltip = new ArrayList<>();
                    tooltip.add(Text.literal("§6§l" + raid.name()));
                    tooltip.add(Text.literal("§7" + raidNames[raid.ordinal()]));
                    tooltip.add(Text.literal(""));
                    tooltip.add(Text.literal(scoreString));
                    tooltip.add(Text.literal("§8(Favorites count 3x)"));
                    tooltip.add(Text.literal(""));
                    tooltip.add(Text.literal("§aClick to join party finder"));
                    hoveredTooltip = tooltip;
                }
            }

            @Override
            protected boolean onClick(int button) {
                joinRaidPartyFinder(raid.name());
                return true;
            }
        }

        private static class AspectWidget extends Widget {
            final LootPoolData.AspectEntry aspect;
            final LootPoolWidget parent;

            public AspectWidget(LootPoolData.AspectEntry aspect, LootPoolWidget parent) {
                super(0, 0, 0, 0);
                this.aspect = aspect;
                this.parent = parent;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                //ui.drawRect(x, y, width, height);

                boolean isFavorite = FavoriteAspectsData.INSTANCE.isFavorite(aspect.name);
                int extra = (hovered || isFavorite) ? 10 : 0;
                int maxWidth = (int) ((width) / 3) - 40 - extra;
                String displayName = aspect.name;
                TextRenderer tr = MinecraftClient.getInstance().textRenderer;

                int availableWidth = (int)(maxWidth) - extra;

                if (tr.getWidth(displayName) > availableWidth) {
                    displayName = tr.trimToWidth(displayName, availableWidth - tr.getWidth("...")) + "...";
                }


                boolean isMax = aspect.tierInfo.contains("MAX");
                CustomColor textColor = CustomColor.fromHexString("FFFFFF");
                String rarityColorCode = "";
                if(isMax) {

                    textColor = CommonColors.RAINBOW;
                } else {
                    rarityColorCode = getAspectColorCode(aspect);
                }

                ui.drawText(rarityColorCode + displayName + (isFavorite ? " §e⭐" : ((hovered && parent.isHovered()) ? " §7☆" : "")), x + 87, y + 3 + height / 2f, textColor, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);


                ApiAspect apiAspect = findApiAspectByName(aspect.name);
                ItemStack flameItem = createAspectFlameIcon(apiAspect, isMax);
                if (!flameItem.isEmpty() && ui != null) {
                    int screenX = (int) ui.sx(x + 40);
                    int screenY = (int) ui.sy(y + 13); // Move flame up
                    float flameScale = 2.1f;
                    ctx.getMatrices().pushMatrix();
                    ctx.getMatrices().scale(flameScale / ui.getScaleFactorF(), flameScale / ui.getScaleFactorF());
                    ctx.drawItem(flameItem, (int)(screenX / (flameScale / ui.getScaleFactorF())), (int)(screenY / (flameScale / ui.getScaleFactorF())));
                    ctx.getMatrices().popMatrix();
                }

                if(hovered && parent.isHovered() && mouseY * ui.getScaleFactorF() > parent.y + 190) {
                    List<Text> tooltip = new ArrayList<>();
                    if(apiAspect == null) return;
                    int tier = 0;
                    if(isMax) {
                        if(aspect.rarity.equalsIgnoreCase("Legendary")) tier = 4;
                        else tier = 3;
                    }

                    Pattern pattern = Pattern.compile("Tier ([IVXLCDM]+)");
                    Matcher matcher = pattern.matcher(aspect.tierInfo);

                    if (matcher.find()) {
                        String roman = matcher.group(1);
                        tier = romanToInt(roman);
                    }

                    ItemStack aspectItemStack = toItemStack(apiAspect, isMax, tier);
                    tooltip = aspectItemStack.getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);

                    int longestTextWidth = 0;
                    for(Text text : tooltip) {
                        if(MinecraftClient.getInstance().textRenderer.getWidth(text) > longestTextWidth) longestTextWidth = MinecraftClient.getInstance().textRenderer.getWidth(text);
                    }

                    String name = tooltip.getFirst().getString();

                    tooltip.set(0, Text.of(name + " §7" + aspect.tierInfo));

                    hoveredTooltip = tooltip;
                }
            }

            @Override
            protected boolean onClick(int button) {
                if(!parent.isHovered()) return false;
                McUtils.playSoundUI(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK);
                FavoriteAspectsData.INSTANCE.toggleFavorite(aspect.name);
                return true;
            }
        }
    }

    private static class ImportFromWynntilsButton extends Widget {
        public ImportFromWynntilsButton() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);
            ui.drawCenteredText("Import favorites from Wynntils", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            int imported = FavoriteAspectsData.INSTANCE.importFromWynntils();
            if (imported > 0) {
                importFeedback = "§aImported " + imported + " favorites!";
            } else {
                importFeedback = "§7No new favorites to import";
            }
            importFeedbackTime = System.currentTimeMillis();
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }

    private static class HideMaxButton extends Widget {
        public HideMaxButton() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);
            ui.drawCenteredText("Hide max aspects", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            hideMax = !hideMax;
            for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
                lootPoolWidget.aspectWidgets.clear();
            }

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }

    private static class OnlyFavoritesButton extends Widget {
        public OnlyFavoritesButton() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);
            ui.drawCenteredText("Only favorite aspects", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            onlyFavorites = !onlyFavorites;
            for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
                lootPoolWidget.aspectWidgets.clear();
            }

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }

    private static class RefreshButton extends Widget {
        public RefreshButton() {
            super(0, 0, 0, 0);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);
            ui.drawCenteredText("Reload your aspects", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            personalAspectProgress.clear();
            fetchedPersonalProgress = false;
            for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
                lootPoolWidget.aspectWidgets.clear();
            }

            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
    }
}
