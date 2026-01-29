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
import julianh06.wynnextras.features.bankoverlay.BankOverlay2;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static julianh06.wynnextras.features.aspects.AspectUtils.*;

public class LootPoolPage extends PageWidget {
    private static java.util.Map<String, List<julianh06.wynnextras.features.aspects.LootPoolData.AspectEntry>> crowdsourcedLootPools = new java.util.HashMap<>();
    private static boolean fetchedCrowdsourcedLootPools = false;

    private static java.util.Map<String, com.mojang.datafixers.util.Pair<Integer, String>> personalAspectProgress = new java.util.HashMap<>();
    private static boolean fetchedPersonalProgress = false;

    private enum Raid { NOTG, NOL, TCC, TNA }

    List<LootPoolWidget> lootPoolWidgets = new ArrayList<>();

    private static List<Text> hoveredTooltip = new ArrayList<>();

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
    }

    @Override
    protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        hoveredTooltip.clear();
        if (!fetchedCrowdsourcedLootPools) {
            fetchedCrowdsourcedLootPools = true;
            String[] raids = {"NOTG", "NOL", "TCC", "TNA"};
            for (String raidType : raids) {
                WynncraftApiHandler.fetchCrowdsourcedLootPool(raidType).thenAccept(result -> {
                    if (result != null && !result.isEmpty()) {
                        crowdsourcedLootPools.put(raidType, result);
                        // Save to local data for offline access
                        julianh06.wynnextras.features.aspects.LootPoolData.INSTANCE.saveLootPoolFull(raidType, result);
                    }
                });
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
        int widgetWidth = (int) (((parent.getScreenWidth() - spacing) * ui.getScaleFactorF() - spacing * 2) / 4f);
        int widgetHeight = 1200 - widgetY;

        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            lootPoolWidget.setBounds(widgetX, widgetY, widgetWidth, widgetHeight);
            lootPoolWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            widgetX += widgetWidth + spacing;
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(hoveredTooltip.isEmpty()) return;
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(), mouseX, mouseY);
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
        return onClick(button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        for(LootPoolWidget lootPoolWidget : lootPoolWidgets) {
            if(lootPoolWidget.mouseScrolled(mx, my, delta)) return true;
        }

        return false;
    }

    private static class LootPoolWidget extends Widget {
        static Identifier NOTGTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/notg.png");
        static Identifier NOLTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/nol.png");
        static Identifier TCCTexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tcc.png");
        static Identifier TNATexture = Identifier.of("wynnextras", "textures/gui/profileviewer/rankingicons/tna.png");

        final Raid raid;
        PersonalScoreWidget scoreWidget;

        List<AspectWidget> aspectWidgets = new ArrayList<>();

        float targetOffset = 0;
        float actualOffset = 0;
        float maxOffset = 999;

        public LootPoolWidget(Raid raid) {
            super(0, 0, 0, 0);
            this.raid = raid;
            scoreWidget = new PersonalScoreWidget(raid);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 12, false);
            int textureWidth = 150;

            ui.drawImage(getTextureForRaid(raid), x + (width - textureWidth) / 2f, y - textureWidth / 4f, textureWidth, textureWidth);
            ui.drawLine(x + 2, y + textureWidth + 40, x + width - 2, y + textureWidth + 40, 3, CustomColor.fromHexString("a68a73"));

            // Calculate and show score
            DecimalFormat df = new DecimalFormat("#.00");

            List<LootPoolData.AspectEntry> lootPool = getLootPoolForRaid(raid.name());

            List<LootPoolData.AspectEntry> mythicAspects = lootPool.stream().filter(a -> a.rarity.equalsIgnoreCase("mythic")).toList();
            List<LootPoolData.AspectEntry> fabledAspects = lootPool.stream().filter(a -> a.rarity.equalsIgnoreCase("fabled")).toList();
            List<LootPoolData.AspectEntry> legendaryAspects = lootPool.stream().filter(a -> a.rarity.equalsIgnoreCase("legendary")).toList();

            if(aspectWidgets.isEmpty()) {
                for (LootPoolData.AspectEntry entry : mythicAspects) {
                    aspectWidgets.add(new AspectWidget(entry));
                }
                for (LootPoolData.AspectEntry entry : fabledAspects) {
                    aspectWidgets.add(new AspectWidget(entry));
                }
                for (LootPoolData.AspectEntry entry : legendaryAspects) {
                    aspectWidgets.add(new AspectWidget(entry));
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
            scoreWidget.setBounds((int) (x + (width - scoreWidth * 3) / 2f), y + textureWidth, (int) (scoreWidth * ui.getScaleFactorF()), 30);
            scoreWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);

            ctx.enableScissor(
                    (int) (x / ui.getScaleFactor()),
                    (int) ((y + 195) / ui.getScaleFactor()),
                    (int) ((x + width - 7) / ui.getScaleFactor()),
                    (int) ((y + height - 7) / ui.getScaleFactor()));

            float snapValue = 0.5f;
            float speed = 0.3f;
            float diff = (targetOffset - actualOffset);
            if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
            else actualOffset += diff * speed * tickDelta;

            int aspectY = y + textureWidth + 60 - (int) actualOffset;
            int aspectHeight = 50;
            int spacing = 5;

            int i = 0;
            for(AspectWidget aspectWidget : aspectWidgets) {
                aspectWidget.setBounds(x, aspectY, width, aspectHeight);
                aspectWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
                aspectY += aspectHeight + spacing;
                i++;
                if(i == 3 || i == 3 + fabledAspects.size()) {
                    aspectY += spacing * 4;
                    ui.drawLine(x + 15, aspectY - spacing * 2, x + width - 15, aspectY - spacing * 2, 3, CustomColor.fromHexString("997e69"));
                }
            }

            maxOffset = textureWidth + 20;

            ctx.disableScissor();
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

            public AspectWidget(LootPoolData.AspectEntry aspect) {
                super(0, 0, 0, 0);
                this.aspect = aspect;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                //ui.drawRect(x, y, width, height);

                int maxChars = Math.max(10, (width - 190) / 12);
                String displayName = aspect.name;
                boolean isFavorite = FavoriteAspectsData.INSTANCE.isFavorite(aspect.name);
                if (displayName.length() > maxChars) {
                    displayName = displayName.substring(0, maxChars - ((hovered || isFavorite) ? 5 : 3)) + "...";
                }

                boolean isMax = aspect.tierInfo.contains("MAX");

                CustomColor textColor = CustomColor.fromHexString("FFFFFF");
                String rarityColorCode = "";
                if(isMax) textColor = CommonColors.RAINBOW;
                else {
                    if(aspect.rarity.equalsIgnoreCase("mythic")) rarityColorCode = "§5";
                    else if(aspect.rarity.equalsIgnoreCase("fabled")) rarityColorCode = "§c";
                    else if(aspect.rarity.equalsIgnoreCase("legendary")) rarityColorCode = "§b";
                }
                ui.drawText(rarityColorCode + displayName + (isFavorite ? " §e⭐" : (hovered ? " §7☆" : "")), x + 70, y + 3 + height / 2f, textColor, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);


                ApiAspect apiAspect = findApiAspectByName(aspect.name);
                ItemStack flameItem = createAspectFlameIcon(apiAspect, isMax);
                if (!flameItem.isEmpty() && ui != null) {
                    int screenX = (int) ui.sx(x + 20);
                    int screenY = (int) ui.sy(y + 13); // Move flame up
                    float flameScale = 0.7f;
                    ctx.getMatrices().pushMatrix();
                    ctx.getMatrices().scale(flameScale, flameScale);
                    ctx.drawItem(flameItem, (int)(screenX / flameScale), (int)(screenY / flameScale));
                    ctx.getMatrices().popMatrix();
                }
            }

            @Override
            protected boolean onClick(int button) {
                FavoriteAspectsData.INSTANCE.toggleFavorite(aspect.name);
                return true;
            }
        }
    }
}
