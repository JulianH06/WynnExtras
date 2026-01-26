package julianh06.wynnextras.features.aspects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynntils.utils.mc.McUtils;
import julianh06.wynnextras.core.WynnExtras;
import julianh06.wynnextras.features.abilitytree.TreeLoader;
import julianh06.wynnextras.features.profileviewer.WynncraftApiHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;

import java.util.*;
import java.util.HashMap;
import java.util.List;


public class aspect {
    //TODO: aus tree stuff zeug klauen, auf aspects page gehen, dann ausgeben aspect x:100
    static int SearchedPages = 0;
    public static final Map<String, Pair<String, String>> allAspects = new HashMap<>();

    public static void openMenu(MinecraftClient client, PlayerEntity player){
        int currentSlot = player.getInventory().getSelectedSlot();
        player.getInventory().setSelectedSlot(7);

        // Just click normally - this opens the character menu
        client.interactionManager.interactItem(player, Hand.MAIN_HAND);

        player.getInventory().setSelectedSlot(currentSlot);

        // Set flag to click on "Ability Tree" when menu opens
        maintracking.setNeedToClickAbilityTree(true);
        maintracking.setAspectScanreq(true);
    }
    public static Map<String, Pair<String, String>> AspectsInMenu() {
        Screen currScreen = MinecraftClient.getInstance().currentScreen;
        HandledScreen<?> screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
        Map<String, Pair<String, String>> result = new HashMap<>();
        if (screen == null) {
            return result;
        } else {
            int[] slotsToRead = {36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53}; // fill slot indices

            int readCount = 0;
            for (int idx = 0; idx < slotsToRead.length; idx++) {
                int i = slotsToRead[idx];
                Slot slot = screen.getScreenHandler().slots.get(i);

                // Stop early if we encounter empty slots before batch is full
                if (!slot.hasStack()) {
                    // If this happens before readCount == 18, stop the batch immediately
                    dummyfunction(screen);
                    break; // Exit loop early, immediately go to next page
                }

                List<Text> tooltips = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
                String name = null;
                String tierLine = null;
                String rarity = "";

                for (Text tooltip : tooltips) {
                    String s = tooltip.getString().replaceAll("§.", "").trim();
                    if (name == null && (s.contains("Aspect") || s.contains("Embodiment"))) {
                        name = s;

                        // Safe null checking for rarity detection
                        if (slot.getStack().getCustomName() != null &&
                            slot.getStack().getCustomName().getStyle() != null &&
                            slot.getStack().getCustomName().getStyle().getColor() != null) {
                            String hexCode = slot.getStack().getCustomName().getStyle().getColor().getHexCode();
                            if(hexCode.equals("#AA00AA")) {
                                rarity = "Mythic";
                            } else if(hexCode.equals("#FF5555")) {
                                rarity = "Fabled";
                            }else if(hexCode.equals("#55FFFF")) {
                                rarity = "Legendary";
                            }
                        }
                    }
                }

                String bestTierLine = null;
                for (Text tooltip : tooltips) {
                    String candidate = extractBestTierLine(tooltip).trim();
                    if (candidate.contains("Tier") &&
                            (candidate.contains(">>>") || candidate.matches(".*\\[\\d+/\\d+\\].*") || candidate.contains("[MAX]"))) {
                        if (bestTierLine == null || candidate.length() > bestTierLine.length()) {
                            bestTierLine = candidate;
                        }
                    }
                }

                if (name != null && bestTierLine != null && !bestTierLine.isEmpty()) {
                    bestTierLine = bestTierLine.replaceAll("^[^A-Za-z0-9]*", "");
                    bestTierLine = bestTierLine.replaceAll("(\\[MAX])\\1+", "$1");
                    result.put(name, new Pair<>(bestTierLine, rarity));
                }

                readCount++;
                if (readCount == 18) {
                    dummyfunction(screen);
                    break; // Done with this page, no need to check further
                }

            }
            if (readCount < 18) {
                int[] extraSlots = {4, 11, 15, 18, 26};
                for (int extra : extraSlots) {
                    // Make sure you don't scan an out-of-bounds slot!
                    if (extra < screen.getScreenHandler().slots.size()) {
                        Slot slot = screen.getScreenHandler().slots.get(extra);
                        if (!slot.hasStack()) continue;
                        List<Text> tooltips = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
                        String name = null, tierLine = null;
                        String rarity = "";
                        for (Text tooltip : tooltips) {
                            String s = tooltip.getString().replaceAll("§.", "").trim();
                            if (name == null && (s.contains("Aspect") || s.contains("Embodiment"))) name = s;

                            // Safe null checking for rarity detection
                            if (slot.getStack().getCustomName() != null &&
                                slot.getStack().getCustomName().getStyle() != null &&
                                slot.getStack().getCustomName().getStyle().getColor() != null) {
                                String hexCode = slot.getStack().getCustomName().getStyle().getColor().getHexCode();
                                if(hexCode.equals("#AA00AA")) {
                                    rarity = "Mythic";
                                } else if(hexCode.equals("#FF5555")) {
                                    rarity = "Fabled";
                                }else if(hexCode.equals("#55FFFF")) {
                                    rarity = "Legendary";
                                }
                            }
                        }
                        String bestTierLine = null;
                        for (Text tooltip : tooltips) {
                            String candidate = extractBestTierLine(tooltip).trim();
                            if (candidate.contains("Tier") &&
                                    (candidate.contains(">>>") || candidate.matches(".*\\[\\d+/\\d+\\].*") || candidate.contains("[MAX]"))) {
                                if (bestTierLine == null || candidate.length() > bestTierLine.length())
                                    bestTierLine = candidate;
                            }
                        }
                        if (name != null && bestTierLine != null && !bestTierLine.isEmpty()) {
                            bestTierLine = bestTierLine.replaceAll("^[^A-Za-z0-9]*", "");
                            bestTierLine = bestTierLine.replaceAll("(\\[MAX])\\1+", "$1");
                            result.put(name, new Pair<>(bestTierLine, rarity));
                        }
                    }
                }
            }
            for (Map.Entry<String, Pair<String, String>> entry : result.entrySet()) {
                allAspects.put(entry.getKey(), entry.getValue());
            }

            maintracking.setAspectScanreq(false);
            if (readCount < 18 && McUtils.mc().currentScreen != null) {
                McUtils.mc().currentScreen.close();
            }
            return result;
        }
    }

    public static void AspectsInRaidChest() {
        Screen currScreen = MinecraftClient.getInstance().currentScreen;
        HandledScreen<?> screen = (currScreen instanceof HandledScreen) ? (HandledScreen<?>) currScreen : null;
        if (screen == null) return;

        if(maintracking.goingBack) {
            if(!screen.getScreenHandler().slots.get(16).getStack().isEmpty()) {
                PrevPageRaid(screen);
            }
        }

        int[] slotsToRead = {11,12,13,14,15};
        List<String> foundNames = new ArrayList<>();

        boolean samePage = true;
        for(int i = 11; i < 16; i++) {
            int arrayIndex = i - 11; // Map slot 11-15 to array index 0-4
            Slot slot = screen.getScreenHandler().slots.get(i);
            ItemStack cachedStack = maintracking.aspectsInChest[arrayIndex];

            // If cached is null, definitely not same page
            if (cachedStack == null) {
                samePage = false;
                break;
            }

            if (slot.getStack().isEmpty()) {
                if (!cachedStack.isEmpty()) {
                    samePage = false;
                    break;
                }
            } else {
                // Safe null check for customName comparison
                Text currentName = slot.getStack().getCustomName();
                Text cachedName = cachedStack.getCustomName();
                if (currentName == null || cachedName == null || !currentName.equals(cachedName)) {
                    samePage = false;
                    break;
                }
            }
        }

        if(samePage) {
            return;
        }

        maintracking.aspectsInChest = new ItemStack[5];

        if(maintracking.goingBack) {
            if(screen.getScreenHandler().slots.get(10).getStack().isEmpty()) {
                maintracking.scanDone = true;
                maintracking.goingBack = false;
            }
        }

        for(int i = 11; i < 16; i++) {
            int arrayIndex = i - 11; // Map slot 11-15 to array index 0-4
            Slot slot = screen.getScreenHandler().slots.get(i);

            maintracking.aspectsInChest[arrayIndex] = slot.getStack().copy();

            // Stop if empty
            if (!slot.hasStack()) break;

            List<Text> tooltips = slot.getStack().getTooltip(Item.TooltipContext.DEFAULT, MinecraftClient.getInstance().player, TooltipType.BASIC);
            String name = null;
            for (Text tooltip : tooltips) {
                String s = tooltip.getString().replaceAll("§.", "").trim();
                if (s.contains("Aspect") || s.contains("Embodiment")) {
                    name = s;
                    break;
                }
            }
            // Stop at the first non-aspect, but keep any already found
            if (name == null) break;
            foundNames.add(name);
        }

        // Print all aspects found this page, in order, with duplicates if there are any
        if (MinecraftClient.getInstance().player != null) {
            for (String aspectName : foundNames) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("Found aspect: " + aspectName));
            }
        }

        // If all 5 slots contained aspects, go to next page
        if (!screen.getScreenHandler().slots.get(16).getStack().isEmpty()) {
            NextPageRaid(screen);
        } else {
            maintracking.goingBack = true;
        }
    }





    private static String extractBestTierLine(Text t) {
        String self = t.getString().replaceAll("§.", "").trim();
        boolean relevant = !self.isEmpty() && (self.contains("Tier") || self.contains(">>>") ||
                self.matches(".*\\[\\d+/\\d+\\].*") || self.contains("[MAX]"));
        StringBuilder out = new StringBuilder();
        if (relevant) out.append(self);

        StringBuilder deepest = new StringBuilder();
        if (t.getSiblings() != null) {
            for (Text sib : t.getSiblings()) {
                String sub = extractBestTierLine(sib).trim();
                if (sub.length() > deepest.length()) {
                    deepest.setLength(0); deepest.append(sub);
                }
            }
        }
        return deepest.length() > out.length() ? deepest.toString() : out.toString();
    }

    private static void dummyfunction(HandledScreen<?> screen) {
        //System.out.println("dummyfunction called after 18 slots read");
        SearchedPages++;
        TreeLoader.clickOnNameInInventory("Next Page",screen,MinecraftClient.getInstance());
        if(SearchedPages<6){
            maintracking.setNextPage(true);
        }
        else{
            System.out.println(allAspects);
            WynncraftApiHandler.processAspects(allAspects);
            resetAllAspects();
            SearchedPages = 0; //TODO
        }
    }
    private static void NextPageRaid(HandledScreen<?> screen) {
        System.out.println("all slots read");
        TreeLoader.clickOnNameInInventory("Next Page",screen,MinecraftClient.getInstance());
        //das hat nichts mit dem treeloader direkt zu tun, ist nur ne util funktion
    }
    public static void PrevPageRaid(HandledScreen<?> screen) {
        System.out.println("go back");
        TreeLoader.clickOnNameInInventory("Previous Page",screen,MinecraftClient.getInstance());
    }
    public static void setSearchedPages(int searchedPages) {
        SearchedPages = searchedPages;
    }
    public static void resetAllAspects() {
        allAspects.clear();
    }

    /**
     * Detects and displays all 4 daily gambits from the Party Finder menu
     * Gambit format: "Name Gambit" or "Name Name's Gambit"
     * Example: "Glutton's Gambit", "Dull Blade's Gambit"
     * Description is between "Refreshes in X hours" and "Rewards for enabling"
     * Also saves to GambitData for the GUI
     */
    public static void detectGambit(HandledScreen<?> screen) {
        if (screen == null) return;

        try {
            List<String> gambitsForChat = new ArrayList<>();
            List<GambitData.GambitEntry> gambitsForSave = new ArrayList<>();

            // Search through inventory slots for all gambit items
            for (Slot slot : screen.getScreenHandler().slots) {
                if (!slot.hasStack()) continue;

                ItemStack stack = slot.getStack();
                if (stack.getCustomName() == null) continue;

                String itemName = stack.getCustomName().getString().replaceAll("§.", "").trim();

                // Check if item name contains "Gambit"
                if (itemName.contains("Gambit")) {
                    // Get the tooltip for description
                    List<Text> tooltips = stack.getTooltip(Item.TooltipContext.DEFAULT,
                        MinecraftClient.getInstance().player, TooltipType.BASIC);

                    StringBuilder description = new StringBuilder();
                    boolean inDescriptionSection = false;

                    for (Text tooltip : tooltips) {
                        String line = tooltip.getString().replaceAll("§.", "").trim();

                        // Start collecting after "Refreshes in" line
                        if (line.contains("Refreshes in")) {
                            inDescriptionSection = true;
                            continue;
                        }

                        // Stop collecting at "Rewards for enabling" or "Rewards for Enabling"
                        if (line.contains("Rewards for enabling") || line.contains("Rewards for Enabling")) {
                            break;
                        }

                        // Collect description lines
                        if (inDescriptionSection && !line.isEmpty()) {
                            if (description.length() > 0) description.append(" ");
                            description.append(line);
                        }
                    }

                    // Only add if we have both name and description
                    if (description.length() > 0) {
                        gambitsForChat.add("§e" + itemName + " §7- " + description.toString());
                        gambitsForSave.add(new GambitData.GambitEntry(itemName, description.toString()));
                    }
                }
            }

            // Save gambits to data storage
            if (!gambitsForSave.isEmpty()) {
                GambitData.INSTANCE.saveGambits(gambitsForSave);
            }

            // Send all gambits to chat
            if (!gambitsForChat.isEmpty()) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§6Today's Gambits:"));
                for (String gambit : gambitsForChat) {
                    McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("  " + gambit));
                }
            } else {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cNo gambits detected"));
            }

        } catch (Exception e) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError detecting gambit: " + e.getMessage()));
        }
    }

    /**
     * Scans the raid preview chest for aspects and detects which raid is selected
     * Raid is detected from the screen title itself (last character changes per raid)
     * Also extracts user's aspect tier and progress for each aspect
     * Saves loot pool data locally for the GUI
     */
    public static void scanPreviewChest(HandledScreen<?> screen, String screenTitle) {
        if (screen == null) return;

        System.out.println("[WynnExtras] scanPreviewChest called with title: " + screenTitle);

        try {
            // Detect raid from the last character of the title
            String selectedRaid = "Unknown";
            if (screenTitle.endsWith("\uF00B")) {
                selectedRaid = "NOTG";
            } else if (screenTitle.endsWith("\uF00C")) {
                selectedRaid = "NOL";
            } else if (screenTitle.endsWith("\uF00D")) {
                selectedRaid = "TCC";
            } else if (screenTitle.endsWith("\uF00E")) {
                selectedRaid = "TNA";
            }

            Map<String, Pair<String, String>> foundAspects = new HashMap<>();
            List<LootPoolData.AspectEntry> lootPoolDataFull = new ArrayList<>(); // For saving with full data

            // Collect all aspects with their progress
            for (Slot slot : screen.getScreenHandler().slots) {
                if (!slot.hasStack()) continue;

                ItemStack stack = slot.getStack();
                List<Text> tooltips = stack.getTooltip(Item.TooltipContext.DEFAULT,
                    MinecraftClient.getInstance().player, TooltipType.BASIC);

                String aspectName = null;
                String tierLine = null;
                String rarity = "";
                StringBuilder description = new StringBuilder();
                boolean foundName = false;
                boolean foundTier = false;

                for (Text tooltip : tooltips) {
                    String line = tooltip.getString().replaceAll("§.", "").trim();

                    // Find aspect name
                    if (aspectName == null && (line.contains("Aspect") || line.contains("Embodiment"))) {
                        aspectName = line;
                        foundName = true;

                        // Detect rarity from color with safe null checking
                        if (stack.getCustomName() != null &&
                            stack.getCustomName().getStyle() != null &&
                            stack.getCustomName().getStyle().getColor() != null) {
                            String hexCode = stack.getCustomName().getStyle().getColor().getHexCode();
                            if (hexCode.equals("#AA00AA")) {
                                rarity = "Mythic";
                            } else if (hexCode.equals("#FF5555")) {
                                rarity = "Fabled";
                            } else if (hexCode.equals("#55FFFF")) {
                                rarity = "Legendary";
                            }
                        }
                        continue;
                    }

                    // Check if we've reached tier info
                    if (foundName && (line.contains("Tier") || line.contains("[") || line.contains(">") || line.contains("Class Req:"))) {
                        foundTier = true;
                    }

                    // Collect all description lines (between name and tier info)
                    if (foundName && !foundTier && !line.isEmpty() &&
                        !line.contains("Aspect") && !line.contains("Embodiment")) {
                        if (description.length() > 0) {
                            description.append("\n");
                        }
                        description.append(line);
                    }
                }

                // Find best tier line
                String bestTierLine = null;
                for (Text tooltip : tooltips) {
                    String candidate = extractBestTierLine(tooltip).trim();
                    if (candidate.contains("Tier") &&
                            (candidate.contains(">>>") || candidate.matches(".*\\[\\d+/\\d+\\].*") || candidate.contains("[MAX]"))) {
                        if (bestTierLine == null || candidate.length() > bestTierLine.length()) {
                            bestTierLine = candidate;
                        }
                    }
                }

                if (aspectName != null && bestTierLine != null && !bestTierLine.isEmpty()) {
                    bestTierLine = bestTierLine.replaceAll("^[^A-Za-z0-9]*", "");
                    bestTierLine = bestTierLine.replaceAll("(\\[MAX])\\1+", "$1");
                    foundAspects.put(aspectName, new Pair<>(bestTierLine, rarity));

                    // Save for loot pool data with full info
                    if (!rarity.isEmpty()) {
                        lootPoolDataFull.add(new LootPoolData.AspectEntry(aspectName, rarity, bestTierLine, description.toString()));
                    }
                }
            }

            // Save loot pool data to local storage with full info
            if (!selectedRaid.equals("Unknown") && !lootPoolDataFull.isEmpty()) {
                LootPoolData.INSTANCE.saveLootPoolFull(selectedRaid, lootPoolDataFull);
            }

            // Display results in chat
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§6Preview Chest - §e" + selectedRaid));
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§7Aspects in pool (with your progress):"));

            for (Map.Entry<String, Pair<String, String>> entry : foundAspects.entrySet()) {
                String aspectName = entry.getKey();
                String tierInfo = entry.getValue().getLeft();
                String rarityStr = entry.getValue().getRight();

                // Color codes: Mythic = dark purple (§5), Fabled = red (§c), Legendary = light blue (§b)
                String raritySymbol = "◆";
                String rarityColor = "§b"; // Default: light blue for legendary

                if (rarityStr.equals("Mythic")) {
                    raritySymbol = "★";
                    rarityColor = "§5"; // dark purple
                } else if (rarityStr.equals("Fabled")) {
                    raritySymbol = "◇";
                    rarityColor = "§c"; // red
                } else if (rarityStr.equals("Legendary")) {
                    raritySymbol = "◆";
                    rarityColor = "§b"; // light blue
                }

                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix(
                    "  " + rarityColor + raritySymbol + " " + aspectName + " §7[" + tierInfo + "]"));
            }

            if (foundAspects.isEmpty()) {
                McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§c  No aspects detected"));
            } else {
                System.out.println("[WynnExtras] Found " + foundAspects.size() + " aspects, uploading to API...");
                // Send aspects to API (same as /we scanaspects)
                WynncraftApiHandler.processAspects(foundAspects);
            }

        } catch (Exception e) {
            McUtils.sendMessageToClient(WynnExtras.addWynnExtrasPrefix("§cError scanning preview chest: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}

