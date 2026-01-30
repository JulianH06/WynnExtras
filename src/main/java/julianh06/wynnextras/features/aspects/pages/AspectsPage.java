package julianh06.wynnextras.features.aspects.pages;

import com.wynntils.utils.colors.CustomColor;
import julianh06.wynnextras.features.aspects.AspectScreen;
import julianh06.wynnextras.features.aspects.FavoriteAspectsData;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import julianh06.wynnextras.features.profileviewer.data.ApiAspect;
import julianh06.wynnextras.features.profileviewer.data.Aspect;
import julianh06.wynnextras.features.profileviewer.data.User;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public class AspectsPage extends PageWidget {
    private String searchedPlayer = ""; // Empty = show own aspects
    private User searchedPlayerData = null;
    private WynncraftApiHandler.FetchStatus searchedPlayerStatus = null;

    private User myAspectsData = null;
    private WynncraftApiHandler.FetchStatus myAspectsFetchStatus = null;
    private boolean fetchedMyAspects = false;
    private int myAspectsFetchGeneration = 0; // Track which fetch is current

    private String classFilter = "Warrior"; // Default to Warrior, no "All" mode
    private String maxFilter = "All"; // All, Max Only, Not Max, Favorites
    private boolean showOverview = true; // Toggle between class view and overview - default to overview

    private String searchInput = "";
    private boolean searchInputFocused = false;
    private int searchCursorPos = 0;
    // Recent searches are stored in FavoriteAspectsData for persistence
    private static final int MAX_RECENT_SEARCHES = 5;

    public AspectsPage(AspectScreen parent) {
        super(parent);
    }

    @Override
    public void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
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
                final int fetchGen = ++myAspectsFetchGeneration; // Track this request

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
            ui.drawCenteredText(loadingText, centerX, logicalH / 2);
            return;
        }

        switch (activeStatus) {
            case NOKEYSET:
                ui.drawCenteredText("§cYou need to set your API key to use this feature", centerX, logicalH / 2 - 30);
                ui.drawCenteredText("§7Run \"/we apikey\" for more information", centerX, logicalH / 2 + 30);
                return;
            case FORBIDDEN:
                String forbiddenText = searchedPlayer.isEmpty() ? "§cYou need to upload your aspects first" : "§c" + searchedPlayer + " hasn't uploaded their aspects";
                ui.drawCenteredText(forbiddenText, centerX, logicalH / 2 - 30);

                if (searchedPlayer.isEmpty()) {
                    // Check if hovering for underline effect
//                    boolean hoverForbidden = logicalMouseY >= logicalH / 2 + 10 && logicalMouseY <= logicalH / 2 + 50 &&
//                            logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;
//                    drawCenteredText(context, hoverForbidden ? "§e§nClick here to scan your aspects" : "§7Click here to scan your aspects", centerX, logicalH / 2 + 30);
                }
                return;
            case UNAUTHORIZED:
                ui.drawCenteredText("§cYour API key is not connected to your account", centerX, logicalH / 2 - 30);
                ui.drawCenteredText("§7Run \"/we apikey\" for more information", centerX, logicalH / 2 + 30);
                return;
            case NOT_FOUND:
                ui.drawCenteredText("§cNo aspect data found for your account", centerX, logicalH / 2 - 30);
                // Check if hovering for underline effect
//                boolean hoverNotFound = logicalMouseY >= logicalH / 2 + 10 && logicalMouseY <= logicalH / 2 + 50 &&
//                        logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;
//                drawCenteredText(context, hoverNotFound ? "§e§nClick here to scan your aspects" : "§7Click here to scan your aspects", centerX, logicalH / 2 + 30);
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

        // allAspects was already fetched at the start of this method

        // Show statistics - filtered by current class selection
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
                // Check if matches current class filter
                if (classFilter.equals("All") || apiAspect.getRequiredClass().equalsIgnoreCase(classFilter)) {
                    unlockedForClass++;

                    // Check if maxed
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

        // Dynamic title based on whose aspects we're viewing
        String title = searchedPlayer.isEmpty() ? "§6§lYOUR ASPECTS" : "§6§l" + searchedPlayer.toUpperCase() + "'S ASPECTS";
        ui.drawCenteredText(title, centerX, 60);

        // "Back to My Aspects" button if viewing another player (top right as styled button)
        if (!searchedPlayer.isEmpty()) {
            int buttonWidth = 260;
            int buttonHeight = 44;
            int buttonX = logicalW - buttonWidth - 40;
            int buttonY = 46;

            // Draw styled button
            //if (hoverBack) {
                ui.drawRect(buttonX, buttonY, buttonWidth, buttonHeight, CustomColor.fromInt(0xAA333333));
                ui.drawRect(buttonX, buttonY, buttonWidth, 2, CustomColor.fromInt(0xFFAAAA00));
                ui.drawRect(buttonX, buttonY + buttonHeight - 2, buttonWidth, 2, CustomColor.fromInt(0xFFAAAA00));
                ui.drawRect(buttonX, buttonY, 2, buttonHeight, CustomColor.fromInt(0xFFAAAA00));
                ui.drawRect(buttonX + buttonWidth - 2, buttonY, 2, buttonHeight, CustomColor.fromInt(0xFFAAAA00));
//            } else {
//                ui.drawRect(buttonX, buttonY, buttonWidth, buttonHeight, CustomColor.fromInt(0xAA1a1a1a));
//                ui.drawRect(buttonX, buttonY, buttonWidth, 2, CustomColor.fromInt(0xFF4e392d));
//                ui.drawRect(buttonX, buttonY + buttonHeight - 2, buttonWidth, 2, CustomColor.fromInt(0xFF4e392d));
//                ui.drawRect(buttonX, buttonY, 2, buttonHeight, CustomColor.fromInt(0xFF4e392d));
//                ui.drawRect(buttonX + buttonWidth - 2, buttonY, 2, buttonHeight, CustomColor.fromInt(0xFF4e392d));
//            }
            //String backText = hoverBack ? "§e§l< My Aspects" : "§7< My Aspects";
            //ui.drawCenteredText(backText, buttonX + buttonWidth / 2, buttonY + buttonHeight / 2);
        }

        // Draw class selector buttons at top (with Overview button) - moved down to give title space
        //drawClassSelectorButtons(context, logicalMouseX, logicalMouseY, centerX, 100);

        if (showOverview) {
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
                    int textWidthPixels = MinecraftClient.getInstance().textRenderer.getWidth(displayText);
                    int cursorOffset = textWidthPixels * 3; // Scale 3f used in drawLeftText
                    ui.drawRect(searchBoxX + 8 + cursorOffset, searchBoxY + 4, 2, searchBoxHeight - 8, CustomColor.fromInt(0xFFFFFFFF));
                }
            }

            // Show overview with progress bars - below search box with more space
            //drawOverview(context, allAspects, activeAspectsData.getAspects(), centerX, 240);

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

//                    if (hoverRecent) {
//                        ui.drawRect(searchBoxX, yOffset - 2, searchBoxWidth, lineHeight, 0x88FFFFFF);
//                    }

                    ui.drawText("§7" + search, searchBoxX + 8, yOffset);
                    yOffset += lineHeight;
                }
            }
        } else {
            // Draw max filter buttons below class selector
            //drawMaxFilterButtons(context, mouseX, mouseY, centerX, 180);

            // Show stats for current class with more space
            String className = classFilter;
            ui.drawCenteredText("§7" + className + " §8| §7Unlocked: §e" + unlockedForClass + "§7/§e" + totalForClass + " §8| §7Maxed: §a" + maxedForClass, centerX, 250);

            // Draw class-specific progress bars
            //int progressY = drawClassProgressBars(context, allAspects, activeAspectsData.getAspects(), centerX, 290);

            // Show single class in 2-column layout below progress bars
            //drawTwoColumnClassLayout(context, allAspects, activeAspectsData.getAspects(), progressY + 30, mouseX, mouseY);
        }

        // Render tooltip if hovering
//        if (hoveredMyAspect != null && hoveredMyAspectProgress != null) {
//            renderMyAspectTooltip(context, mouseX, mouseY);
//        }

        // Instructions above navigation - make it clickable (in logical coords)
//        int scanTextY = logicalH - 165;
//        boolean hoverScan = logicalMouseY >= scanTextY - 20 && logicalMouseY <= scanTextY + 40 &&
//                logicalMouseX >= centerX - 400 && logicalMouseX <= centerX + 400;
//
//        ui.drawCenteredText(hoverScan ? "§e§nClick here to scan your aspects" : "§7Click here to scan your aspects", centerX, scanTextY);
    }
}
