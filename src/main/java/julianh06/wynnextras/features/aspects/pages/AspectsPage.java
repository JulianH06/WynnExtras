package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CommonColors;
import com.wynntils.utils.colors.CustomColor;
import com.wynntils.utils.mc.McUtils;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import julianh06.wynnextras.config.WynnExtrasConfig;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.aspects.AspectUtils;
import julianh06.wynnextras.features.aspects.FavoriteAspectsData;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.features.profileviewer.data.Aspect;
import julianh06.wynnextras.features.profileviewer.data.User;
import julianh06.wynnextras.utils.UI.Widget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static julianh06.wynnextras.features.aspects.AspectUtils.createAspectFlameIcon;
import static julianh06.wynnextras.features.aspects.AspectUtils.findApiAspectByName;
import static julianh06.wynnextras.features.aspects.AspectUtils.romanToInt;
import static julianh06.wynnextras.features.aspects.AspectUtils.toItemStack;

public class AspectsPage extends PageWidget {
    static Identifier barBackground = Identifier.of("wynnextras", "textures/gui/aspectspage/xpbarbackground.png");
    static Identifier barBackground_dark = Identifier.of("wynnextras", "textures/gui/aspectspage/xpbarbackground_dark.png");
    static Identifier border = Identifier.of("wynnextras", "textures/gui/aspectspage/xpbarborder.png");
    static Identifier border_dark = Identifier.of("wynnextras", "textures/gui/aspectspage/xpbarborder_dark.png");
    static Identifier progress_warrior = Identifier.of("wynnextras", "textures/gui/aspectspage/warrior_progress.png");
    static Identifier progress_mage = Identifier.of("wynnextras", "textures/gui/aspectspage/mage_progress.png");
    static Identifier progress_archer = Identifier.of("wynnextras", "textures/gui/aspectspage/archer_progress.png");
    static Identifier progress_assassin = Identifier.of("wynnextras", "textures/gui/aspectspage/assassin_progress.png");
    static Identifier progress_shaman = Identifier.of("wynnextras", "textures/gui/aspectspage/shaman_progress.png");
    static Identifier progress_legendary = Identifier.of("wynnextras", "textures/gui/aspectspage/legendary_progress.png");
    static Identifier progress_fabled = Identifier.of("wynnextras", "textures/gui/aspectspage/fabled_progress.png");
    static Identifier progress_mythic = Identifier.of("wynnextras", "textures/gui/aspectspage/mythic_progress.png");
    static Identifier progress_white = Identifier.of("wynnextras", "textures/gui/aspectspage/white_progress.png");
    static Identifier progress_green = Identifier.of("wynnextras", "textures/gui/aspectspage/green_progress.png");

    private static String searchedPlayer = ""; // Empty = show own aspects
    private static User searchedPlayerData = null;
    private static WynncraftApiHandler.FetchStatus searchedPlayerStatus = null;

    private User myAspectsData = null;
    private WynncraftApiHandler.FetchStatus myAspectsFetchStatus = null;
    private boolean fetchedMyAspects = false;
    private int myAspectsFetchGeneration = 0;

    private static String classFilter = "Warrior";

    private static String searchInput = "";
    private static boolean searchInputFocused = false;
    private static int searchCursorPos = 0;
    private static final int MAX_RECENT_SEARCHES = 5;

    private enum Tab { Overview, Warrior, Shaman, Mage, Archer, Assassin }
    private static Tab currentTab = Tab.Overview;

    List<TabSwitchButton> tabSwitchButtons = new ArrayList<>();

    ProgressBarShowMaxWidget progressBarShowMaxWidget;
    private static boolean progressBarShowMax = true;

    ResetToOwnAspectsWidget resetToOwnAspectsWidget;
    private static List<Text> hoveredTooltip = new ArrayList<>();

    private static LootPoolWidget mythicAndFabledWidget;
    private static LootPoolWidget legendaryWidget;

    public AspectsPage(AspectScreen parent) {
        super(parent);

        for(Tab tab : Tab.values()) {
            tabSwitchButtons.add(new TabSwitchButton(tab));
        }

        progressBarShowMaxWidget = new ProgressBarShowMaxWidget();
        resetToOwnAspectsWidget = new ResetToOwnAspectsWidget();
        mythicAndFabledWidget = new LootPoolWidget();
        legendaryWidget = new LootPoolWidget();
    }

    @Override
    public void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
        hoveredTooltip.clear();
        int logicalW = (int) (width * ui.getScaleFactorF());
        int logicalH = (int) (height * ui.getScaleFactorF());
        int centerX = logicalW / 2;

        // First, check if aspect database is loaded - this must happen first
        List<ApiAspect> allAspects = new ArrayList<>(WynncraftApiHandler.fetchAllAspects());
        if (allAspects.isEmpty()) {
            ui.drawCenteredText("§eLoading aspect database...", centerX, logicalH / 2f);
            return;
        }

        // Determine which data source to use (own aspects or searched player's aspects)
        User activeAspectsData;
        WynncraftApiHandler.FetchStatus activeStatus;

        if (!searchedPlayer.isEmpty()) {
            // Viewing another player's aspects
            activeAspectsData = searchedPlayerData;
            activeStatus = searchedPlayerStatus;
        } else {
            // Viewing own aspects
            activeAspectsData = myAspectsData;
            activeStatus = myAspectsFetchStatus;

            // Fetch aspects if not already fetched
            if (!fetchedMyAspects && MinecraftClient.getInstance().player != null) {
                fetchedMyAspects = true;
                String playerUuid = MinecraftClient.getInstance().player.getUuidAsString();
                final int fetchGen = ++myAspectsFetchGeneration;

                WynncraftApiHandler.fetchPlayerAspectData(playerUuid, playerUuid)
                        .thenAccept(result -> {
                            // Only update if this is still the current request
                            if (fetchGen != myAspectsFetchGeneration) return;
                            if (result == null) return;
                            if (result.status() != null) {
                                if (result.status() == WynncraftApiHandler.FetchStatus.OK) {
                                    myAspectsData = result.user();
                                }
                                myAspectsFetchStatus = result.status();
                            }
                        })
                        .exceptionally(ex -> {
                            // Only log if this is still the current request
                            if (fetchGen == myAspectsFetchGeneration) {
                                System.err.println("Failed to fetch aspects: " + ex.getMessage());
                            }
                            return null;
                        });
            }
        }

        // Show loading or error states
        if (activeStatus == null) {
            String loadingText = searchedPlayer.isEmpty() ? "§eLoading your aspects..." : "§eLoading " + searchedPlayer + "'s aspects...";
            ui.drawCenteredText(loadingText, centerX, logicalH / 2f);
            return;
        }

        if(searchedPlayer.isEmpty()) {
            resetToOwnAspectsWidget.setBounds(0, 0, 0, 0);
        } else {
            resetToOwnAspectsWidget.setBounds(logicalW - 400, 0, 400, 50);
            resetToOwnAspectsWidget.draw(context, mouseX, mouseY, tickDelta, ui);
        }

        switch (activeStatus) {
            case NOKEYSET:
                ui.drawCenteredText("§cYou need to set your API key to use this feature", centerX, logicalH / 2f - 30);
                ui.drawCenteredText("§7Run \"/we apikey\" for more information", centerX, logicalH / 2f + 30);
                return;
            case FORBIDDEN:
                String forbiddenText = searchedPlayer.isEmpty() ? "§cYou need to upload your aspects first" : "§c" + searchedPlayer + " hasn't uploaded their aspects";
                ui.drawCenteredText(forbiddenText, centerX, logicalH / 2f - 30);
                return;
            case UNAUTHORIZED:
                ui.drawCenteredText("§cYour API key is not connected to your account", centerX, logicalH / 2 - 30);
                ui.drawCenteredText("§7Run \"/we apikey\" for more information", centerX, logicalH / 2 + 30);
                return;
            case NOT_FOUND:
                ui.drawCenteredText("§cNo aspect data found for your account", centerX, logicalH / 2 - 30);
                return;
            case SERVER_UNREACHABLE:
                ui.drawCenteredText("§cServer unreachable. Try again later.", centerX, logicalH / 2);
                return;
            case SERVER_ERROR:
                ui.drawCenteredText("§cServer error occurred!", centerX, logicalH / 2);
                return;
            case UNKNOWN_ERROR:
                String failedText = searchedPlayer.isEmpty() ? "§cFailed to load your aspects" : "§cFailed to load " + searchedPlayer + "'s aspects";
                ui.drawCenteredText(failedText, centerX, logicalH / 2);
                return;
        }

        if (activeAspectsData == null || activeAspectsData.getAspects() == null) {
            ui.drawCenteredText("§cNo aspect data found", centerX, logicalH / 2);
            return;
        }

        int totalForClass = (int) allAspects.stream()
                .filter(a -> classFilter.equals("All") || a.getRequiredClass().equalsIgnoreCase(classFilter))
                .count();

        int unlockedForClass = 0;
        int maxedForClass = 0;

        for (Aspect playerAspect : activeAspectsData.getAspects()) {
            ApiAspect apiAspect = allAspects.stream()
                    .filter(a -> a.getName().equals(playerAspect.getName()))
                    .findFirst()
                    .orElse(null);

            if (apiAspect != null) {
                if (classFilter.equals("All") || apiAspect.getRequiredClass().equalsIgnoreCase(classFilter)) {
                    unlockedForClass++;

                    int maxAmount = switch (apiAspect.getRarity().toLowerCase()) {
                        case "mythic" -> 15;
                        case "fabled" -> 75;
                        case "legendary" -> 150;
                        default -> 0;
                    };

                    if (playerAspect.getAmount() >= maxAmount) {
                        maxedForClass++;
                    }
                }
            }
        }

        String title = searchedPlayer.isEmpty() ? "§6§lYOUR ASPECTS" : "§6§l" + searchedPlayer.toUpperCase() + "'S ASPECTS";
        ui.drawCenteredText(title, centerX, 60);

        if (currentTab == Tab.Overview) {
            drawOverview(context, allAspects, activeAspectsData.getAspects(), centerX, 240);

            int toggleWidth = 190;
            int toggleHeight = 50;
            int toggleX = centerX + 400;
            int toggleY = 215;

            progressBarShowMaxWidget.setBounds(toggleX, toggleY, toggleWidth, toggleHeight);
            progressBarShowMaxWidget.draw(context, mouseX, mouseY, tickDelta, ui);
            // Player search input box (only on overview page) - BELOW the class buttons
            int searchBoxWidth = 500;
            int searchBoxHeight = 40;
            int searchBoxX = centerX - searchBoxWidth / 2;
            int searchBoxY = 160; // Below class buttons (which end at ~140)

            // Draw search box background using drawRect
            int boxColor = searchInputFocused ? 0xFFFFAA00 : 0xFFAAAAAA;
            ui.drawRect(searchBoxX - 2, searchBoxY - 2, searchBoxWidth + 4, searchBoxHeight + 4, CustomColor.fromInt(boxColor));
            ui.drawRect(searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, CustomColor.fromInt(0xFF000000));

            // Draw search text or placeholder
            if (searchInput.isEmpty() && !searchInputFocused) {
                ui.drawText("§7Search player...", searchBoxX + 8, searchBoxY + 6);
            } else {
                String displayText = searchInput;
                // Truncate if too long
                int maxChars = (searchBoxWidth - 20) / 12;
                if (displayText.length() > maxChars) {
                    displayText = displayText.substring(displayText.length() - maxChars);
                }
                ui.drawText(displayText, searchBoxX + 8, searchBoxY + 6);

                // Draw cursor if focused - put at end of text
                if (searchInputFocused) {
                    // drawLeftText uses scale 3f, so multiply text width by 3 to get logical units
                    int textWidthPixels = MinecraftClient.getInstance().textRenderer.getWidth(displayText.substring(0, searchCursorPos));
                    int cursorOffset = textWidthPixels * 3; // Scale 3f used in drawLeftText
                    ui.drawRect(searchBoxX + 8 + cursorOffset, searchBoxY + 4, 2, searchBoxHeight - 8, CustomColor.fromInt(0xFFFFFFFF));
                }
            }

            // Show recent searches dropdown when focused and search is empty (RENDER LAST so it's on top)
            if (searchInputFocused && searchInput.isEmpty() && !FavoriteAspectsData.INSTANCE.getRecentSearches().isEmpty()) {
                int dropdownY = searchBoxY + searchBoxHeight + 4;
                int lineHeight = 28;
                int dropdownHeight = Math.min(FavoriteAspectsData.INSTANCE.getRecentSearches().size(), MAX_RECENT_SEARCHES) * lineHeight + 8;

                // Draw dropdown background
                ui.drawRect(searchBoxX - 2, dropdownY - 2, searchBoxWidth + 4, dropdownHeight + 4, CustomColor.fromInt(0xFFAAAAAA));
                ui.drawRect(searchBoxX, dropdownY, searchBoxWidth, dropdownHeight, CustomColor.fromInt(0xFF000000));

                // Draw recent searches
                int yOffset = dropdownY + 4;
                for (int i = 0; i < Math.min(FavoriteAspectsData.INSTANCE.getRecentSearches().size(), MAX_RECENT_SEARCHES); i++) {
                    String search = FavoriteAspectsData.INSTANCE.getRecentSearches().get(i);
                    boolean hoverRecent = mouseX * ui.getScaleFactorF() >= searchBoxX && mouseX * ui.getScaleFactorF() <= searchBoxX + searchBoxWidth &&
                            mouseY * ui.getScaleFactorF() >= yOffset && mouseY * ui.getScaleFactorF() <= yOffset + lineHeight;

                    if (hoverRecent) {
                        ui.drawRect(searchBoxX, yOffset - 2, searchBoxWidth, lineHeight, CustomColor.fromInt(0x88FFFFFF));
                    }

                    ui.drawText("§7" + search, searchBoxX + 8, yOffset);
                    yOffset += lineHeight;
                }
            }
        } else {
            progressBarShowMaxWidget.setBounds(0, 0, 0, 0);

            String className = classFilter;
            ui.drawCenteredText("§7" + className + " §8| §7Unlocked: §e" + unlockedForClass + "§7/§e" + totalForClass + " §8| §7Maxed: §a" + maxedForClass, centerX, 170);

            int toggleWidth = 170;
            int toggleHeight = 50;
            int toggleX = centerX + 380;
            int toggleY = 150;

            progressBarShowMaxWidget.setBounds(toggleX, toggleY, toggleWidth, toggleHeight);
            progressBarShowMaxWidget.draw(context, mouseX, mouseY, tickDelta, ui);

            int aspectY = drawClassProgressBars(context, allAspects, activeAspectsData.getAspects(), centerX, 200);

            mythicAndFabledWidget.setBounds(centerX - 660, aspectY, 625, logicalH - aspectY - 120);

            List<Aspect> mythicAndFabledAspects = new ArrayList<>();
            List<Aspect> legendaryAspects = new ArrayList<>();

            for(Aspect aspect : activeAspectsData.getAspects()) {
                for(ApiAspect apiAspect : allAspects) {
                    if(!apiAspect.getRequiredClass().equalsIgnoreCase(currentTab.name())) continue;
                    if(!apiAspect.getName().equalsIgnoreCase(aspect.getName())) continue;
                    if(apiAspect.getRarity().equalsIgnoreCase("legendary")) legendaryAspects.add(aspect);
                    else mythicAndFabledAspects.add(aspect);
                }
            }

            mythicAndFabledWidget.aspectEntries = mythicAndFabledAspects;
            mythicAndFabledWidget.draw(context, mouseX, mouseY, tickDelta, ui);

            legendaryWidget.setBounds(centerX + 25, aspectY, 625, logicalH - aspectY - 120);
            legendaryWidget.aspectEntries = legendaryAspects;
            legendaryWidget.draw(context, mouseX, mouseY, tickDelta, ui);

            ui.drawCenteredText("§5Mythic §7& §cFabled", centerX - 660 + 625 / 2f, aspectY + 40);
            ui.drawCenteredText("§bLegendary", centerX + 25 + 625 / 2f, aspectY + 40);
        }

        int buttonWidth = 170;
        int buttonHeight = 50;
        int spacing = 16;
        int totalWidth = (buttonWidth * Tab.values().length) + (spacing * (Tab.values().length - 1));
        int x = centerX - totalWidth / 2;
        for(TabSwitchButton tabSwitchButton : tabSwitchButtons) {
            tabSwitchButton.setBounds(x, 90, buttonWidth, buttonHeight);
            tabSwitchButton.draw(context, mouseX, mouseY, tickDelta, ui);
            x += buttonWidth + spacing;
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
        if(hoveredTooltip.isEmpty()) return;
        ctx.drawTooltip(MinecraftClient.getInstance().textRenderer, hoveredTooltip, Optional.empty(), mouseX - 5, mouseY + 20);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchInputFocused) {
            // Insert character at cursor position
            searchInput = searchInput.substring(0, searchCursorPos) + chr + searchInput.substring(searchCursorPos);
            searchCursorPos++;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchInputFocused) {
            if (keyCode == 259) { // Backspace
                if (searchCursorPos > 0) {
                    searchInput = searchInput.substring(0, searchCursorPos - 1) + searchInput.substring(searchCursorPos);
                    searchCursorPos--;
                }
                return true;
            } else if (keyCode == 261) { // Delete
                if (searchCursorPos < searchInput.length()) {
                    searchInput = searchInput.substring(0, searchCursorPos) + searchInput.substring(searchCursorPos + 1);
                }
                return true;
            } else if (keyCode == 263) {
                if (searchCursorPos > 0) {
                    searchCursorPos--;
                }
                return true;
            } else if (keyCode == 262) {
                if (searchCursorPos < searchInput.length()) {
                    searchCursorPos++;
                }
                return true;
            } else if (keyCode == 257 || keyCode == 335) {
                if (!searchInput.isEmpty()) {
                    performPlayerSearch(searchInput);
                    searchInputFocused = false;
                }
                return true;
            } else if (keyCode == 256) { // Escape
                searchInputFocused = false;
                return false;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if(mythicAndFabledWidget.mouseScrolled(mx, my, delta)) return true;
        if(legendaryWidget.mouseScrolled(mx, my, delta)) return true;

        return false;
    }

    private int drawClassProgressBars(DrawContext context, List<ApiAspect> allAspects, List<Aspect> playerAspects, int centerX, int startY) {
        int logicalW = (int) (width * ui.getScaleFactorF());
        int barWidth = Math.min(600, logicalW - 600);
        int barX = centerX - barWidth / 2;

        String className = classFilter;

        int allTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className)).count();
        int allCount = progressBarShowMax
                ? countMaxedForClassAndRarity(allAspects, playerAspects, className, null)
                : countUnlockedForClassAndRarity(allAspects, playerAspects, className, null);

        int mythicTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className) && a.getRarity().equalsIgnoreCase("mythic")).count();
        int mythicCount = progressBarShowMax
                ? countMaxedForClassAndRarity(allAspects, playerAspects, className, "mythic")
                : countUnlockedForClassAndRarity(allAspects, playerAspects, className, "mythic");

        int fabledTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className) && a.getRarity().equalsIgnoreCase("fabled")).count();
        int fabledCount = progressBarShowMax
                ? countMaxedForClassAndRarity(allAspects, playerAspects, className, "fabled")
                : countUnlockedForClassAndRarity(allAspects, playerAspects, className, "fabled");

        int legendaryTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className) && a.getRarity().equalsIgnoreCase("legendary")).count();
        int legendaryCount = progressBarShowMax
                ? countMaxedForClassAndRarity(allAspects, playerAspects, className, "legendary")
                : countUnlockedForClassAndRarity(allAspects, playerAspects, className, "legendary");

        int y = startY;

        boolean allMax = allCount == allTotal;
        ui.drawText("§6§lAll " + className, barX - 350, y + 20);
        ui.drawText("§7" + allCount + "§8/§7" + allTotal, barX + barWidth + 20, y + 20);
        ui.drawProgressBar(barX, y, barWidth, 60, 5, (float) allCount / allTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, allMax ? progress_white : progress_green, context, allMax);
        y += 70;

        boolean mythicMax = mythicCount == mythicTotal;
        ui.drawText("§5Mythic " + className, barX - 350, y + 20);
        ui.drawText("§7" + mythicCount + "§8/§7" + mythicTotal, barX + barWidth + 20, y + 20);
        ui.drawProgressBar(barX, y, barWidth, 60, 5, (float) mythicCount / mythicTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, mythicMax ? progress_white : progress_green, context, mythicMax);
        y += 70;

        boolean fabledMax = fabledCount == fabledTotal;
        ui.drawText("§cFabled " + className, barX - 350, y + 20);
        ui.drawText("§7" + fabledCount + "§8/§7" + fabledTotal, barX + barWidth + 20, y + 20);
        ui.drawProgressBar(barX, y, barWidth, 60, 5, (float) fabledCount / fabledTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, fabledMax ? progress_white : progress_green, context, fabledMax);
        y += 70;

        boolean legendaryMax = legendaryCount == legendaryTotal;
        ui.drawText("§bLegendary " + className, barX - 350, y + 20);
        ui.drawText("§7" + legendaryCount + "§8/§7" + legendaryTotal, barX + barWidth + 20, y + 20);
        ui.drawProgressBar(barX, y, barWidth, 60, 5, (float) legendaryCount / legendaryTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, legendaryMax ? progress_white : progress_green, context, legendaryMax);
        y += 70;

        return y;
    }

    private void drawOverview(DrawContext context, List<ApiAspect> allAspects, List<Aspect> playerAspects, int centerX, int startY) {
        int logicalW = (int) (width * ui.getScaleFactorF());

        ui.drawCenteredText("§6§lOVERVIEW", centerX, startY);

        int barStartY = startY + 30;
        int barWidth = Math.min(800, logicalW - 600);
        int barX = centerX - barWidth / 2;

        int totalAspects = allAspects.size();
        int totalCount = progressBarShowMax ? countMaxedAspects(allAspects, playerAspects) : playerAspects.size();

        int mythicTotal = (int) allAspects.stream().filter(a -> a.getRarity().equalsIgnoreCase("mythic")).count();
        int mythicCount = progressBarShowMax ? countMaxedByRarity(allAspects, playerAspects, "mythic") : countUnlockedByRarity(allAspects, playerAspects, "mythic");

        int fabledTotal = (int) allAspects.stream().filter(a -> a.getRarity().equalsIgnoreCase("fabled")).count();
        int fabledCount = progressBarShowMax ? countMaxedByRarity(allAspects, playerAspects, "fabled") : countUnlockedByRarity(allAspects, playerAspects, "fabled");

        int legendaryTotal = (int) allAspects.stream().filter(a -> a.getRarity().equalsIgnoreCase("legendary")).count();
        int legendaryCount = progressBarShowMax ? countMaxedByRarity(allAspects, playerAspects, "legendary") : countUnlockedByRarity(allAspects, playerAspects, "legendary");

        String suffix = progressBarShowMax ? " Max" : " unlocked";

        ui.drawText("§6§lTotal" + suffix, barX - 350, barStartY + 20);
        ui.drawText("§7" + totalCount + "§8/§7" + totalAspects, barX + barWidth + 20, barStartY + 20);
        boolean totalMax = totalCount == totalAspects;
        ui.drawProgressBar(barX, barStartY, barWidth, 60, 5, (float) totalCount / totalAspects, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, totalMax ? progress_white : progress_green, context, totalMax);
        barStartY += 70;

        ui.drawText("§5Mythic" + suffix, barX - 350, barStartY + 20);
        ui.drawText("§7" + mythicCount + "§8/§7" + mythicTotal, barX + barWidth + 20, barStartY + 20);
        boolean mythicMax = mythicCount == mythicTotal;
        ui.drawProgressBar(barX, barStartY, barWidth, 60, 5, (float) mythicCount / mythicTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, mythicMax ? progress_white : progress_mythic, context, mythicMax);
        barStartY += 70;

        ui.drawText("§cFabled" + suffix, barX - 350, barStartY + 20);
        ui.drawText("§7" + fabledCount + "§8/§7" + fabledTotal, barX + barWidth + 20, barStartY + 20);
        boolean fabledMax = fabledCount == fabledTotal;
        ui.drawProgressBar(barX, barStartY, barWidth, 60, 5, (float) fabledCount / fabledTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, fabledMax ? progress_white : progress_fabled, context, fabledMax);
        barStartY += 70;

        ui.drawText("§bLegendary" + suffix, barX - 350, barStartY + 20);
        ui.drawText("§7" + legendaryCount + "§8/§7" + legendaryTotal, barX + barWidth + 20, barStartY + 20);
        boolean legendaryMax = legendaryCount == legendaryTotal;
        ui.drawProgressBar(barX, barStartY, barWidth, 60, 5, (float) legendaryCount / legendaryTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, legendaryMax ? progress_white : progress_legendary, context, legendaryMax);
        barStartY += 90;

        ui.drawCenteredText("§e§lPER CLASS", centerX, barStartY);
        barStartY += 20;

        String[] classes = {"Warrior", "Shaman", "Mage", "Archer", "Assassin"};
        for (String className : classes) {
            int classTotal = (int) allAspects.stream().filter(a -> a.getRequiredClass().equalsIgnoreCase(className)).count();

            int classCount = progressBarShowMax
                    ? countMaxedForClassAndRarity(allAspects, playerAspects, className, null)
                    : countUnlockedForClassAndRarity(allAspects, playerAspects, className, null);

            CustomColor classColor = switch (className) {
                case "Warrior" -> CustomColor.fromHexString("f15e3f");
                case "Shaman" -> CustomColor.fromHexString("41bee7");
                case "Mage" -> CustomColor.fromHexString("fce635");
                case "Archer" -> CustomColor.fromHexString("ca1478");
                case "Assassin" -> CustomColor.fromHexString("bd40d6");
                default -> CustomColor.fromHexString("FFFFFF");
            };

            Identifier progressTexture = switch (className) {
                case "Warrior" -> progress_warrior;
                case "Shaman" -> progress_shaman;
                case "Mage" -> progress_mage;
                case "Archer" -> progress_archer;
                case "Assassin" -> progress_assassin;
                default -> progress_white;
            };

            ui.drawText(className + suffix, barX - 350, barStartY + 20, classColor);
            ui.drawText("§7" + classCount + "§8/§7" + classTotal, barX + barWidth + 20, barStartY + 20);
            boolean classMax = fabledCount == fabledTotal;
            ui.drawProgressBar(barX, barStartY, barWidth, 60, 5, (float) classCount / classTotal, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? border_dark : border, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode ? barBackground_dark : barBackground, classMax ? progress_white : progressTexture, context, classMax);
            barStartY += 70;
        }
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if(mythicAndFabledWidget.mouseReleased(mx, my, button)) return true;
        if(legendaryWidget.mouseReleased(mx, my, button)) return true;
        return false;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if(resetToOwnAspectsWidget.mouseClicked(mx, my, button)) return true;

        if(currentTab == Tab.Overview) {
            int logicalW = (int) (width * ui.getScaleFactorF());
            int centerX = logicalW / 2;
            int searchBoxWidth = 500;
            int searchBoxHeight = 40;
            int searchBoxX = centerX - searchBoxWidth / 2;
            int searchBoxY = 160;

            float logicalMX = (float) (mx * ui.getScaleFactorF());
            float logicalMY = (float) (my * ui.getScaleFactorF());

            // Right-click to clear search box
            if (button == 1 && logicalMX >= searchBoxX && logicalMX <= searchBoxX + searchBoxWidth &&
                    logicalMY >= searchBoxY && logicalMY <= searchBoxY + searchBoxHeight) {
                searchInput = "";
                searchCursorPos = 0;
                return true;
            }

            // Click on recent searches dropdown
            if (button == 0 && searchInputFocused && searchInput.isEmpty() && !FavoriteAspectsData.INSTANCE.getRecentSearches().isEmpty()) {
                int dropdownY = searchBoxY + searchBoxHeight + 4;
                int lineHeight = 28;
                int yOffset = dropdownY + 4;

                for (int i = 0; i < Math.min(FavoriteAspectsData.INSTANCE.getRecentSearches().size(), MAX_RECENT_SEARCHES); i++) {
                    if (logicalMX >= searchBoxX && logicalMX <= searchBoxX + searchBoxWidth &&
                            logicalMY >= yOffset && logicalMY <= yOffset + lineHeight) {
                        // Clicked on this recent search
                        String selectedSearch = FavoriteAspectsData.INSTANCE.getRecentSearches().get(i);
                        searchInput = selectedSearch;
                        searchCursorPos = selectedSearch.length();
                        performPlayerSearch(selectedSearch);
                        searchInputFocused = false;
                        McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                        return true;
                    }
                    yOffset += lineHeight;
                }
            }

            // Left-click on search box
            if (button == 0 && logicalMX >= searchBoxX && logicalMX <= searchBoxX + searchBoxWidth &&
                    logicalMY >= searchBoxY && logicalMY <= searchBoxY + searchBoxHeight) {
                searchInputFocused = true;
                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
                return true;
            } else {
                searchInputFocused = false;
            }
        } else {
            if(mythicAndFabledWidget.mouseClicked(mx, my, button)) return true;
            if(legendaryWidget.mouseClicked(mx, my, button)) return true;
        }
        
        for(TabSwitchButton tabSwitchButton : tabSwitchButtons) {
            if(tabSwitchButton.mouseClicked(mx, my, button)) return true;
        }

        if(progressBarShowMaxWidget.mouseClicked(mx, my, button)) return true;

        return false;
    }

    public static void performPlayerSearch(String playerName) {
        searchedPlayer = playerName;
        searchedPlayerData = null;
        searchedPlayerStatus = null; // null = loading

        String requestingUUID = McUtils.player() != null ? McUtils.player().getUuidAsString() : null;
        final String expectedPlayer = playerName; // Capture to detect race condition

        // First convert username to UUID
        WynncraftApiHandler.fetchUUID(playerName).thenCompose(rawUUID -> {
            // Check if search target changed while we were fetching UUID
            if (!expectedPlayer.equals(searchedPlayer)) {
                return CompletableFuture.completedFuture(null);
            }
            if (rawUUID == null) {
                searchedPlayerStatus = WynncraftApiHandler.FetchStatus.NOT_FOUND;
                return CompletableFuture.completedFuture(null);
            }

            // Format UUID and fetch aspect data
            String formattedUUID = WynncraftApiHandler.formatUUID(rawUUID);
            return WynncraftApiHandler.fetchPlayerAspectData(formattedUUID, requestingUUID);
        }).thenAccept(result -> {
            // Only update if we're still searching for the same player
            if (!expectedPlayer.equals(searchedPlayer)) return;
            if (result == null) return;
            if (result.status() != null) {
                if (result.status() == WynncraftApiHandler.FetchStatus.OK) {
                    searchedPlayerData = result.user();
                    // Add to recent searches on success
                    addToRecentSearches(playerName);
                }
                searchedPlayerStatus = result.status();
            }
        }).exceptionally(ex -> {
            // Only log/update if we're still searching for the same player
            if (expectedPlayer.equals(searchedPlayer)) {
                System.err.println("[WynnExtras] Error fetching aspects for " + playerName + ": " + ex.getMessage());
                searchedPlayerStatus = WynncraftApiHandler.FetchStatus.UNKNOWN_ERROR;
            }
            return null;
        });
    }

    private static void addToRecentSearches(String playerName) {
        for(String name : FavoriteAspectsData.INSTANCE.getRecentSearches()) {
            if(name.equalsIgnoreCase(playerName)) return;
        }
        FavoriteAspectsData.INSTANCE.addRecentSearch(playerName);
    }

    private int countMaxedAspects(List<ApiAspect> allAspects, List<Aspect> playerAspects) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;

                int maxAmount = switch (apiAspect.getRarity().toLowerCase()) {
                    case "mythic" -> 15;
                    case "fabled" -> 75;
                    case "legendary" -> 150;
                    default -> 0;
                };

                if (playerAspect.getAmount() >= maxAmount) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private int countMaxedByRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (!apiAspect.getRarity().equalsIgnoreCase(rarity)) continue;

                int maxAmount = switch (rarity.toLowerCase()) {
                    case "mythic" -> 15;
                    case "fabled" -> 75;
                    case "legendary" -> 150;
                    default -> 0;
                };

                if (playerAspect.getAmount() >= maxAmount) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private int countUnlockedByRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (apiAspect.getRarity().equalsIgnoreCase(rarity)) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private int countMaxedForClassAndRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String className, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (!apiAspect.getRequiredClass().equalsIgnoreCase(className)) continue;
                if (rarity != null && !apiAspect.getRarity().equalsIgnoreCase(rarity)) continue;

                int maxAmount = switch (apiAspect.getRarity().toLowerCase()) {
                    case "mythic" -> 15;
                    case "fabled" -> 75;
                    case "legendary" -> 150;
                    default -> 0;
                };

                if (playerAspect.getAmount() >= maxAmount) {
                    count++;
                }
                break;
            }
        }
        return count;
    }

    private int countUnlockedForClassAndRarity(List<ApiAspect> allAspects, List<Aspect> playerAspects, String className, String rarity) {
        int count = 0;
        for (Aspect playerAspect : playerAspects) {
            for (ApiAspect apiAspect : allAspects) {
                if (!apiAspect.getName().equals(playerAspect.getName())) continue;
                if (!apiAspect.getRequiredClass().equalsIgnoreCase(className)) continue;
                if (rarity != null && !apiAspect.getRarity().equalsIgnoreCase(rarity)) continue;
                count++;
                break;
            }
        }
        return count;
    }

    private static class TabSwitchButton extends Widget {
        final Tab tab;

        public TabSwitchButton(Tab tab) {
            this.tab = tab;
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);

            CustomColor textColor = CustomColor.fromHexString("FFFFFF");
            if(currentTab == tab) textColor = CustomColor.fromHexString("fca800");
            ui.drawCenteredText(tab.name(), x + width / 2f, y + height / 2f, textColor);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            currentTab = tab;
            if(tab != Tab.Overview) classFilter = tab.name();
            return true;
        }
    }

    private static class ProgressBarShowMaxWidget extends Widget {
        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);

            String modeText = progressBarShowMax ? "§a§lMax" : "§e§lUnlocked";
            ui.drawCenteredText(modeText, x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            progressBarShowMax = !progressBarShowMax;
            return true;
        }
    }

    private static class ResetToOwnAspectsWidget extends Widget {

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            ui.drawButton(x, y, width, height, 13, hovered, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);
            ui.drawCenteredText("Back to My Aspects", x + width / 2f, y + height / 2f);
        }

        @Override
        protected boolean onClick(int button) {
            searchedPlayer = "";
            searchedPlayerData = null;
            searchedPlayerStatus = null;
            searchInput = "";
            searchCursorPos = 0;
            McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
            return true;
        }
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
        List<LootPoolWidget.AspectWidget> aspectWidgets = new ArrayList<>();
        public List<Aspect> aspectEntries = new ArrayList<>();

        float targetOffset = 0;
        float actualOffset = 0;
        float maxOffset = 999;

        public LootPoolWidget() {
            super(0, 0, 0, 0);
            scrollBarWidget = new LootPoolWidget.ScrollBarWidget(this);
        }

        @Override
        protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
            int topHeight = 80;

            if(WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode) {
                ui.drawNineSlice(x, y, width, topHeight, 33, ltopd, rtopd, ttopd, btopd, tltopd, trtopd, bltopd, brtopd, CustomColor.fromHexString("2c2d2f"));

                ui.drawNineSlice(x,y + topHeight, width, height - topHeight, 33, ld, rd, td, bd, tld, trd, bld, brd, CustomColor.fromHexString("444448"));
            } else {
                ui.drawNineSlice(x,
                        y, width,
                        topHeight, 33, ltop, rtop, ttop, btop, tltop, trtop, bltop, brtop, CustomColor.fromHexString("81644b"));

                ui.drawNineSlice(x,y + topHeight, width, height - topHeight, 33, l, r, t, b, tl, tr, bl, br, CustomColor.fromHexString("cca76f"));
            }

            List<Aspect> mythicAspects = aspectEntries.stream().filter(a -> a.getRarity().equalsIgnoreCase("mythic")).toList();
            List<Aspect> fabledAspects = aspectEntries.stream().filter(a -> a.getRarity().equalsIgnoreCase("fabled")).toList();
            List<Aspect> legendaryAspects = aspectEntries.stream().filter(a -> a.getRarity().equalsIgnoreCase("legendary")).toList();

            aspectWidgets.clear();

            for (Aspect entry : mythicAspects) {
//                    if(hideMax && entry.tierInfo.contains("MAX")) continue;
//                    if(onlyFavorites && !FavoriteAspectsData.INSTANCE.isFavorite(entry.name)) continue;
                aspectWidgets.add(new LootPoolWidget.AspectWidget(entry, this));
            }
            for (Aspect entry : fabledAspects) {
                //if(hideMax && entry.tierInfo.contains("MAX")) continue;
                //if(onlyFavorites && !FavoriteAspectsData.INSTANCE.isFavorite(entry.name)) continue;
                aspectWidgets.add(new LootPoolWidget.AspectWidget(entry, this));
            }
            for (Aspect entry : legendaryAspects) {
                //if(hideMax && entry.tierInfo.contains("MAX")) continue;
                //if(onlyFavorites && !FavoriteAspectsData.INSTANCE.isFavorite(entry.name)) continue;
                aspectWidgets.add(new LootPoolWidget.AspectWidget(entry, this));
            }

            ctx.enableScissor(
                    (int) (x / ui.getScaleFactor()),
                    (int) ((y + 70) / ui.getScaleFactor()),
                    (int) ((x + width - 7) / ui.getScaleFactor()),
                    (int) ((y + height - 20) / ui.getScaleFactor()));

            float snapValue = 0.5f;
            float speed = 0.3f;
            float diff = (targetOffset - actualOffset);
            if(Math.abs(diff) < snapValue || !WynnExtrasConfig.INSTANCE.smoothScrollToggle) actualOffset = targetOffset;
            else actualOffset += diff * speed * tickDelta;

            int aspectY = y + 80 - (int) actualOffset;
            int aspectHeight = 50;
            int spacing = 5;

            float contentHeight = 0;

            for (int i = 0; i < aspectWidgets.size(); i++) {
                contentHeight += aspectHeight;
                contentHeight += spacing;

                LootPoolWidget.AspectWidget aspectWidget = aspectWidgets.get(i);

                aspectWidget.setBounds(x, aspectY, width, aspectHeight);
                aspectWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);

                aspectY += aspectHeight + spacing;

                boolean isLastOfRarity =
                        i + 1 < aspectWidgets.size() &&
                                !aspectWidgets.get(i + 1).aspect.getRarity()
                                        .equalsIgnoreCase(aspectWidget.aspect.getRarity());

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

            int listTop = y + 70;
            int listBottom = y + height - 40;
            float visibleHeight = listBottom - listTop;

            maxOffset = Math.max(contentHeight - visibleHeight, 0);

            if(targetOffset > maxOffset) {
                targetOffset = maxOffset;
            }

            ctx.disableScissor();
            if(maxOffset == 0) {
                scrollBarWidget.setBounds(0, 0, 0, 0);
            } else {
                scrollBarWidget.setBounds(x + width, y + 100, 25, height - 100);
                scrollBarWidget.draw(ctx, mouseX, mouseY, tickDelta, ui);
            }
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
            for(LootPoolWidget.AspectWidget aspectWidget : aspectWidgets) {
                if(aspectWidget.isHovered()) {
                    aspectWidget.onClick(button);
                    return true;
                }
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
                ui.drawSliderBackground(x, y, width, height, 5, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);

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
                    ui.drawButton(x, y, width, height, 5, hovered || isHold, WynnExtrasConfig.INSTANCE.lootPoolPagesDarkMode);
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

        private static class AspectWidget extends Widget {
            final Aspect aspect;
            final LootPoolWidget parent;

            public AspectWidget(Aspect aspect, LootPoolWidget parent) {
                super(0, 0, 0, 0);
                this.aspect = aspect;
                this.parent = parent;
            }

            @Override
            protected void drawContent(DrawContext ctx, int mouseX, int mouseY, float tickDelta) {
                //ui.drawRect(x, y, width, height);

                int maxChars = Math.max(10, (width - 220) / 12);
                String displayName = aspect.getName();
                boolean isFavorite = FavoriteAspectsData.INSTANCE.isFavorite(aspect.getName());
                if (displayName.length() + ((isFavorite || hovered) ? 5 : 0) > maxChars) {
                    displayName = displayName.substring(0, maxChars - ((hovered || isFavorite) ? 5 : 3)) + "...";
                }

                String tierInfo = AspectUtils.convertAmountToTierInfo(aspect.getAmount(), aspect.getRarity());

                boolean isMax = tierInfo.contains("MAX");
                CustomColor textColor = CustomColor.fromHexString("FFFFFF");
                String rarityColorCode = "";
                if(isMax) {

                    textColor = CommonColors.RAINBOW;
                } else {
                    if(aspect.getRarity().equalsIgnoreCase("mythic")) rarityColorCode = "§5";
                    else if(aspect.getRarity().equalsIgnoreCase("fabled")) rarityColorCode = "§c";
                    else if(aspect.getRarity().equalsIgnoreCase("legendary")) rarityColorCode = "§b";
                }

                ui.drawText(rarityColorCode + displayName + (isFavorite ? " §e⭐" : ((hovered && parent.isHovered()) ? " §7☆" : "")), x + 90, y + 3 + height / 2f, textColor, HorizontalAlignment.LEFT, VerticalAlignment.MIDDLE, 3f);


                ApiAspect apiAspect = findApiAspectByName(aspect.getName());
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

                if(hovered && parent.isHovered()) {
                    List<Text> tooltip = new ArrayList<>();
                    if(apiAspect == null) return;
                    int tier = 0;
                    if(isMax) {
                        if(aspect.getRarity().equalsIgnoreCase("legendary")) tier = 4;
                        else tier = 3;
                    }

                    Pattern pattern = Pattern.compile("Tier ([IVXLCDM]+)");
                    Matcher matcher = pattern.matcher(tierInfo);

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

                    tooltip.set(0, Text.of(name + " §7" + tierInfo));

                    hoveredTooltip = tooltip;
                }
            }

            @Override
            protected boolean onClick(int button) {
                if(!parent.isHovered()) return false;
                McUtils.playSoundUI(SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK);
                FavoriteAspectsData.INSTANCE.toggleFavorite(aspect.getName());
                return true;
            }
        }
    }
}