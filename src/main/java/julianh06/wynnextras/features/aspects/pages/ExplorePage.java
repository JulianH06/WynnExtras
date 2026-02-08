//package julianh06.wynnextras.features.aspects.pages;
//
//import com.wynntils.utils.colors.CustomColor;
//import com.wynntils.utils.mc.McUtils;
//import julianh06.wynnextras.features.aspects.AspectScreen;
//import julianh06.wynnextras.features.aspects.oldPages.AspectScreenHost;
//import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
//import julianh06.wynnextras.features.profileviewer.data.PlayerListEntry;
//import net.minecraft.client.gui.DrawContext;
//import net.minecraft.sound.SoundEvents;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ExplorePage extends PageWidget {
//    public ExplorePage(AspectScreen parent) {
//        super(parent);
//    }
//
//    private List<PlayerListEntry> playerList = null;
//    private boolean fetchedPlayerList = false;
//    private int sortMode = 0; // 0 = Most Aspects, 1 = Username
//
//    // Layout constants
//    private static final int FILTER_BUTTON_WIDTH = 300;
//    private static final int FILTER_BUTTON_HEIGHT = 50;
//    private static final int FILTER_SPACING = 30;
//    private static final int FILTER_Y = 125;
//
//    private static final int COLUMN_COUNT = 3;
//    private static final int ENTRY_WIDTH = 280;
//    private static final int ENTRY_HEIGHT = 85;
//    private static final int ENTRY_SPACING = 30;
//    private static final int START_Y = 190;
//    private static final int PER_PAGE = 15; // 5 rows × 3 columns
//
//    @Override
//    public void drawContent(DrawContext context, int mouseX, int mouseY, float tickDelta) {
//        if (!fetchedPlayerList) {
//            fetchedPlayerList = true;
//            WynncraftApiHandler.fetchPlayerList().thenAccept(result -> {
//                playerList = result;
//            });
//        }
//
//        int logicalW = (int) (width * ui.getScaleFactorF());
//        int logicalH = (int) (height * ui.getScaleFactorF());
//        int centerX = logicalW / 2;
//
//        // Title
//        ui.drawCenteredText("§6§lEXPLORE PLAYERS", centerX, 60);
//        ui.drawCenteredText("§7Browse all players who have scanned their aspects", centerX, 110);
//
//        // Trigger fetch if needed
//        if (!fetchedPlayerList) {
//            fetchedPlayerList = true;
//            WynncraftApiHandler.fetchPlayerList().thenAccept(result -> {
//                playerList = result;
//            });
//        }
//
//        // Show loading or data
//        if (playerList == null) {
//            ui.drawCenteredText("§eLoading players...", centerX, 200);
//            return;
//        }
//
//        if (playerList.isEmpty()) {
//            ui.drawCenteredText("§cNo players found", centerX, 200);
//            return;
//        }
//
//        // Render sort filter buttons
//        renderFilterButtons(context, logicalW, mouseX, mouseY);
//
//        // Sort and render player list
//        List<PlayerListEntry> sortedList = getSortedList();
//
//        int totalWidth = (ENTRY_WIDTH * COLUMN_COUNT) + (ENTRY_SPACING * (COLUMN_COUNT - 1));
//        int startX = (logicalW - totalWidth) / 2;
//
//        int maxEntries = Math.min(sortedList.size(), PER_PAGE);
//
//        for (int i = 0; i < maxEntries; i++) {
//            PlayerListEntry player = sortedList.get(i);
//            renderPlayerEntry(context, player, i, startX, mouseX, mouseY);
//        }
//
//        // Instructions above navigation
//        ui.drawCenteredText("§7Click on a player to view their aspects", centerX, logicalH - 165);
//    }
//
//    private void renderFilterButtons(DrawContext context, int logicalW, int logicalMouseX, int logicalMouseY) {
//        int totalFilterWidth = (FILTER_BUTTON_WIDTH * 2) + FILTER_SPACING;
//        int filterStartX = (logicalW - totalFilterWidth) / 2;
//
//        String[] filterNames = {"Most Aspects", "Username (A-Z)"};
//        for (int i = 0; i < 2; i++) {
//            int fx = filterStartX + (i * (FILTER_BUTTON_WIDTH + FILTER_SPACING));
//            boolean active = sortMode == i;
//            boolean hovering = logicalMouseX >= fx && logicalMouseX <= fx + FILTER_BUTTON_WIDTH &&
//                    logicalMouseY >= FILTER_Y && logicalMouseY <= FILTER_Y + FILTER_BUTTON_HEIGHT;
//
//            // Draw textured button
//            if (ui != null) {
//                ui.drawButton(fx, FILTER_Y, FILTER_BUTTON_WIDTH, FILTER_BUTTON_HEIGHT, 12, hovering || active);
//            }
//
//            String text = active ? "§6§l" + filterNames[i] : "§7" + filterNames[i];
//            ui.drawCenteredText(text, fx + FILTER_BUTTON_WIDTH / 2, FILTER_Y + FILTER_BUTTON_HEIGHT / 2);
//        }
//    }
//
//    private void renderPlayerEntry(DrawContext context, PlayerListEntry player, int index, int startX, int logicalMouseX, int logicalMouseY) {
//        int row = index / COLUMN_COUNT;
//        int col = index % COLUMN_COUNT;
//        int x = startX + (col * (ENTRY_WIDTH + ENTRY_SPACING));
//        int y = START_Y + (row * (ENTRY_HEIGHT + ENTRY_SPACING));
//
//        // Check if hovering
//        boolean hovering = logicalMouseX >= x && logicalMouseX <= x + ENTRY_WIDTH &&
//                logicalMouseY >= y && logicalMouseY <= y + ENTRY_HEIGHT;
//
//        // Background box (darker when hovering)
//        int bgColor = hovering ? 0xCC1a1a1a : 0xAA000000;
//        ui.drawRect(x, y, ENTRY_WIDTH, ENTRY_HEIGHT, CustomColor.fromInt(bgColor));
//
//        // Border (golden when hovering)
//        int borderColor = hovering ? 0xFFFFAA00 : 0xFF4e392d;
//        ui.drawRect(x, y, ENTRY_WIDTH, 3, CustomColor.fromInt(borderColor)); // top
//        ui.drawRect(x, y + ENTRY_HEIGHT - 3, ENTRY_WIDTH, 3, CustomColor.fromInt(borderColor)); // bottom
//        ui.drawRect(x, y, 3, ENTRY_HEIGHT, CustomColor.fromInt(borderColor)); // left
//        ui.drawRect(x + ENTRY_WIDTH - 3, y, 3, ENTRY_HEIGHT, CustomColor.fromInt(borderColor)); // right
//
//        // Player name (centered, larger)
//        ui.drawCenteredText("§6§l" + player.getPlayerName(), x + ENTRY_WIDTH / 2, y + 25);
//
//        // Aspect count - show "Total | Max" format if max count is available
//        int maxCount = player.getMaxAspectCount();
//        if (maxCount > 0) {
//            ui.drawCenteredText("§e" + player.getAspectCount() + " §7Total §8| §a" + maxCount + " §7Max", x + ENTRY_WIDTH / 2, y + 60);
//        } else {
//            ui.drawCenteredText("§e" + player.getAspectCount() + " §7aspects", x + ENTRY_WIDTH / 2, y + 60);
//        }
//    }
//
//    private List<PlayerListEntry> getSortedList() {
//        List<PlayerListEntry> sortedList = new ArrayList<>(playerList);
//        if (sortMode == 0) {
//            // Sort by most aspects (descending)
//            sortedList.sort((a, b) -> Integer.compare(b.getAspectCount(), a.getAspectCount()));
//        } else {
//            // Sort by username (A-Z)
//            sortedList.sort((a, b) -> a.getPlayerName().compareToIgnoreCase(b.getPlayerName()));
//        }
//        return sortedList;
//    }
//
//    @Override
//    public boolean mouseClicked(double mouseX, double mouseY, int button) {
////        if (button != 0) return false;
////
////        int logicalW = getLogicalWidth();
////        int logicalMouseX = toLogicalX(mouseX);
////        int logicalMouseY = toLogicalY(mouseY);
////
////        // Check filter button clicks
////        int totalFilterWidth = (FILTER_BUTTON_WIDTH * 2) + FILTER_SPACING;
////        int filterStartX = (logicalW - totalFilterWidth) / 2;
////
////        for (int i = 0; i < 2; i++) {
////            int fx = filterStartX + (i * (FILTER_BUTTON_WIDTH + FILTER_SPACING));
////            if (logicalMouseX >= fx && logicalMouseX <= fx + FILTER_BUTTON_WIDTH &&
////                    logicalMouseY >= FILTER_Y && logicalMouseY <= FILTER_Y + FILTER_BUTTON_HEIGHT) {
////                if (sortMode != i) {
////                    sortMode = i;
////                    McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
////                }
////                return true;
////            }
////        }
////
////        // Check player entry clicks
////        if (playerList == null) return false;
////
////        List<PlayerListEntry> sortedList = getSortedList();
////        int totalWidth = (ENTRY_WIDTH * COLUMN_COUNT) + (ENTRY_SPACING * (COLUMN_COUNT - 1));
////        int startX = (logicalW - totalWidth) / 2;
////        int maxEntries = Math.min(sortedList.size(), PER_PAGE);
////
////        for (int i = 0; i < maxEntries; i++) {
////            PlayerListEntry player = sortedList.get(i);
////            int row = i / COLUMN_COUNT;
////            int col = i % COLUMN_COUNT;
////            int x = startX + (col * (ENTRY_WIDTH + ENTRY_SPACING));
////            int y = START_Y + (row * (ENTRY_HEIGHT + ENTRY_SPACING));
////
////            if (logicalMouseX >= x && logicalMouseX <= x + ENTRY_WIDTH &&
////                    logicalMouseY >= y && logicalMouseY <= y + ENTRY_HEIGHT) {
////                // Click on player - search for their aspects
////                McUtils.playSoundUI(SoundEvents.UI_BUTTON_CLICK.value());
////                host.searchPlayer(player.getPlayerName());
////                return true;
////            }
////        }
//
//        return false;
//    }
//
//    private static class MostAspectsFil
//} removed for now, might be added in a later update
